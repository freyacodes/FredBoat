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

package fredboat.command.fun.img;

import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IFunCommand;
import fredboat.main.BotController;
import fredboat.main.Launcher;
import fredboat.messaging.internal.Context;
import fredboat.util.rest.CacheUtil;
import fredboat.util.rest.Http;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RandomImageCommand extends Command implements IFunCommand {


    private static final Logger log = LoggerFactory.getLogger(RandomImageCommand.class);
    private static final ScheduledExecutorService imgurRefresher = Executors.newSingleThreadScheduledExecutor(
            runnable -> new Thread(runnable, "imgur-refresher"));

    //https://regex101.com/r/0TDxsu/2
    private static final Pattern IMGUR_ALBUM = Pattern.compile("^https?://imgur\\.com/a/([a-zA-Z0-9]+)$");

    private volatile String etag = "";

    //contains the images that this class randomly serves, default entry is a "my body is not ready" gif
    private volatile String[] urls = {"http://i.imgur.com/NqyOqnj.gif"};

    public RandomImageCommand(@Nonnull String imgurAlbumUrl, String name, String... aliases) {
        super(name, aliases);
        //update the album every hour
        imgurRefresher.scheduleAtFixedRate(() -> {
            try {
                populateItems(imgurAlbumUrl);
            } catch (Exception e) {
                log.error("Populating imgur album {} failed", imgurAlbumUrl, e);
            }
        }, 0, 1, TimeUnit.HOURS);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        context.replyImage(getRandomImageUrl());
    }

    public File getRandomFile() {
        //Get a random image as a cached file
        String randomUrl;
        synchronized (this) {
            randomUrl = getRandomImageUrl();
        }

        return CacheUtil.getImageFromURL(randomUrl);
    }

    public String getRandomImageUrl() {
        return (String) Array.get(urls, ThreadLocalRandom.current().nextInt(urls.length));
    }

    /**
     * Updates the imgur backed images managed by this object
     */
    private void populateItems(String imgurAlbumUrl) {

        Matcher m = IMGUR_ALBUM.matcher(imgurAlbumUrl);

        if (!m.find()) {
            log.error("Not a valid imgur album url {}", imgurAlbumUrl);
            return;
        }

        String albumId = m.group(1);
        Http.SimpleRequest request = BotController.HTTP.get("https://api.imgur.com/3/album/" + albumId)
                .auth("Client-ID " + Launcher.getBotController().getCredentials().getImgurClientId())
                .header("If-None-Match", etag);

        try (Response response = request.execute()) {
            //etag implementation: nothing has changed
            //https://api.imgur.com/performancetips
            //NOTE: imgur's implementation of this is wonky. occasionally they will return a new Etag without a
            // data change, and on the next fetch they will return the old Etag again.
            if (response.code() == 304) {
                //nothing to do here
                log.info("Refreshed imgur album {}, no update.", imgurAlbumUrl);
            } else if (response.isSuccessful()) {
                //noinspection ConstantConditions
                JSONArray images = new JSONObject(response.body().string()).getJSONObject("data").getJSONArray("images");
                List<String> imageUrls = new ArrayList<>();
                images.forEach(o -> imageUrls.add(((JSONObject) o).getString("link")));

                synchronized (this) {
                    urls = imageUrls.toArray(urls);
                    etag = response.header("ETag");
                }
                log.info("Refreshed imgur album {}, new data found.", imgurAlbumUrl);
            } else {
                //some other status
                //noinspection ConstantConditions
                log.warn("Unexpected http status for imgur album request {}, response: {}\n{}",
                        imgurAlbumUrl, response.toString(), response.body().string());
            }

        } catch (IOException e) {
            log.error("Imgur down? Could not fetch imgur album {}", imgurAlbumUrl, e);
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1}\n#Post a random image.";
    }
}
