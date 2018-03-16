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

package fredboat.util.rest;

import fredboat.audio.queue.PlaylistInfo;
import fredboat.config.property.Credentials;
import fredboat.main.BotController;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by napster on 08.03.17.
 *
 * @author napster
 *
 * When expanding this class, make sure to call refreshTokenIfNecessary() before every request
 */
@Component
public class SpotifyAPIWrapper {
    //https://regex101.com/r/FkknVc/1
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("offset=([0-9]*)&limit=([0-9]*)$");

    private static final String URL_SPOTIFY_API = "https://api.spotify.com";
    private static final String URL_SPOTIFY_AUTHENTICATION_HOST = "https://accounts.spotify.com";

    private static final Logger log = LoggerFactory.getLogger(SpotifyAPIWrapper.class);
    private final Credentials credentials;

    private volatile long accessTokenExpires = 0;
    private volatile String accessToken = "";

    /**
     * Do not call this.
     * Get an instance of this class by using SpotifyAPIWrapper.getApi()
     */
    public SpotifyAPIWrapper(Credentials credentials) {
        this.credentials = credentials;
        refreshTokenIfNecessary();
    }

    /**
     * This is related to the client credentials flow.
     * https://developer.spotify.com/web-api/authorization-guide/#client-credentials-flow
     */
    private void refreshAccessToken() {
        if (credentials.getSpotifyId().isEmpty() || credentials.getSpotifySecret().isEmpty()) {
            return; //no spotify credentials configured, dont throw unnecessary errors
        }
        try {
            JSONObject jsonClientCredentials = BotController.HTTP.post(URL_SPOTIFY_AUTHENTICATION_HOST + "/api/token",
                    Http.Params.of(
                            "grant_type", "client_credentials"
                    ))
                    .auth(okhttp3.Credentials.basic(credentials.getSpotifyId(), credentials.getSpotifySecret()))
                    .asJson();

            accessToken = jsonClientCredentials.getString("access_token");
            accessTokenExpires = System.currentTimeMillis() + (jsonClientCredentials.getInt("expires_in") * 1000);
            log.debug("Retrieved spotify access token " + accessToken + " expiring in " + jsonClientCredentials.getInt("expires_in") + " seconds");
        } catch (final Exception e) {
            log.error("Could not retrieve spotify access token: " + e.getMessage(), e);
        }
    }

    /**
     * Call this before doing any requests
     */
    private void refreshTokenIfNecessary() {
        //refresh the token if it's too old
        if (System.currentTimeMillis() > this.accessTokenExpires) try {
            refreshAccessToken();
        } catch (final Exception e) {
            log.error("Could not request spotify access token", e);
        }
    }

    /**
     * Returns some data on a spotify playlist, currently it's name and tracks total.
     *
     * @param userId     Spotify user id of the owner of the requested playlist
     * @param playlistId Spotify playlist identifier
     * @return an array containing information about the requested spotify playlist
     */
    public PlaylistInfo getPlaylistDataBlocking(String userId, String playlistId) throws IOException, JSONException {
        refreshTokenIfNecessary();

        JSONObject jsonPlaylist = BotController.HTTP.get(URL_SPOTIFY_API + "/v1/users/" + userId + "/playlists/" + playlistId)
                .auth("Bearer " + accessToken)
                .asJson();

        // https://developer.spotify.com/web-api/object-model/#playlist-object-full
        String name = jsonPlaylist.getString("name");
        int tracks = jsonPlaylist.getJSONObject("tracks").getInt("total");

        return new PlaylistInfo(tracks, name, PlaylistInfo.Source.SPOTIFY);
    }

    /**
     * @param userId     Spotify user id of the owner of the requested playlist
     * @param playlistId Spotify playlist identifier
     * @return a string for each track on the requested playlist, containing track and artist names
     */
    public List<String> getPlaylistTracksSearchTermsBlocking(String userId, String playlistId) throws IOException, JSONException {
        refreshTokenIfNecessary();

        //strings on this list will contain name of the track + names of the artists
        List<String> list = new ArrayList<>();

        JSONObject jsonPage = null;
        //get page, then collect its tracks
        do {
            String offset = "0";
            String limit = "100";

            //this determines offset and limit on the 2nd+ pass of the do loop
            if (jsonPage != null) {
                String nextPageUrl;
                if (!jsonPage.has("next") || jsonPage.get("next") == JSONObject.NULL) break;
                nextPageUrl = jsonPage.getString("next");

                final Matcher m = PARAMETER_PATTERN.matcher(nextPageUrl);

                if (!m.find()) {
                    log.debug("Did not find parameter pattern in next page URL provided by Spotify");
                    break;
                }
                //We are trusting Spotify to get their shit together and provide us sane values for these
                offset = m.group(1);
                limit = m.group(2);
            }

            //request a page of tracks
            jsonPage = BotController.HTTP.get(URL_SPOTIFY_API + "/v1/users/" + userId + "/playlists/" + playlistId + "/tracks",
                    Http.Params.of(
                            "offset", offset,
                            "limit", limit
                    ))
                    .auth("Bearer " + accessToken)
                    .asJson();

            //add tracks to our result list
            // https://developer.spotify.com/web-api/object-model/#paging-object
            JSONArray jsonTracks = jsonPage.getJSONArray("items");

            jsonTracks.forEach((jsonPlaylistTrack) -> {
                try {
                    JSONObject track = ((JSONObject) jsonPlaylistTrack).getJSONObject("track");
                    final StringBuilder trackNameAndArtists = new StringBuilder();
                    trackNameAndArtists.append(track.getString("name"));

                    track.getJSONArray("artists").forEach((jsonArtist) -> trackNameAndArtists.append(" ")
                            .append(((JSONObject) jsonArtist).getString("name")));

                    list.add(trackNameAndArtists.toString());
                } catch (Exception e) {
                    log.warn("Could not create track from json, skipping", e);
                }
            });

        } while (jsonPage.has("next") && jsonPage.get("next") != null);

        return list;
    }
}
