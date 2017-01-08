/*
 * MIT License
 *
 * Copyright (c) 2016 Frederik Ar. Mikkelsen
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

package fredboat.audio.queue;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fredboat.audio.GuildPlayer;
import fredboat.util.TextUtils;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;

public class AudioLoader implements AudioLoadResultHandler {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(AudioLoader.class);

    private final ITrackProvider trackProvider;
    private final AudioPlayerManager playerManager;
    private final GuildPlayer gplayer;
    private final ConcurrentLinkedQueue<IdentifierContext> identifierQueue = new ConcurrentLinkedQueue<>();
    private IdentifierContext context = null;
    private volatile boolean isLoading = false;

    public AudioLoader(ITrackProvider trackProvider, AudioPlayerManager playerManager, GuildPlayer gplayer) {
        this.trackProvider = trackProvider;
        this.playerManager = playerManager;
        this.gplayer = gplayer;
    }

    public void loadAsync(IdentifierContext ic) {
        identifierQueue.add(ic);
        if (!isLoading) {
            loadNextAsync();
        }
    }

    private void loadNextAsync() {
        try {
            IdentifierContext ic = identifierQueue.poll();
            if (ic != null) {
                isLoading = true;
                context = ic;
                playerManager.loadItem(ic.identifier, this);
            } else {
                isLoading = false;
            }
        } catch (Throwable th) {
            handleThrowable(context, th);
            isLoading = false;
        }
    }

    @Override
    public void trackLoaded(AudioTrack at) {
        try {
            if (!context.isQuiet()) {
                context.textChannel.sendMessage(
                        gplayer.isPlaying() ? "**" + at.getInfo().title + "** has been added to the queue." : "**" + at.getInfo().title + "** will now play."
                ).queue();
            } else {
                log.info("Quietly loaded " + at.getIdentifier());
            }

            at.setPosition(context.getPosition());

            trackProvider.add(new AudioTrackContext(at, context.member));
            if (!gplayer.isPaused()) {
                gplayer.play();
            }
        } catch (Throwable th) {
            handleThrowable(context, th);
        }
        loadNextAsync();
    }

    @Override
    public void playlistLoaded(AudioPlaylist ap) {
        try {
            context.textChannel.sendMessage(
                    "Found and added `" + ap.getTracks().size() + "` songs from playlist **" + ap.getName() + "**."
            ).queue();

            for (AudioTrack at : ap.getTracks()) {
                trackProvider.add(new AudioTrackContext(at, context.member));
            }
            if (!gplayer.isPaused()) {
                gplayer.play();
            }
        } catch (Throwable th) {
            handleThrowable(context, th);
        }
        loadNextAsync();
    }

    @Override
    public void noMatches() {
        try {
            context.textChannel.sendMessage("No audio could be found for `" + context.identifier + "`.").queue();
        } catch (Throwable th) {
            handleThrowable(context, th);
        }
        loadNextAsync();
    }

    @Override
    public void loadFailed(FriendlyException fe) {
        handleThrowable(context, fe);

        loadNextAsync();
    }

    @SuppressWarnings("ThrowableResultIgnored")
    private void handleThrowable(IdentifierContext ic, Throwable th) {
        try {
            if (th instanceof FriendlyException) {
                FriendlyException fe = (FriendlyException) th;
                if (fe.severity == FriendlyException.Severity.COMMON) {
                    if (ic.textChannel != null) {
                        context.textChannel.sendMessage("Error occurred when loading info for `" + context.identifier + "`:\n"
                        + fe.getMessage()).queue();
                    } else {
                        log.error("Error while loading track ", th);
                    }
                } else if (ic.textChannel != null) {
                    context.textChannel.sendMessage("Suspicious error when loading info for `" + context.identifier + "`.").queue();
                    Throwable exposed = fe.getCause() == null ? fe : fe.getCause();
                    TextUtils.handleException(exposed, context.textChannel);
                } else {
                    log.error("Error while loading track ", th);
                }
            } else if (ic.textChannel != null) {
                context.textChannel.sendMessage("Suspicious error when loading info for `" + context.identifier + "`.").queue();
                TextUtils.handleException(th, context.textChannel);
            } else {
                log.error("Error while loading track ", th);
            }
        } catch (Exception e) {
            log.error("Error when trying to handle another error", th);
            log.error("DEBUG", e);
        }
    }

}
