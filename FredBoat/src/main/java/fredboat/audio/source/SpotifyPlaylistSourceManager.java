package fredboat.audio.source;

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.*;
import com.wrapper.spotify.models.Playlist;
import com.wrapper.spotify.models.PlaylistTrack;
import com.wrapper.spotify.models.SimpleArtist;
import com.wrapper.spotify.models.Track;
import fredboat.util.SpotifyAPIWrapper;
import fredboat.util.YoutubeAPI;
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
 * @author napster
 */
public class SpotifyPlaylistSourceManager implements AudioSourceManager {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(SpotifyPlaylistSourceManager.class);

    //https://regex101.com/r/AEWyxi/1
    private static final Pattern PLAYLIST_PATTERN = Pattern.compile("^https?://.*\\.spotify\\.com/user/(.*)/playlist/(.*)$");


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

        log.info("matched spotify playlist link! user: " + spotifyUser + ", listId: " + spotifyListId);

        final SpotifyAPIWrapper saw = SpotifyAPIWrapper.getApi();
        final Playlist playlist;
        try {
            playlist = saw.getPlaylist(spotifyUser, spotifyListId).get();
        } catch (final Exception e) {
            log.error("Could not get playlist " + spotifyListId + " of user " + spotifyUser, e);
            return null;
        }
        log.info("Retrieved playlist " + playlist.getName() + " from spotify with " + playlist.getTracks().getTotal() + " tracks");

        final List<AudioTrack> trackList = new ArrayList<>();
        for (final PlaylistTrack t : playlist.getTracks().getItems()) {
            final Track track = t.getTrack();
            final StringBuilder sb = new StringBuilder();
            sb.append(track.getName());
            track.getArtists().forEach((SimpleArtist artist) -> sb.append(" ").append(artist.getName()));
            String query = sb.toString();

            //remove all punctuation
            query = query.replaceAll("[.,/#!$%\\^&*;:{}=\\-_`~()]", "");

            final AudioPlaylist list;
            try {
                list = YoutubeAPI.searchForVideos(query);
            } catch (final JSONException e) {
                log.debug("YouTube search exception", e);
                continue; //look for the next track
            }
            if (list == null || list.getTracks().size() == 0) {
                continue; //look for the next track TODO: inform about not being able to find a track?
            }
            //pick top most result and hope it's what the user wants to listen to
            trackList.add(list.getTracks().get(0));
        }

        //TODO does it only load 100 tracks at a time? maybe related to that com.wrapper.spotify.models.Page thing
        //TODO holding them?

        //TODO: say something as soon as the search starts

        //TODO: play the first track as soon as possible while continuing to search

        //TODO: are ppl being informed properly for all exceptions?

        //TODO: java documentation
        return new BasicAudioPlaylist(playlist.getName(), trackList, null, false);
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
