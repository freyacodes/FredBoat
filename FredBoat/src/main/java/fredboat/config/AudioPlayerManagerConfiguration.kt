/*
 * MIT License
 *
 * Copyright (c) 2017-2018 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package fredboat.config

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.lava.extensions.youtuberotator.YoutubeIpRotator
import com.sedmelluq.lava.extensions.youtuberotator.planner.AbstractRoutePlanner
import com.sedmelluq.lava.extensions.youtuberotator.planner.BalancingIpRoutePlanner
import com.sedmelluq.lava.extensions.youtuberotator.planner.NanoIpRoutePlanner
import com.sedmelluq.lava.extensions.youtuberotator.planner.RotatingIpRoutePlanner
import com.sedmelluq.lava.extensions.youtuberotator.planner.RotatingNanoIpRoutePlanner
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv4Block
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv6Block
import fredboat.audio.source.PlaylistImportSourceManager
import fredboat.audio.source.SpotifyPlaylistSourceManager
import fredboat.config.property.AppConfig
import fredboat.config.property.AudioSourcesConfig
import fredboat.util.rest.SpotifyAPIWrapper
import fredboat.util.rest.TrackSearcher
import org.apache.http.client.config.CookieSpecs
import org.apache.http.client.config.RequestConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import java.net.InetAddress
import java.util.*
import java.util.function.Predicate

/**
 * Created by napster on 25.02.18.
 *
 *
 * Defines all the AudioPlayerManagers used throughout the application.
 *
 *
 * Two special quirks to keep in mind:
 * - if an Http(Audio)SourceManager is used, it needs to be added as the last source manager, because it eats each
 * request (either returns with success or throws a failure)
 * - the paste service AudioPlayerManager should not contain the paste playlist importer to avoid recursion / users
 * abusing fredboat into paste file chains
 *
 *
 * We manage the lifecycles of these Beans ourselves. See [fredboat.event.MusicPersistenceHandler]
 */
@Configuration
class AudioPlayerManagerConfiguration {

    private val log: Logger = LoggerFactory.getLogger(AudioPlayerManagerConfiguration::class.java)

    /**
     * @return all AudioPlayerManagers
     */
    @Bean
    fun allPlayerManagers(@Qualifier("loadAudioPlayerManager") load: AudioPlayerManager,
                          @Qualifier("searchAudioPlayerManager") search: AudioPlayerManager,
                          @Qualifier("pasteAudioPlayerManager") paste: AudioPlayerManager): Set<AudioPlayerManager> {
        return setOf(load, search, paste)
    }

    /**
     * @return the AudioPlayerManager to be used for loading the tracks
     */
    @Bean(destroyMethod = "")
    fun loadAudioPlayerManager(@Qualifier("preconfiguredAudioPlayerManager") playerManager: AudioPlayerManager,
                               audioSourceManagers: ArrayList<AudioSourceManager>,
                               playlistImportSourceManager: PlaylistImportSourceManager): AudioPlayerManager {
        playerManager.registerSourceManager(playlistImportSourceManager)
        for (audioSourceManager in audioSourceManagers) {
            playerManager.registerSourceManager(audioSourceManager)
        }
        return playerManager
    }

    /**
     * @return the AudioPlayerManager to be used for searching
     */
    @Bean(destroyMethod = "")
    fun searchAudioPlayerManager(@Qualifier("preconfiguredAudioPlayerManager") playerManager: AudioPlayerManager,
                                 youtubeAudioSourceManager: YoutubeAudioSourceManager,
                                 soundCloudAudioSourceManager: SoundCloudAudioSourceManager): AudioPlayerManager {
        playerManager.registerSourceManager(youtubeAudioSourceManager)
        playerManager.registerSourceManager(soundCloudAudioSourceManager)
        return playerManager
    }

    /**
     * @return audioPlayerManager to load from paste lists
     */
    @Bean(destroyMethod = "")
    fun pasteAudioPlayerManager(@Qualifier("preconfiguredAudioPlayerManager") playerManager: AudioPlayerManager,
                                audioSourceManagers: ArrayList<AudioSourceManager>): AudioPlayerManager {
        for (audioSourceManager in audioSourceManagers) {
            playerManager.registerSourceManager(audioSourceManager)
        }
        return playerManager
    }


    // it is important that the list is ordered with the httpSourceManager being last
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @Suppress("DuplicatedCode")
    fun getConfiguredAudioSourceManagers(audioSourcesConfig: AudioSourcesConfig,
                                         youtubeAudioSourceManager: YoutubeAudioSourceManager,
                                         soundCloudAudioSourceManager: SoundCloudAudioSourceManager,
                                         bandcampAudioSourceManager: BandcampAudioSourceManager,
                                         twitchStreamAudioSourceManager: TwitchStreamAudioSourceManager,
                                         vimeoAudioSourceManager: VimeoAudioSourceManager,
                                         beamAudioSourceManager: BeamAudioSourceManager,
                                         spotifyPlaylistSourceManager: SpotifyPlaylistSourceManager,
                                         localAudioSourceManager: LocalAudioSourceManager,
                                         httpAudioSourceManager: HttpAudioSourceManager): ArrayList<AudioSourceManager> {
        val audioSourceManagers = ArrayList<AudioSourceManager>()

        if (audioSourcesConfig.isYouTubeEnabled) {
            audioSourceManagers.add(youtubeAudioSourceManager)
        }
        if (audioSourcesConfig.isSoundCloudEnabled) {
            audioSourceManagers.add(soundCloudAudioSourceManager)
        }
        if (audioSourcesConfig.isBandCampEnabled) {
            audioSourceManagers.add(bandcampAudioSourceManager)
        }
        if (audioSourcesConfig.isTwitchEnabled) {
            audioSourceManagers.add(twitchStreamAudioSourceManager)
        }
        if (audioSourcesConfig.isVimeoEnabled) {
            audioSourceManagers.add(vimeoAudioSourceManager)
        }
        if (audioSourcesConfig.isMixerEnabled) {
            audioSourceManagers.add(beamAudioSourceManager)
        }
        if (audioSourcesConfig.isSpotifyEnabled) {
            audioSourceManagers.add(spotifyPlaylistSourceManager)
        }
        if (audioSourcesConfig.isLocalEnabled) {
            audioSourceManagers.add(localAudioSourceManager)
        }
        audioSourceManagers.add(httpAudioSourceManager)
        if (audioSourcesConfig.isHttpEnabled) {
            //add new source managers above the HttpAudio one, because it will either eat your request or throw an exception
            //so you will never reach a source manager below it
            audioSourceManagers.add(httpAudioSourceManager)
        }
        return audioSourceManagers
    }


