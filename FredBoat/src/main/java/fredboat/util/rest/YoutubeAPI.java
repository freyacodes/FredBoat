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

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import fredboat.Config;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class YoutubeAPI {

    private static final Logger log = LoggerFactory.getLogger(YoutubeAPI.class);

    private YoutubeAPI() {
    }

    private static YoutubeVideo getVideoFromID(String id) {
        JSONObject data = null;
        try {
            data = Unirest.get("https://www.googleapis.com/youtube/v3/videos?part=contentDetails,snippet&fields=items(id,snippet/title,contentDetails/duration)")
                    .queryString("id", id)
                    .queryString("key", Config.CONFIG.getRandomGoogleKey())
                    .asJson()
                    .getBody()
                    .getObject();

            YoutubeVideo vid = new YoutubeVideo();
            vid.id = data.getJSONArray("items").getJSONObject(0).getString("id");
            vid.name = data.getJSONArray("items").getJSONObject(0).getJSONObject("snippet").getString("title");
            vid.duration = data.getJSONArray("items").getJSONObject(0).getJSONObject("contentDetails").getString("duration");

            return vid;
        } catch (JSONException ex) {
            System.err.println(data);
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static YoutubeVideo getVideoFromID(String id, boolean verbose) {
        if(verbose){
            JSONObject data = null;
            String gkey = Config.CONFIG.getRandomGoogleKey();
            try {
                data = Unirest.get("https://www.googleapis.com/youtube/v3/videos?part=contentDetails,snippet")
                        .queryString("id", id)
                        .queryString("key", gkey)
                        .asJson()
                        .getBody()
                        .getObject();

                YoutubeVideo vid = new YoutubeVideo();
                vid.id = data.getJSONArray("items").getJSONObject(0).getString("id");
                vid.name = data.getJSONArray("items").getJSONObject(0).getJSONObject("snippet").getString("title");
                vid.duration = data.getJSONArray("items").getJSONObject(0).getJSONObject("contentDetails").getString("duration");
                vid.description = data.getJSONArray("items").getJSONObject(0).getJSONObject("snippet").getString("description");
                vid.channelId = data.getJSONArray("items").getJSONObject(0).getJSONObject("snippet").getString("channelId");
                vid.channelTitle = data.getJSONArray("items").getJSONObject(0).getJSONObject("snippet").getString("channelTitle");
                vid.isStream = !data.getJSONArray("items").getJSONObject(0).getJSONObject("snippet").getString("liveBroadcastContent").equals("none");

                return vid;
            } catch (JSONException ex) {
                log.error(data != null ? data.toString() : null);

                log.error("API key used ends with: " + gkey.substring(20));

                throw ex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } else {
            return getVideoFromID(id);
        }
    }

    /**
     * @param query         Search Youtube for this query
     * @param maxResults    Keep this as small as necessary, each of the videos needs to be looked up for more detailed info
     * @param sourceManager The source manager may be used by the tracks to look further information up
     * @return A playlist representing the search results; null if there was an exception
     */
    //docs: https://developers.google.com/youtube/v3/docs/search/list
    //theres a lot of room for tweaking the searches
    public static AudioPlaylist search(String query, int maxResults, YoutubeAudioSourceManager sourceManager)
            throws SearchUtil.SearchingException {
        JSONObject data;
        String gkey = Config.CONFIG.getRandomGoogleKey();
        try {
            data = Unirest.get("https://www.googleapis.com/youtube/v3/search?part=snippet")
                    .queryString("key", gkey)
                    .queryString("type", "video")
                    .queryString("maxResults", maxResults)
                    .queryString("q", query)
                    .asJson()
                    .getBody()
                    .getObject();
        } catch (UnirestException e) {
            throw new SearchUtil.SearchingException("Unirest exception when search with the Youtube API", e);
        }

        //The search contains all values we need, except for the duration :feelsbadman:
        //so we need to do another query for each video.
        List<String> ids = new ArrayList<>(maxResults);
        try {
            JSONArray items = data.getJSONArray("items");
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                ids.add(item.getJSONObject("id").getString("videoId"));
            }
        } catch (JSONException e) {
            String message = String.format("Youtube search with API key ending on %s for query %s returned unexpected JSON:\n%s",
                    gkey.substring(20), query, data.toString());
            throw new SearchUtil.SearchingException(message, e);
        }

        List<AudioTrack> tracks = new ArrayList<>();
        for (String id : ids) {
            try {
                YoutubeVideo vid = getVideoFromID(id, true);
                tracks.add(sourceManager.buildTrackObject(id, vid.name, vid.channelTitle, vid.isStream, vid.getDurationInMillis()));
            } catch (RuntimeException e) {
                throw new SearchUtil.SearchingException("Could not look up details for youtube video with id " + id, e);
            }
        }
        return new BasicAudioPlaylist("Search results for: " + query, tracks, null, true);
    }
}
