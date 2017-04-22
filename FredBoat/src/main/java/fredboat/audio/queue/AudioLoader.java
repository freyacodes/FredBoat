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

package fredboat.audio.queue;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fredboat.audio.GuildPlayer;
import fredboat.feature.I18n;
import fredboat.util.TextUtils;
import fredboat.util.YoutubeAPI;
import fredboat.util.YoutubeVideo;
import net.dv8tion.jda.core.MessageBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AudioLoader implements AudioLoadResultHandler {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(AudioLoader.class);

    //Matches a timestamp and the description
    private static final Pattern SPLIT_DESCRIPTION_PATTERN = Pattern.compile("(.*?)[( \\[]*((?:\\d?\\d:)?\\d?\\d:\\d\\d)[) \\]]*(.*)");
    private static final int QUEUE_TRACK_LIMIT = 10000;

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

                if (gplayer.getRemainingTracks().size() >= QUEUE_TRACK_LIMIT) {
                    TextUtils.replyWithName(gplayer.getActiveTextChannel(), context.getMember(), "You can't add tracks to a queue with more than " + QUEUE_TRACK_LIMIT + " tracks! This is to prevent abuse.");
                    isLoading = false;
                    return;
                }

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
            if(context.isSplit()){
                loadSplit(at, context);
            } else {

                if (!context.isQuiet()) {
                    context.getTextChannel().sendMessage(
                            gplayer.isPlaying() ?
                                    MessageFormat.format(I18n.get(context.getMember().getGuild()).getString("loadSingleTrack"), at.getInfo().title)
                                    :
                                    MessageFormat.format(I18n.get(context.getMember().getGuild()).getString("loadSingleTrackAndPlay"), at.getInfo().title)
                    ).queue();
                } else {
                    log.info("Quietly loaded " + at.getIdentifier());
                }

                at.setPosition(context.getPosition());

                trackProvider.add(new AudioTrackContext(at, context.getMember()));
                if (!gplayer.isPaused()) {
                    gplayer.play();
                }
            }
        } catch (Throwable th) {
            handleThrowable(context, th);
        }
        loadNextAsync();
    }

    @Override
    public void playlistLoaded(AudioPlaylist ap) {
        try {
            if(context.isSplit()){
                TextUtils.replyWithName(context.getTextChannel(), context.getMember(), I18n.get(context.getTextChannel().getGuild()).getString("loadPlaySplitListFail"));
                loadNextAsync();
                return;
            }

            context.getTextChannel().sendMessage(
                    MessageFormat.format(I18n.get(context.getTextChannel().getGuild()).getString("loadListSuccess"), ap.getTracks().size(), ap.getName())
            ).queue();

            for (AudioTrack at : ap.getTracks()) {
                trackProvider.add(new AudioTrackContext(at, context.getMember()));
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
            context.getTextChannel().sendMessage(MessageFormat.format(I18n.get(context.getTextChannel().getGuild()).getString("loadNoMatches"), context.identifier)).queue();
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

    private void loadSplit(AudioTrack at, IdentifierContext ic){
        if(!(at instanceof YoutubeAudioTrack)){
            ic.getTextChannel().sendMessage(I18n.get(ic.getTextChannel().getGuild()).getString("loadSplitNotYouTube")).queue();
            return;
        }
        YoutubeAudioTrack yat = (YoutubeAudioTrack) at;

        YoutubeVideo yv = YoutubeAPI.getVideoFromID(yat.getIdentifier(), true);
        String desc = yv.getDescription();
        Matcher m = SPLIT_DESCRIPTION_PATTERN.matcher(desc);

        ArrayList<Pair<Long, String>> pairs = new ArrayList<>();

        while(m.find()) {
            long timestamp;
            try {
                timestamp = TextUtils.parseTimeString(m.group(2));
            } catch (NumberFormatException e) {
                continue;
            }

            String title1 = m.group(1);
            String title2 = m.group(3);
            
            if(title1.length() > title2.length()) {
                pairs.add(new ImmutablePair<>(timestamp, title1));
            } else {
                pairs.add(new ImmutablePair<>(timestamp, title2));
            }


        }

        if(pairs.size() < 2) {
            ic.getTextChannel().sendMessage(I18n.get(ic.getTextChannel().getGuild()).getString("loadSplitNotResolves")).queue();
            return;
        }

        ArrayList<SplitAudioTrackContext> list = new ArrayList<>();

        int i = 0;
        for(Pair<Long, String> pair : pairs){
            long startPos;
            long endPos;

            if(i != pairs.size() - 1){
                // Not last
                startPos = pair.getLeft();
                endPos = pairs.get(i + 1).getLeft();
            } else {
                // Last
                startPos = pair.getLeft();
                endPos = at.getDuration();
            }

            AudioTrack newAt = at.makeClone();
            newAt.setPosition(startPos);

            SplitAudioTrackContext atc = new SplitAudioTrackContext(newAt, ic.getMember(), startPos, endPos, pair.getRight());

            list.add(atc);
            gplayer.queue(atc);

            i++;
        }

        MessageBuilder mb = new MessageBuilder()
                .append(I18n.get(ic.getTextChannel().getGuild()).getString("loadFollowingTracksAdded") + "\n");
        for(SplitAudioTrackContext atc : list) {
            mb.append("`[")
                    .append(TextUtils.formatTime(atc.getEffectiveDuration()))
                    .append("]` ")
                    .append(atc.getEffectiveTitle())
                    .append("\n");
        }

        //This is pretty spammy .. let's use a shorter one
        if(mb.length() > 800){
            mb = new MessageBuilder()
                    .append(MessageFormat.format(I18n.get(ic.getTextChannel().getGuild()).getString("loadPlaylistTooMany"), list.size()));
        }

        context.getTextChannel().sendMessage(mb.build()).queue();

    }

    @SuppressWarnings("ThrowableResultIgnored")
    private void handleThrowable(IdentifierContext ic, Throwable th) {
        try {
            if (th instanceof FriendlyException) {
                FriendlyException fe = (FriendlyException) th;
                if (fe.severity == FriendlyException.Severity.COMMON) {
                    if (ic.getTextChannel() != null) {
                        context.getTextChannel().sendMessage(MessageFormat.format(I18n.get(ic.getTextChannel().getGuild()).getString("loadErrorCommon"), context.identifier, fe.getMessage())).queue();
                    } else {
                        log.error("Error while loading track ", th);
                    }
                } else if (ic.getTextChannel() != null) {
                    context.getTextChannel().sendMessage(MessageFormat.format(I18n.get(ic.getTextChannel().getGuild()).getString("loadErrorSusp"), context.identifier)).queue();
                    Throwable exposed = fe.getCause() == null ? fe : fe.getCause();
                    TextUtils.handleException(exposed, context.getTextChannel());
                } else {
                    log.error("Error while loading track ", th);
                }
            } else if (ic.getTextChannel() != null) {
                context.getTextChannel().sendMessage(I18n.get(ic.getTextChannel().getGuild()).getString("loadErrorSusp")).queue();
                TextUtils.handleException(th, context.getTextChannel());
            } else {
                log.error("Error while loading track ", th);
            }
        } catch (Exception e) {
            log.error("Error when trying to handle another error", th);
            log.error("DEBUG", e);
        }
    }

}