    /**
     * @return a preconfigured AudioPlayerManager, no AudioSourceManagers set
     */
    @Bean(destroyMethod = "")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    fun preconfiguredAudioPlayerManager(appConfig: AppConfig): AudioPlayerManager {
        val playerManager = DefaultAudioPlayerManager()

        //Patrons and development get higher quality
        var quality: AudioConfiguration.ResamplingQuality = AudioConfiguration.ResamplingQuality.LOW
        if (appConfig.isPatronDistribution || appConfig.isDevDistribution) {
            quality = AudioConfiguration.ResamplingQuality.MEDIUM
        }

        playerManager.configuration.resamplingQuality = quality

        playerManager.frameBufferDuration = 1000
        playerManager.setItemLoaderThreadPoolSize(500)

        return playerManager
    }


    @Bean(destroyMethod = "")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    fun playlistImportSourceManager(@Qualifier("pasteAudioPlayerManager") audioPlayerManager: AudioPlayerManager): PlaylistImportSourceManager {
        return PlaylistImportSourceManager(audioPlayerManager)
    }

    @Bean(destroyMethod = "")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    fun youtubeAudioSourceManager(routePlanner: AbstractRoutePlanner?): YoutubeAudioSourceManager {
        val youtubeAudioSourceManager = YoutubeAudioSourceManager()

        val youtube = YoutubeAudioSourceManager()
        if (routePlanner != null) {
            YoutubeIpRotator.setup(youtube, routePlanner)
        }

        youtubeAudioSourceManager.configureRequests { config ->
            RequestConfig.copy(config)
                    .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                    .build()
        }
        youtubeAudioSourceManager.setMixLoaderMaximumPoolSize(50)
        youtubeAudioSourceManager.setPlaylistPageCount(3)
        return youtubeAudioSourceManager
    }

    @Bean(destroyMethod = "")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    fun soundCloudAudioSourceManager() = SoundCloudAudioSourceManager()

    @Bean(destroyMethod = "")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    fun bandcampAudioSourceManager() = BandcampAudioSourceManager()

    @Bean(destroyMethod = "")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    fun twitchStreamAudioSourceManager() = TwitchStreamAudioSourceManager()

    @Bean(destroyMethod = "")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    fun vimeoAudioSourceManager() = VimeoAudioSourceManager()

    @Bean(destroyMethod = "")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    fun beamAudioSourceManager() = BeamAudioSourceManager()

    @Bean(destroyMethod = "")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    fun spotifyPlaylistSourceManager(trackSearcher: TrackSearcher, spotifyAPIWrapper: SpotifyAPIWrapper) =
            SpotifyPlaylistSourceManager(trackSearcher, spotifyAPIWrapper)

    @Bean(destroyMethod = "")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    fun localAudioSourceManager() = LocalAudioSourceManager()

    @Bean(destroyMethod = "")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    fun httpSourceManager() = HttpAudioSourceManager()

    @Bean
    fun routePlanner(appConfig: AppConfig): AbstractRoutePlanner? {
        val rateLimitConfig = appConfig.ratelimit
        if(rateLimitConfig == null) {
            log.debug("No rate limit config block found, skipping setup of route planner")
            return null
        }
        val ipBlockList = if (rateLimitConfig.balancingBlock.isNotBlank()) {
            log.warn("Using deprecated balancingBlock config property")
            listOf(rateLimitConfig.balancingBlock)
        } else {
            rateLimitConfig.ipBlocks
        }
        if (ipBlockList.isEmpty()) {
            log.info("List of ip blocks is empty, skipping setup of route planner")
            return null
        }

        val blacklisted = rateLimitConfig.excludedIps.map { InetAddress.getByName(it) }
        val filter = Predicate<InetAddress> {
            !blacklisted.contains(it)
        }
        val ipBlocks = ipBlockList.map {
            when {
                Ipv4Block.isIpv4CidrBlock(it) -> Ipv4Block(it)
                Ipv6Block.isIpv6CidrBlock(it) -> Ipv6Block(it)
                else -> throw RuntimeException("Invalid IP Block '$it', make sure to provide a valid CIDR notation")
            }
        }

        return when (rateLimitConfig.strategy.toLowerCase().trim()) {
            "rotateonban" -> RotatingIpRoutePlanner(ipBlocks, filter, rateLimitConfig.searchTriggersFail)
            "loadbalance" -> BalancingIpRoutePlanner(ipBlocks, filter, rateLimitConfig.searchTriggersFail)
            "nanoswitch" -> NanoIpRoutePlanner(ipBlocks, rateLimitConfig.searchTriggersFail)
            "rotatingnanoswitch" -> RotatingNanoIpRoutePlanner(ipBlocks, filter, rateLimitConfig.searchTriggersFail)
            else -> throw RuntimeException("Unknown strategy!")
        }
    }
}
