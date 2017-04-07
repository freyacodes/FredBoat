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

package fredboat.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
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
import fredboat.util.DistributionEnum;
import net.dv8tion.jda.core.audio.AudioSendHandler;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractPlayer extends AudioEventAdapter implements AudioSendHandler {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(AbstractPlayer.class);

    private static AudioPlayerManager playerManager;
    private AudioPlayer player;
    ITrackProvider audioTrackProvider;
    private AudioFrame lastFrame = null;
    private AudioTrackContext context;
    private AudioLossCounter audioLossCounter = new AudioLossCounter();
    private boolean splitTrackEnded = false;

    @SuppressWarnings("LeakingThisInConstructor")
    AbstractPlayer() {
        initAudioPlayerManager();
        player = playerManager.createPlayer();

        player.addListener(this);
    }

    private static void initAudioPlayerManager() {
        if (playerManager == null) {
            playerManager = new DefaultAudioPlayerManager();
            registerSourceManagers(playerManager);

            //Patrons get higher quality
            AudioConfiguration.ResamplingQuality quality = Config.CONFIG.getDistribution() == DistributionEnum.PATRON ? AudioConfiguration.ResamplingQuality.HIGH : AudioConfiguration.ResamplingQuality.LOW;
            playerManager.getConfiguration().setResamplingQuality(quality);
            playerManager.enableGcMonitoring();

            if (Config.CONFIG.getDistribution() != DistributionEnum.PATRON && Config.CONFIG.getDistribution() != DistributionEnum.DEVELOPMENT && Config.CONFIG.isLavaplayerNodesEnabled()) {
                playerManager.useRemoteNodes(Config.CONFIG.getLavaplayerNodes());
            }
        }
    }

    public static AudioPlayerManager registerSourceManagers(AudioPlayerManager mng) {
        mng.registerSourceManager(new YoutubeAudioSourceManager());
        mng.registerSourceManager(new SoundCloudAudioSourceManager());
        mng.registerSourceManager(new BandcampAudioSourceManager());
        mng.registerSourceManager(new PlaylistImportSourceManager());
        mng.registerSourceManager(new TwitchStreamAudioSourceManager());
        mng.registerSourceManager(new VimeoAudioSourceManager());
        mng.registerSourceManager(new BeamAudioSourceManager());
        mng.registerSourceManager(new HttpAudioSourceManager());
        
        return mng;
    }

    public void play() {
        if (player.isPaused()) {
            player.setPaused(false);
        }
        if (player.getPlayingTrack() == null) {
            play0(false);
        }

    }

    public void setPause(boolean pause) {
        if (pause) {
            player.setPaused(true);
        } else {
            player.setPaused(false);
            play();
        }
    }

    public void pause() {
        player.setPaused(true);
    }

    public void stop() {
        audioTrackProvider.clear();
        context = null;
        player.stopTrack();
    }

    public void skip() {
        player.stopTrack();
    }

    //used by TrackEndMarkerHandler to differentiate between skips issued by users and tracks finishing playing
    public void splitTrackEnded() {
        splitTrackEnded = true;
        skip();
    }

    public boolean isQueueEmpty() {
        return getPlayingTrack() == null && audioTrackProvider.isEmpty();
    }

    public AudioTrackContext getPlayingTrack() {
        if (player.getPlayingTrack() == null) {
            play0(true);//Ensure we have something to return, unless the queue is really empty
        }

        if (player.getPlayingTrack() == null) {
            context = null;
        }

        return context;
    }

    public List<AudioTrackContext> getQueuedTracks() {
        return audioTrackProvider.getAsList();
    }

    public List<AudioTrackContext> getRemainingTracks() {
        //Includes currently playing track, which comes first
        if (getPlayingTrack() != null) {
            ArrayList<AudioTrackContext> list = new ArrayList<>();
            list.add(getPlayingTrack());
            list.addAll(getQueuedTracks());
            return list;
        } else {
            return getQueuedTracks();
        }
    }

    public List<AudioTrackContext> getRemainingTracksOrdered() {
        List<AudioTrackContext> list = new ArrayList<>();
        if (getPlayingTrack() != null) {
            list.add(getPlayingTrack());
        }

        list.addAll(getAudioTrackProvider().getAsListOrdered());
        return list;
    }

    public void setVolume(float vol) {
        player.setVolume((int) (vol * 100));
    }

    public float getVolume() {
        return ((float) player.getVolume()) / 100;
    }

    public void setAudioTrackProvider(ITrackProvider audioTrackProvider) {
        this.audioTrackProvider = audioTrackProvider;
    }

    public ITrackProvider getAudioTrackProvider() {
        return audioTrackProvider;
    }

    public static AudioPlayerManager getPlayerManager() {
        initAudioPlayerManager();
        return playerManager;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason == AudioTrackEndReason.FINISHED) {
            play0(false);
        } else if(endReason == AudioTrackEndReason.STOPPED) {
            play0(true);
        }
    }

    private void play0(boolean skipped) {
        boolean userSkip = skipped;
        if (audioTrackProvider != null) {
            if (splitTrackEnded) {
                userSkip = false;
                splitTrackEnded = false;
            }
            context = audioTrackProvider.provideAudioTrack(userSkip);

            if(context != null) {
                player.playTrack(context.getTrack());
                context.getTrack().setPosition(context.getStartPosition());

                if(context instanceof SplitAudioTrackContext){
                    //Ensure we don't step over our bounds
                    log.info("Start: " + context.getStartPosition() + "End: " + (context.getStartPosition() + context.getEffectiveDuration()));

                    context.getTrack().setMarker(
                            new TrackMarker(context.getStartPosition() + context.getEffectiveDuration(),
                                    new TrackEndMarkerHandler(this, context)));
                }
            }
        } else {
            log.warn("TrackProvider doesn't exist");
        }
    }

    void destroy() {
        player.destroy();
    }

    @Override
    public byte[] provide20MsAudio() {
        return lastFrame.data;
    }

    @Override
    public boolean canProvide() {
        lastFrame = player.provide();

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
        return player.getPlayingTrack() != null && !player.isPaused();
    }

    public boolean isPaused() {
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
}
