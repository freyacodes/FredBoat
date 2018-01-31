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
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.source.PlaylistImportSourceManager;
import fredboat.audio.source.PlaylistImporter;
import fredboat.audio.source.SpotifyPlaylistSourceManager;
import fredboat.feature.metrics.Metrics;
import fredboat.feature.togglz.FeatureFlags;
import fredboat.messaging.CentralMessaging;
import fredboat.util.TextUtils;
import fredboat.util.ratelimit.Ratelimiter;
import fredboat.util.rest.YoutubeAPI;
import fredboat.util.rest.YoutubeVideo;
import net.dv8tion.jda.core.MessageBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
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

        if (ratelimitIfSlowLoadingPlaylistAndAnnounce(ic)) {
            identifierQueue.add(ic);
            if (!isLoading) {
                loadNextAsync();
            }
        }
    }

    private void loadNextAsync() {
        try {
            IdentifierContext ic = identifierQueue.poll();
            if (ic != null) {
                isLoading = true;
                context = ic;

                if (gplayer.getTrackCount() >= QUEUE_TRACK_LIMIT) {
                    context.replyWithName(context.i18nFormat("loadQueueTrackLimit", QUEUE_TRACK_LIMIT));
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

    /**
     * If the requested item is a slow loading playlist that we know of, check for rate limits and announce to the user
     * that it might take a while to gather it.
     *
     * @return false if the user is not allowed to load the playlist, true if he is
     */
    private boolean ratelimitIfSlowLoadingPlaylistAndAnnounce(IdentifierContext ic) {
        PlaylistInfo playlistInfo = getSlowLoadingPlaylistData(ic.identifier);

        if (playlistInfo == null) //not a slow loading playlist
            return true;
        else {
            boolean result = true;
            if (FeatureFlags.RATE_LIMITER.isActive()) {
                result = Ratelimiter.getRatelimiter().isAllowed(ic, playlistInfo, playlistInfo.getTotalTracks()).a;
            }

            if (result) {
                //inform user we are possibly about to do nasty time consuming work
                if (playlistInfo.getTotalTracks() > 50) {
                    ic.replyWithName(ic.i18nFormat("loadAnnouncePlaylist",
                            playlistInfo.getName(), playlistInfo.getTotalTracks()));
                }
                return true;
            } else {
                ic.replyWithMention(ic.i18n("ratelimitedGuildSlowLoadingPlaylist"));
                return false;
            }
        }
    }

    /**
     * this function needs to be updated if we add more manual playlist loaders
     * currently it only covers the Hastebin and Spotify playlists
     *
     * @param identifier the very same identifier that the playlist loaders will be presented with if we asked them to
     *                   load a playlist
     * @return null if it's not a playlist that we manually parse, some data about it if it is
     */
    private PlaylistInfo getSlowLoadingPlaylistData(String identifier) {

        PlaylistInfo playlistInfo = null;
        PlaylistImporter pi = playerManager.source(SpotifyPlaylistSourceManager.class);
        if (pi != null) {
            playlistInfo = pi.getPlaylistDataBlocking(identifier);
        }

        if (playlistInfo == null) {
            pi = playerManager.source(PlaylistImportSourceManager.class);
            if (pi != null) {
                playlistInfo = pi.getPlaylistDataBlocking(identifier);
            }
        }

        //can be null
        return playlistInfo;
    }

    @Override
    public void trackLoaded(AudioTrack at) {
        Metrics.tracksLoaded.inc();
        try {
            if(context.isSplit()){
                loadSplit(at, context);
            } else {

                if (!context.isQuiet()) {
                    context.reply(gplayer.isPlaying() ?
                            context.i18nFormat("loadSingleTrack", TextUtils.escapeAndDefuse(at.getInfo().title))
                            :
                            context.i18nFormat("loadSingleTrackAndPlay", TextUtils.escapeAndDefuse(at.getInfo().title))
                    );
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
        Metrics.tracksLoaded.inc(ap.getTracks() == null ? 0 : ap.getTracks().size());
        try {
            if(context.isSplit()){
                context.reply(context.i18n("loadPlaySplitListFail"));
                loadNextAsync();
                return;
            }

            List<AudioTrackContext> toAdd = new ArrayList<>();
            for (AudioTrack at : ap.getTracks()) {
                toAdd.add(new AudioTrackContext(at, context.getMember()));
            }
            trackProvider.addAll(toAdd);
            context.reply(context.i18nFormat("loadListSuccess", ap.getTracks().size(), ap.getName()));
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
            context.reply(context.i18nFormat("loadNoMatches", context.identifier));
        } catch (Throwable th) {
            handleThrowable(context, th);
        }
        loadNextAsync();
    }

    @Override
    public void loadFailed(FriendlyException fe) {
        Metrics.trackLoadsFailed.inc();
        handleThrowable(context, fe);

        loadNextAsync();
    }

    private void loadSplit(AudioTrack at, IdentifierContext ic){
        if(!(at instanceof YoutubeAudioTrack)){
            ic.reply(ic.i18n("loadSplitNotYouTube"));
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
            ic.reply(ic.i18n("loadSplitNotResolves"));
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

        MessageBuilder mb = CentralMessaging.getClearThreadLocalMessageBuilder()
                .append(ic.i18n("loadFollowingTracksAdded")).append("\n");
        for(SplitAudioTrackContext atc : list) {
            mb.append("`[")
                    .append(TextUtils.formatTime(atc.getEffectiveDuration()))
                    .append("]` ")
                    .append(TextUtils.escapeAndDefuse(atc.getEffectiveTitle()))
                    .append("\n");
        }

        //This is pretty spammy .. let's use a shorter one
        if(mb.length() > 800){
            mb = CentralMessaging.getClearThreadLocalMessageBuilder()
                    .append(ic.i18nFormat("loadPlaylistTooMany", list.size()));
        }

        context.reply(mb.build());
    }

    @SuppressWarnings("ThrowableResultIgnored")
    private void handleThrowable(IdentifierContext ic, Throwable th) {
        try {
            if (th instanceof FriendlyException) {
                FriendlyException fe = (FriendlyException) th;
                if (fe.severity == FriendlyException.Severity.COMMON) {
                    if (ic.getTextChannel() != null) {
                        context.reply(ic.i18nFormat("loadErrorCommon", context.identifier, fe.getMessage()));
                    } else {
                        log.error("Error while loading track ", th);
                    }
                } else if (ic.getTextChannel() != null) {
                    context.reply(ic.i18nFormat("loadErrorSusp", context.identifier));
                    Throwable exposed = fe.getCause() == null ? fe : fe.getCause();
                    TextUtils.handleException(exposed, context);
                } else {
                    log.error("Error while loading track ", th);
                }
            } else if (ic.getTextChannel() != null) {
                context.reply(ic.i18n("loadErrorSusp"));
                TextUtils.handleException(th, context);
            } else {
                log.error("Error while loading track ", th);
            }
        } catch (Exception e) {
            log.error("Error when trying to handle another error", th);
            log.error("DEBUG", e);
        }
    }

}
