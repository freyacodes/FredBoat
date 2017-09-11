/*
 * MIT License
 *
 * Copyright (c) 2017 Frederik Ar. Mikkelsen
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
 *
 */

package fredboat.audio.player;

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.TrackMarker;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import fredboat.Config;
import fredboat.audio.queue.AudioTrackContext;
import fredboat.audio.queue.ITrackProvider;
import fredboat.audio.queue.SplitAudioTrackContext;
import fredboat.audio.queue.TrackEndMarkerHandler;
import fredboat.audio.source.PlaylistImportSourceManager;
import fredboat.audio.source.SpotifyPlaylistSourceManager;
import fredboat.commandmeta.MessagingException;
import fredboat.shared.constant.DistributionEnum;
import lavalink.client.player.IPlayer;
import lavalink.client.player.LavaplayerPlayerWrapper;
import lavalink.client.player.event.AudioEventAdapterWrapped;
import net.dv8tion.jda.core.audio.AudioSendHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractPlayer extends AudioEventAdapterWrapped implements AudioSendHandler {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(AbstractPlayer.class);

    private static AudioPlayerManager playerManager;
    protected final IPlayer player;
    ITrackProvider audioTrackProvider;
    private AudioFrame lastFrame = null;
    protected AudioTrackContext context;
    private final AudioLossCounter audioLossCounter = new AudioLossCounter();

    protected Consumer<AudioTrackContext> onPlayHook;
    protected Consumer<Throwable> onErrorHook;

    @SuppressWarnings("LeakingThisInConstructor")
    AbstractPlayer(String guildId) {
        initAudioPlayerManager();
        player = LavalinkManager.ins.createPlayer(guildId);

        player.addListener(this);
    }

    private static void initAudioPlayerManager() {
        if (playerManager == null) {
            playerManager = new DefaultAudioPlayerManager();
            registerSourceManagers(playerManager);

            //Patrons and development get higher quality
            AudioConfiguration.ResamplingQuality quality = AudioConfiguration.ResamplingQuality.LOW;
            if (Config.CONFIG.getDistribution() == DistributionEnum.PATRON || Config.CONFIG.getDistribution() == DistributionEnum.DEVELOPMENT)
                quality = AudioConfiguration.ResamplingQuality.MEDIUM;

            playerManager.getConfiguration().setResamplingQuality(quality);
            playerManager.enableGcMonitoring();
            playerManager.setFrameBufferDuration(1000);

            if (Config.CONFIG.getDistribution() != DistributionEnum.DEVELOPMENT && Config.CONFIG.isLavaplayerNodesEnabled()) {
                playerManager.useRemoteNodes(Config.CONFIG.getLavaplayerNodes());
            }
            
            playerManager.setItemLoaderThreadPoolSize(500);
        }
    }

    public static AudioPlayerManager registerSourceManagers(AudioPlayerManager mng) {
        mng.registerSourceManager(new PlaylistImportSourceManager());
        //Determine which Source managers are enabled
        //By default, all are enabled except HttpAudioSources
        if (Config.CONFIG.isYouTubeEnabled()) {
            YoutubeAudioSourceManager youtubeAudioSourceManager = new YoutubeAudioSourceManager();
            youtubeAudioSourceManager.configureRequests(config -> RequestConfig.copy(config)
                    .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                    .build());
            mng.registerSourceManager(youtubeAudioSourceManager);
        }
        if (Config.CONFIG.isSoundCloudEnabled()) {
            mng.registerSourceManager(new SoundCloudAudioSourceManager());
        }
        if (Config.CONFIG.isBandCampEnabled()) {
            mng.registerSourceManager(new BandcampAudioSourceManager());
        }
        if (Config.CONFIG.isTwitchEnabled()) {
            mng.registerSourceManager(new TwitchStreamAudioSourceManager());
        }
        if (Config.CONFIG.isVimeoEnabled()) {
            mng.registerSourceManager(new VimeoAudioSourceManager());
        }
        if (Config.CONFIG.isMixerEnabled()) {
            mng.registerSourceManager(new BeamAudioSourceManager());
        }
        if (Config.CONFIG.isSpotifyEnabled()) {
            mng.registerSourceManager(new SpotifyPlaylistSourceManager());
        }
        if (Config.CONFIG.isHttpEnabled()) {
            //add new source managers above the HttpAudio one, because it will either eat your request or throw an exception
            //so you will never reach a source manager below it
            mng.registerSourceManager(new HttpAudioSourceManager());
        }
        return mng;
    }

    public void play() {
        log.debug("play()");

        if (player.isPaused()) {
            player.setPaused(false);
        }
        if (player.getPlayingTrack() == null) {
            loadAndPlay();
        }

    }

    public void setPause(boolean pause) {
        log.debug("setPause({})", pause);

        if (pause) {
            player.setPaused(true);
        } else {
            player.setPaused(false);
            play();
        }
    }

    /**
     * Pause the player
     */
    public void pause() {
        log.debug("pause()");

        player.setPaused(true);
    }

    /**
     * Clear the tracklist and stop the current track
     */
    public void stop() {
        log.debug("stop()");

        audioTrackProvider.clear();
        stopTrack();
    }

    /**
     * Skip the current track
     */
    public void skip() {
        log.debug("skip()");

        audioTrackProvider.skipped();
        stopTrack();
    }

    /**
     * Stop the current track.
     */
    public void stopTrack() {
        log.debug("stopTrack()");

        context = null;
        player.stopTrack();
    }

    public boolean isQueueEmpty() {
        log.debug("isQueueEmpty()");

        return player.getPlayingTrack() == null && audioTrackProvider.isEmpty();
    }

    public AudioTrackContext getPlayingTrack() {
        log.debug("getPlayingTrack()");

        if (player.getPlayingTrack() == null && context == null) {
            return audioTrackProvider.peek();
        }


        return context;
    }

    //the unshuffled playlist
    public List<AudioTrackContext> getRemainingTracks() {
        log.debug("getRemainingTracks()");

        //Includes currently playing track, which comes first
        List<AudioTrackContext> list = new ArrayList<>();
        AudioTrackContext atc = getPlayingTrack();
        if (atc != null) {
            list.add(atc);
        }

        list.addAll(audioTrackProvider.getAsList());
        return list;
    }

    public void setVolume(float vol) {
        player.setVolume((int) (vol * 100));
    }

    public float getVolume() {
        return ((float) player.getVolume()) / 100;
    }

    public static AudioPlayerManager getPlayerManager() {
        initAudioPlayerManager();
        return playerManager;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        log.debug("onTrackEnd({} {} {}) called", track.getInfo().title, endReason.name(), endReason.mayStartNext);

        if (endReason == AudioTrackEndReason.FINISHED || endReason == AudioTrackEndReason.STOPPED) {
            loadAndPlay();
        } else if(endReason == AudioTrackEndReason.CLEANUP) {
            log.info("Track " + track.getIdentifier() + " was cleaned up");
        } else if (endReason == AudioTrackEndReason.LOAD_FAILED) {
            if (onErrorHook != null)
                onErrorHook.accept(new MessagingException("Track `" + track.getInfo().title + "` failed to load. Skipping..."));
            audioTrackProvider.skipped();
            loadAndPlay();
        } else {
            log.warn("Track " + track.getIdentifier() + " ended with unexpected reason: " + endReason);
        }
    }

    //request the next track from the track provider and start playing it
    private void loadAndPlay() {
        log.debug("loadAndPlay()");

        AudioTrackContext atc = null;
        if (audioTrackProvider != null) {
            atc = audioTrackProvider.provideAudioTrack();
        } else {
            log.warn("TrackProvider doesn't exist");
        }

        if (atc != null) {
            playTrack(atc);
        }
    }

    /**
     * Plays the provided track.
     * <p>
     * Silently playing a track will not trigger the onPlayHook (which announces the track usually)
     */
    protected void playTrack(AudioTrackContext trackContext, boolean... silent) {
        log.debug("playTrack({})", trackContext.getEffectiveTitle());

        context = trackContext;
        player.playTrack(trackContext.getTrack());
        trackContext.getTrack().setPosition(trackContext.getStartPosition());

        if (trackContext instanceof SplitAudioTrackContext) {
            //Ensure we don't step over our bounds
            log.info("Start: " + trackContext.getStartPosition() + " End: " + (trackContext.getStartPosition() + trackContext.getEffectiveDuration()));

            trackContext.getTrack().setMarker(
                    new TrackMarker(trackContext.getStartPosition() + trackContext.getEffectiveDuration(),
                            new TrackEndMarkerHandler(this, trackContext)));
        }

        if (silent.length < 1 || !silent[0]) {
            if (onPlayHook != null) onPlayHook.accept(trackContext);
        }
    }

    void destroy() {
        log.debug("destroy()");

        stop();
    }

    @Override
    public byte[] provide20MsAudio() {
        return lastFrame.data;
    }

    @Override
    public boolean canProvide() {
        LavaplayerPlayerWrapper lavaplayerPlayer = (LavaplayerPlayerWrapper) player;
        lastFrame = lavaplayerPlayer.provide();

        if(lastFrame == null) {
            audioLossCounter.onLoss();
            return false;
        } else {
            audioLossCounter.onSuccess();
            return true;
        }
    }

    public AudioLossCounter getAudioLossCounter() {
        return audioLossCounter;
    }

    @Override
    public boolean isOpus() {
        return true;
    }

    public boolean isPlaying() {
        log.debug("isPlaying()");

        return player.getPlayingTrack() != null && !player.isPaused();
    }

    public boolean isPaused() {
        log.debug("isPaused()");

        return player.isPaused();
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        if(Config.CONFIG.getLavaplayerNodes().length > 0) {
            log.error("Lavaplayer encountered an exception during playback while playing " + track.getIdentifier(), exception);
            log.error("Performance stats for errored track: " + audioLossCounter);
        }
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        log.error("Lavaplayer got stuck while playing " + track.getIdentifier() + "\nPerformance stats for stuck track: " + audioLossCounter);
    }

    public long getPosition() {
        return player.getTrackPosition();
    }

    public void seekTo(long position) {
        player.seekTo(position);
    }

    public IPlayer getPlayer() {
        return player;
    }
}
