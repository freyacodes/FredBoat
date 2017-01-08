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

package fredboat.util;

import com.mashape.unirest.http.Unirest;
import fredboat.FredBoat;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;

public class YoutubeAPI {

    private YoutubeAPI() {
    }

    public static ArrayList<YoutubeVideo> searchForVideos(String query) {
        JSONObject data = null;
        try {
            data = Unirest.get("https://www.googleapis.com/youtube/v3/search?part=id&type=video&maxResults=5&regionCode=US&fields=items(id/videoId)")
                    .queryString("q", URLEncoder.encode(query, "UTF-8"))
                    .queryString("key", FredBoat.getRandomGoogleKey())
                    .asJson()
                    .getBody()
                    .getObject();

            ArrayList<YoutubeVideo> vids = new ArrayList<>();

            data.getJSONArray("items").forEach((Object t) -> {
                JSONObject item = (JSONObject) t;
                vids.add(getVideoFromID(item.getJSONObject("id").getString("videoId")));
            });

            return vids;
        } catch (JSONException ex) {
            System.err.println(data);
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static YoutubeVideo getVideoFromID(String id) {
        JSONObject data = null;
        try {
            data = Unirest.get("https://www.googleapis.com/youtube/v3/videos?part=contentDetails,snippet&fields=items(id,snippet/title,contentDetails/duration)")
                    .queryString("id", id)
                    .queryString("key", FredBoat.getRandomGoogleKey())
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
            try {
                data = Unirest.get("https://www.googleapis.com/youtube/v3/videos?part=contentDetails,snippet")
                        .queryString("id", id)
                        .queryString("key", FredBoat.getRandomGoogleKey())
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

                return vid;
            } catch (JSONException ex) {
                System.err.println(data);
                throw ex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } else {
            return getVideoFromID(id);
        }
    }

}
