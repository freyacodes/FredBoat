package fredboat.util;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.wrapper.spotify.Api;
import com.wrapper.spotify.methods.PlaylistRequest;
import com.wrapper.spotify.models.ClientCredentials;
import com.wrapper.spotify.models.Playlist;
import fredboat.Config;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

/**
 * Created by napster on 08.03.17.
 *
 * @author napster
 */
public class SpotifyAPIWrapper {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(SpotifyAPIWrapper.class);

    private static SpotifyAPIWrapper SPOTIFYAPIWRAPPER;

    public static SpotifyAPIWrapper getApi() {
        if (SPOTIFYAPIWRAPPER == null) {
            SPOTIFYAPIWRAPPER = new SpotifyAPIWrapper(Config.CONFIG.getSpotifyId(), Config.CONFIG.getSpotifySecret());
        }

        return SPOTIFYAPIWRAPPER;
    }


    private Api spotifyApi;
    private long accessTokenExpires = 0;

    private SpotifyAPIWrapper(String clientId, String clientSecret) {
        spotifyApi = Api.builder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();

        getFreshAccessToken();
    }

    private SettableFuture<ClientCredentials> getFreshAccessToken() {

        final SettableFuture<ClientCredentials> responseFuture = spotifyApi.clientCredentialsGrant().build().getAsync();

        Futures.addCallback(responseFuture, new FutureCallback<ClientCredentials>() {
            @Override
            public void onSuccess(ClientCredentials cc) {
                spotifyApi.setAccessToken(cc.getAccessToken());
                accessTokenExpires = System.currentTimeMillis() + (cc.getExpiresIn() * 1000);
                log.info("Retrieved spotify access token. Expires in " + cc.getExpiresIn() + " seconds");
            }

            @Override
            public void onFailure(Throwable throwable) {
                log.error("Could not request spotify access token", throwable);
            }
        });

        return responseFuture;
    }


    public Future<Playlist> getPlaylist(String userId, String listId) {

        //refresh the token if it's too old
        if (System.currentTimeMillis() > accessTokenExpires) try {
            getFreshAccessToken().get();
        } catch (Exception e) {
            log.error("Could not request spotify access token", e);
        }

        final PlaylistRequest request = spotifyApi.getPlaylist(userId, listId).build();

        return request.getAsync();
    }
}