package fredboat.util;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.wrapper.spotify.Api;
import com.wrapper.spotify.methods.PlaylistRequest;
import com.wrapper.spotify.methods.PlaylistTracksRequest;
import com.wrapper.spotify.models.ClientCredentials;
import com.wrapper.spotify.models.Page;
import com.wrapper.spotify.models.Playlist;
import com.wrapper.spotify.models.PlaylistTrack;
import fredboat.Config;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by napster on 08.03.17.
 *
 * @author napster
 *
 * When expanding this class, make sure to call refreshTokenIfNecessary() before every request
 */
public class SpotifyAPIWrapper {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(SpotifyAPIWrapper.class);
    private static SpotifyAPIWrapper SPOTIFYAPIWRAPPER;

    //https://regex101.com/r/FkknVc/1
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("offset=([0-9]*)&limit=([0-9]*)$");

    /**
     * This should be the only way to grab a handle on this class.
     * //TODO is the Singleton pattern really a good idea for production, or does FredBoat need a different design?
     *
     * @return the singleton of the Spotify API
     */
    public static SpotifyAPIWrapper getApi() {
        if (SPOTIFYAPIWRAPPER == null) {
            SPOTIFYAPIWRAPPER = new SpotifyAPIWrapper(Config.CONFIG.getSpotifyId(), Config.CONFIG.getSpotifySecret());
        }
        return SPOTIFYAPIWRAPPER;
    }


    private final Api spotifyApi;
    private long accessTokenExpires = 0;

    /**
     * Get an instance of this class by using SpotifyAPIWrapper.getApi()
     */
    private SpotifyAPIWrapper() {
        this.spotifyApi = Api.builder().build();
    }

    private SpotifyAPIWrapper(final String clientId, final String clientSecret) {
        this.spotifyApi = Api.builder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();

        getFreshAccessToken();
    }

    /**
     * This is related to the client credentials flow.
     */
    private SettableFuture<ClientCredentials> getFreshAccessToken() {

        final SettableFuture<ClientCredentials> responseFuture = this.spotifyApi.clientCredentialsGrant().build().getAsync();

        Futures.addCallback(responseFuture, new FutureCallback<ClientCredentials>() {
            @Override
            public void onSuccess(final ClientCredentials cc) {
                SpotifyAPIWrapper.this.spotifyApi.setAccessToken(cc.getAccessToken());
                SpotifyAPIWrapper.this.accessTokenExpires = System.currentTimeMillis() + (cc.getExpiresIn() * 1000);
                log.debug("Retrieved spotify access token. Expires in " + cc.getExpiresIn() + " seconds");
            }

            @Override
            public void onFailure(final Throwable throwable) {
                log.error("Could not request spotify access token", throwable);
            }
        });

        return responseFuture;
    }

    /**
     * Call this before doing any requests
     */
    private void refreshTokenIfNecessary() {
        //refresh the token if it's too old
        if (System.currentTimeMillis() > this.accessTokenExpires) try {
            getFreshAccessToken().get();
        } catch (final Exception e) {
            log.error("Could not request spotify access token", e);
        }
    }

    /**
     * @param userId identifier of the spotify user which this list belongs to
     * @param listId identifier of requested requested playlist
     * @return a Future on the requested playlist
     */
    public Future<Playlist> getPlaylist(final String userId, final String listId) {
        final PlaylistRequest request = this.spotifyApi.getPlaylist(userId, listId).build();
        refreshTokenIfNecessary();
        return request.getAsync();
    }


    /**
     * Returns the full tracklist of a playlist
     *
     * @param playlist playlist for which the full tracklist shall be retrieved
     * @return a list containing all tracks of the provided playlist
     */
    public List<PlaylistTrack> getFullTrackList(final Playlist playlist) {
        final List<PlaylistTrack> result = new ArrayList<>();
        Page<PlaylistTrack> page = null;

        do {
            final PlaylistTracksRequest.Builder builder = this.spotifyApi.getPlaylistTracks(playlist.getOwner().getId(), playlist.getId());

            //this should be true in every iteration except for the first one, where we also don't need to set any parameters
            if (page != null) {
                final String nextPageUrl = page.getNext();
                final Matcher m = PARAMETER_PATTERN.matcher(nextPageUrl);

                if (!m.find()) {
                    log.debug("Did not find parameter pattern in next page URL provided by Spotify");
                    break;
                }

                final String offset = m.group(1);
                final String limit = m.group(2);

                //We are trusting Spotify to get their shit together and provide us sane values for these
                builder.parameter("offset", offset);
                builder.parameter("limit", limit);
            }

            final PlaylistTracksRequest request = builder.build();
            try {
                refreshTokenIfNecessary();
                page = request.get();
                result.addAll(page.getItems());
            } catch (final Exception e) {
                log.error("Could not get next page in Spotify playlist " + playlist.getId(), e);
                break;
            }

        } while (page.getNext() != null);

        return result;
    }
}