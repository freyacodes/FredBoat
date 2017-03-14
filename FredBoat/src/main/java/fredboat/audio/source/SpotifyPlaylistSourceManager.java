package fredboat.audio.source;

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;
import fredboat.util.SearchUtil;
import fredboat.util.SpotifyAPIWrapper;
import org.json.JSONException;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by napster on 08.03.17.
 *
 * Loads playlists from Spotify playlist links.
 *
 * @author napster
 */
public class SpotifyPlaylistSourceManager implements AudioSourceManager {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(SpotifyPlaylistSourceManager.class);

    //https://regex101.com/r/AEWyxi/3
    private static final Pattern PLAYLIST_PATTERN = Pattern.compile("https?://.*\\.spotify\\.com/user/(.*)/playlist/([^?/\\s]*)");


    @Override
    public String getSourceName() {
        return "spotify_playlist_import";
    }

    @Override
    public AudioItem loadItem(final DefaultAudioPlayerManager manager, final AudioReference ar) {
        final Matcher m = PLAYLIST_PATTERN.matcher(ar.identifier);

        if (!m.find()) {
            return null;
        }

        final String spotifyUser = m.group(1);
        final String spotifyListId = m.group(2);

        log.debug("matched spotify playlist link. user: " + spotifyUser + ", listId: " + spotifyListId);

        final SpotifyAPIWrapper saw = SpotifyAPIWrapper.getApi();

        String[] plData;
        try {
            plData = saw.getPlaylistData(spotifyUser, spotifyListId);
        } catch (Exception e) {
            log.warn("Could not retrieve playlist " + spotifyListId + " of user " + spotifyUser, e);
            throw new FriendlyException("Couldn't load playlist. Either Spotify is down or the playlist does not exist.", FriendlyException.Severity.COMMON, e);
        }

        String playlistName = plData[0];
        if (playlistName == null || "".equals(playlistName)) playlistName = "Spotify Playlist";
        String tracksTotal = plData[1];

        final List<AudioTrack> trackList = new ArrayList<>();
        final List<String> trackListSearchTerms;

        try {
            trackListSearchTerms = saw.getPlaylistTracksSearchTerms(spotifyUser, spotifyListId);
        } catch (Exception e) {
            log.warn("Could not retrieve tracks for playlist " + spotifyListId + " of user " + spotifyUser, e);
            throw new FriendlyException("Couldn't load playlist. Either Spotify is down or the playlist does not exist.", FriendlyException.Severity.COMMON, e);
        }
        log.info("Retrieved playlist data for " + playlistName + " from Spotify, loading up " + tracksTotal + " tracks");

        for (final String s : trackListSearchTerms) {

            String query = s;
            //remove all punctuation
            query = query.replaceAll("[.,/#!$%\\^&*;:{}=\\-_`~()]", "");

            final AudioTrack audioItem = searchSingleTrack(query);
            if (audioItem == null) {
                continue; //skip the track if we couldn't find it
            }
            trackList.add(audioItem);
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
        boolean gotYoutubeResult = true;
        AudioPlaylist list = null;
        try {

            list = SearchUtil.searchForTracks(SearchUtil.SearchProvider.YOUTUBE, query);
            if (list == null || list.getTracks().size() == 0) {
                gotYoutubeResult = false;
            }
        } catch (final JSONException e) {
            log.debug("YouTube search exception", e);
            gotYoutubeResult = false;
        }

        //got a result from youtube? return it
        if (gotYoutubeResult)
            return list.getTracks().get(0);


        //continue looking for the track on SoundCloud
        try {
            list = SearchUtil.searchForTracks(SearchUtil.SearchProvider.SOUNDCLOUD, query);
        } catch (final JSONException e) {
            log.debug("SoundCloud search exception", e);
        }

        //didn't find anything, or youtube & soundcloud not available
        if (list == null || list.getTracks().size() == 0) {
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
}
