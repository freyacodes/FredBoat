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

package fredboat.config;

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import fredboat.audio.source.HttpSourceManager;
import fredboat.audio.source.PlaylistImportSourceManager;
import fredboat.audio.source.SpotifyPlaylistSourceManager;
import fredboat.config.property.AppConfig;
import fredboat.config.property.AudioSourcesConfig;
import fredboat.util.rest.SpotifyAPIWrapper;
import fredboat.util.rest.TrackSearcher;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.ArrayList;

/**
 * Created by napster on 25.02.18.
 * <p>
 * Defines all the AudioPlayerManagers used throughout the application.
 * <p>
 * Two special quirks to keep in mind:
 * - if an Http(Audio)SourceManager is used, it needs to be added as the last source manager, because it eats each
 * request (either returns with success or throws a failure)
 * - the paste service AudioPlayerManager should not contain the paste playlist importer to avoid recursion / users
 * abusing fredboat into paste file chains
 */
@Configuration
public class AudioPlayerManagerConfiguration {


    /**
     * @return the AudioPlayerManager to be used for loading the tracks
     */
    @Bean
    public AudioPlayerManager loadAudioPlayerManager(@Qualifier("preconfiguredAudioPlayerManager") AudioPlayerManager playerManager,
                                                     ArrayList<AudioSourceManager> audioSourceManagers,
                                                     PlaylistImportSourceManager playlistImportSourceManager) {
        playerManager.registerSourceManager(playlistImportSourceManager);
        for (AudioSourceManager audioSourceManager : audioSourceManagers) {
            playerManager.registerSourceManager(audioSourceManager);
        }
        return playerManager;
    }

    /**
     * @return the AudioPlayerManager to be used for searching
     */
    @Bean
    public AudioPlayerManager searchAudioPlayerManager(@Qualifier("preconfiguredAudioPlayerManager") AudioPlayerManager playerManager,
                                                       YoutubeAudioSourceManager youtubeAudioSourceManager,
                                                       SoundCloudAudioSourceManager soundCloudAudioSourceManager) {
        playerManager.registerSourceManager(youtubeAudioSourceManager);
        playerManager.registerSourceManager(soundCloudAudioSourceManager);
        return playerManager;
    }

    /**
     * @return audioPlayerManager to load from paste lists
     */
    @Bean
    public AudioPlayerManager pasteAudioPlayerManager(@Qualifier("preconfiguredAudioPlayerManager") AudioPlayerManager playerManager,
                                                      ArrayList<AudioSourceManager> audioSourceManagers) {
        for (AudioSourceManager audioSourceManager : audioSourceManagers) {
            playerManager.registerSourceManager(audioSourceManager);
        }
        return playerManager;
    }


    // it is important that the list is ordered with the httpSourceManager being last
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public ArrayList<AudioSourceManager> getConfiguredAudioSourceManagers(AudioSourcesConfig audioSourcesConfig,
                                                                          YoutubeAudioSourceManager youtubeAudioSourceManager,
                                                                          SoundCloudAudioSourceManager soundCloudAudioSourceManager,
                                                                          BandcampAudioSourceManager bandcampAudioSourceManager,
                                                                          TwitchStreamAudioSourceManager twitchStreamAudioSourceManager,
                                                                          VimeoAudioSourceManager vimeoAudioSourceManager,
                                                                          BeamAudioSourceManager beamAudioSourceManager,
                                                                          SpotifyPlaylistSourceManager spotifyPlaylistSourceManager,
                                                                          LocalAudioSourceManager localAudioSourceManager,
                                                                          HttpSourceManager httpSourceManager) {
        ArrayList<AudioSourceManager> audioSourceManagers = new ArrayList<>();

        if (audioSourcesConfig.isYouTubeEnabled()) {
            audioSourceManagers.add(youtubeAudioSourceManager);
        }
        if (audioSourcesConfig.isSoundCloudEnabled()) {
            audioSourceManagers.add(soundCloudAudioSourceManager);
        }
        if (audioSourcesConfig.isBandCampEnabled()) {
            audioSourceManagers.add(bandcampAudioSourceManager);
        }
        if (audioSourcesConfig.isTwitchEnabled()) {
            audioSourceManagers.add(twitchStreamAudioSourceManager);
        }
        if (audioSourcesConfig.isVimeoEnabled()) {
            audioSourceManagers.add(vimeoAudioSourceManager);
        }
        if (audioSourcesConfig.isMixerEnabled()) {
            audioSourceManagers.add(beamAudioSourceManager);
        }
        if (audioSourcesConfig.isSpotifyEnabled()) {
            audioSourceManagers.add(spotifyPlaylistSourceManager);
        }
        if (audioSourcesConfig.isLocalEnabled()) {
            audioSourceManagers.add(localAudioSourceManager);
        }
        if (audioSourcesConfig.isHttpEnabled()) {
            //add new source managers above the HttpAudio one, because it will either eat your request or throw an exception
            //so you will never reach a source manager below it
            audioSourceManagers.add(httpSourceManager);
        }
        return audioSourceManagers;
    }


    /**
     * @return a preconfigured AudioPlayerManager, no AudioSourceManagers set
     */
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public AudioPlayerManager preconfiguredAudioPlayerManager(AppConfig appConfig) {
        AudioPlayerManager playerManager = new DefaultAudioPlayerManager();

        //Patrons and development get higher quality
        AudioConfiguration.ResamplingQuality quality = AudioConfiguration.ResamplingQuality.LOW;
        if (appConfig.isPatronDistribution() || appConfig.isDevDistribution()) {
            quality = AudioConfiguration.ResamplingQuality.MEDIUM;
        }

        playerManager.getConfiguration().setResamplingQuality(quality);

        playerManager.setFrameBufferDuration(1000);
        playerManager.setItemLoaderThreadPoolSize(500);

        return playerManager;
    }


    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public PlaylistImportSourceManager playlistImportSourceManager(@Qualifier("pasteAudioPlayerManager") AudioPlayerManager audioPlayerManager) {
        return new PlaylistImportSourceManager(audioPlayerManager);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public YoutubeAudioSourceManager youtubeAudioSourceManager() {
        YoutubeAudioSourceManager youtubeAudioSourceManager = new YoutubeAudioSourceManager();
        youtubeAudioSourceManager.configureRequests(config -> RequestConfig.copy(config)
                .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                .build());
        youtubeAudioSourceManager.setMixLoaderMaximumPoolSize(50);
        return youtubeAudioSourceManager;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public SoundCloudAudioSourceManager soundCloudAudioSourceManager() {
        return new SoundCloudAudioSourceManager();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public BandcampAudioSourceManager bandcampAudioSourceManager() {
        return new BandcampAudioSourceManager();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public TwitchStreamAudioSourceManager twitchStreamAudioSourceManager() {
        return new TwitchStreamAudioSourceManager();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public VimeoAudioSourceManager vimeoAudioSourceManager() {
        return new VimeoAudioSourceManager();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public BeamAudioSourceManager beamAudioSourceManager() {
        return new BeamAudioSourceManager();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public SpotifyPlaylistSourceManager spotifyPlaylistSourceManager(TrackSearcher trackSearcher,
                                                                     SpotifyAPIWrapper spotifyAPIWrapper) {
        return new SpotifyPlaylistSourceManager(trackSearcher, spotifyAPIWrapper);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public LocalAudioSourceManager localAudioSourceManager() {
        return new LocalAudioSourceManager();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public HttpSourceManager httpSourceManager() {
        return new HttpSourceManager();
    }
}
