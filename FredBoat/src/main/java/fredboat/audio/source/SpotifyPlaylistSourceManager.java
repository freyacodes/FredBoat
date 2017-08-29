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
 */

package fredboat.audio.source;

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import fredboat.audio.queue.PlaylistInfo;
import fredboat.util.rest.SearchUtil;
import fredboat.util.rest.SpotifyAPIWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by napster on 08.03.17.
 * <p>
 * Loads playlists from Spotify playlist links.
 *
 * todo bulk load the songs from the search cache (remote db connections are slow when loading one by one)
 *
 * @author napster
 */
public class SpotifyPlaylistSourceManager implements AudioSourceManager, PlaylistImporter {

    public static long CACHE_DURATION = TimeUnit.DAYS.toMillis(7);// 1 week;

    private static final Logger log = LoggerFactory.getLogger(SpotifyPlaylistSourceManager.class);

    //https://regex101.com/r/AEWyxi/3
    private static final Pattern PLAYLIST_PATTERN = Pattern.compile("https?://.*\\.spotify\\.com/user/(.*)/playlist/([^?/\\s]*)");

    //Take care when deciding on upping the core pool size: The threads may hog database connections
    // (for selfhosters running on the SQLite db) when loading an uncached playlist.
    // Upping the threads will also fire search requests more aggressively against Youtube which is probably better avoided.
    private static final ScheduledExecutorService loader = Executors.newScheduledThreadPool(1);

    private static final List<SearchUtil.SearchProvider> searchProviders
            = Arrays.asList(SearchUtil.SearchProvider.YOUTUBE, SearchUtil.SearchProvider.SOUNDCLOUD);

    @Override
    public String getSourceName() {
        return "spotify_playlist_import";
    }

    @Override
    public AudioItem loadItem(final DefaultAudioPlayerManager manager, final AudioReference ar) {

        String[] data = parse(ar.identifier);
        if (data == null) return null;
        final String spotifyUser = data[0];
        final String spotifyListId = data[1];

        final SpotifyAPIWrapper saw = SpotifyAPIWrapper.getApi();

        PlaylistInfo plData;
        try {
            plData = saw.getPlaylistDataBlocking(spotifyUser, spotifyListId);
        } catch (Exception e) {
            log.warn("Could not retrieve playlist " + spotifyListId + " of user " + spotifyUser, e);
            throw new FriendlyException("Couldn't load playlist. Either Spotify is down or the playlist does not exist.", FriendlyException.Severity.COMMON, e);
        }

        String playlistName = plData.getName();
        if (playlistName == null || "".equals(playlistName)) playlistName = "Spotify Playlist";
        int tracksTotal = plData.getTotalTracks();

        final List<AudioTrack> trackList = new ArrayList<>();
        final List<String> trackListSearchTerms;

        try {
            trackListSearchTerms = saw.getPlaylistTracksSearchTermsBlocking(spotifyUser, spotifyListId);
        } catch (Exception e) {
            log.warn("Could not retrieve tracks for playlist " + spotifyListId + " of user " + spotifyUser, e);
            throw new FriendlyException("Couldn't load playlist. Either Spotify is down or the playlist does not exist.", FriendlyException.Severity.COMMON, e);
        }
        log.info("Retrieved playlist data for " + playlistName + " from Spotify, loading up " + tracksTotal + " tracks");

        //build a task list
        List<CompletableFuture<AudioTrack>> taskList = new ArrayList<>();
        for (final String s : trackListSearchTerms) {
            //remove all punctuation
            final String query = s.replaceAll(SearchUtil.PUNCTUATION_REGEX, "");

            CompletableFuture<AudioTrack> f = CompletableFuture.supplyAsync(() -> searchSingleTrack(query), loader);
            taskList.add(f);
        }

        //build a tracklist from that task list
        for (CompletableFuture<AudioTrack> futureTrack : taskList) {
            try {
                final AudioTrack audioItem = futureTrack.get();
                if (audioItem == null) {
                    continue; //skip the track if we couldn't find it
                }
                trackList.add(audioItem);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ExecutionException ignored) {
                //this is fine, loop will go for the next item
            }
        }
        return new BasicAudioPlaylist(playlistName, trackList, null, true);
    }

    /**
     * Searches all available searching sources for a single track.
     * <p>
     * Will go Youtube > SoundCloud > return null
     * This could probably be moved to SearchUtil
     *
     * @param query Term that shall be searched
     * @return An AudioTrack likely corresponding to the query term or null.
     */
    private AudioTrack searchSingleTrack(final String query) {
        try {
            AudioPlaylist list = SearchUtil.searchForTracks(query, CACHE_DURATION, 60000, searchProviders);
            //didn't find anything
            if (list == null || list.getTracks().isEmpty()) {
                return null;
            }

            //pick topmost result, and hope it's what the user wants to listen to
            //having users pick tracks like they can do for individual searches would be ridiculous for playlists with
            //dozens of tracks. youtube search is probably good enough for this
            //
            //testcase:   Rammstein playlists; high quality Rammstein vids are really rare on Youtube.
            //            https://open.spotify.com/user/11174036433/playlist/0ePRMvD3Dn3zG31A8y64xX
            //result:     lots of low quality (covers, pitched up/down, etc) tracks loaded.
            //conclusion: there's room for improvement to this whole method
            return list.getTracks().get(0);
        } catch (SearchUtil.SearchingException e) {
            //youtube & soundcloud not available
            return null;
        }
    }

    @Override
    public boolean isTrackEncodable(final AudioTrack track) {
        return false;
    }

    @Override
    public void encodeTrack(final AudioTrack track, final DataOutput output) throws IOException {
        throw new UnsupportedOperationException("This source manager is only for loading playlists");
    }

    @Override
    public AudioTrack decodeTrack(final AudioTrackInfo trackInfo, final DataInput input) throws IOException {
        throw new UnsupportedOperationException("This source manager is only for loading playlists");
    }

    @Override
    public void shutdown() {

    }

    /**
     * @return null or a string array containing spotifyUser at [0] and playlistId at [1] of the requested playlist
     */
    private String[] parse(String identifier) {
        String[] result = new String[2];
        final Matcher m = PLAYLIST_PATTERN.matcher(identifier);

        if (!m.find()) {
            return null;
        }

        result[0] = m.group(1);
        result[1] = m.group(2);

        log.debug("matched spotify playlist link. user: " + result[0] + ", listId: " + result[1]);
        return result;
    }

    @Override
    public PlaylistInfo getPlaylistDataBlocking(String identifier) {

        String[] data = parse(identifier);
        if (data == null) return null;
        final String spotifyUser = data[0];
        final String spotifyListId = data[1];

        try {
            return SpotifyAPIWrapper.getApi().getPlaylistDataBlocking(spotifyUser, spotifyListId);
        } catch (Exception e) {
            log.warn("Could not retrieve playlist " + spotifyListId + " of user " + spotifyUser, e);
            throw new FriendlyException("Couldn't load playlist. Either Spotify is down or the playlist does not exist.", FriendlyException.Severity.COMMON, e);
        }
    }
}
