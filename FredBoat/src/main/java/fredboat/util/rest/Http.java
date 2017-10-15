/*
 *
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

package fredboat.util.rest;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.collections4.MapUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by napster on 03.10.17.
 * <p>
 * A Unirest-like wrapper with focus on convenience methods and classes for the OKHttpClient lib
 */
public class Http {

    private static final Logger log = LoggerFactory.getLogger(Http.class);

    public final static OkHttpClient defaultHttpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();


    @Nonnull
    @CheckReturnValue
    //if content type is left null we will assume it is text/plain UTF-8
    public static SimpleRequest post(@Nonnull String url, @Nonnull String body, @Nullable String contentType) {
        MediaType mediaType = contentType != null ? MediaType.parse(contentType) : MediaType.parse("text/plain");
        return new SimpleRequest(new Request.Builder()
                .post(RequestBody.create(mediaType, body))
                .url(url));
    }

    @Nonnull
    @CheckReturnValue
    //post a simple form body made of string string key values
    public static SimpleRequest post(@Nonnull String url, @Nonnull Params params) {
        FormBody.Builder body = new FormBody.Builder();
        for (Map.Entry<String, String> param : params.params.entrySet()) {
            body.add(param.getKey(), param.getValue());
        }
        return new SimpleRequest(new Request.Builder()
                .post(body.build())
                .url(url));
    }

    @Nonnull
    @CheckReturnValue
    public static SimpleRequest get(@Nonnull String url) {
        return new SimpleRequest(new Request.Builder()
                .get()
                .url(url));
    }


    @Nonnull
    @CheckReturnValue
    public static SimpleRequest get(@Nonnull String url, @Nonnull Params params) {
        return new SimpleRequest(new Request.Builder()
                .get()
                .url(paramUrl(url, params.params).build()));
    }

    @Nonnull
    @CheckReturnValue
    private static HttpUrl.Builder paramUrl(@Nonnull String url, @Nonnull Map<String, String> params) {
        //noinspection ConstantConditions
        HttpUrl.Builder httpUrlBuilder = HttpUrl.parse(url).newBuilder();
        for (Map.Entry<String, String> param : params.entrySet()) {
            httpUrlBuilder.addQueryParameter(param.getKey(), param.getValue());
        }
        return httpUrlBuilder;
    }

    /**
     * A simplified request.
     */
    public static class SimpleRequest {
        private Request.Builder requestBuilder;
        private OkHttpClient httpClient = defaultHttpClient;

        public SimpleRequest(Request.Builder requestBuilder) {
            this.requestBuilder = requestBuilder;
        }

        @Nonnull
        @CheckReturnValue
        public SimpleRequest url(@Nonnull String url, @Nonnull Params params) {
            requestBuilder.url(paramUrl(url, params.params).build());
            return this;
        }

        //set a custom client to execute this request with
        @Nonnull
        @CheckReturnValue
        public SimpleRequest client(@Nonnull OkHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        //add a header
        @Nonnull
        @CheckReturnValue
        public SimpleRequest header(@Nonnull String name, @Nonnull String value) {
            requestBuilder.header(name, value);
            return this;
        }

        //set an authorization header
        @Nonnull
        @CheckReturnValue
        public SimpleRequest auth(@Nonnull String value) {
            requestBuilder.header("Authorization", value);
            return this;
        }

        //remember to close the response
        @Nonnull
        @CheckReturnValue
        public Response execute() throws IOException {
            Request req = requestBuilder.build();
            log.debug("{} {}", req.method(), req.url().toString());
            return httpClient.newCall(req).execute();
        }

        //give me the content, don't care about error handling
        @Nonnull
        @CheckReturnValue
        public String asString() throws IOException {
            try (Response response = this.execute()) {
                //noinspection ConstantConditions
                return response.body().string();
            }
        }

        //give me the content, I don't care about error handling
        // catching JSONExceptions when parsing the returned object is a good idea
        @Nonnull
        @CheckReturnValue
        public JSONObject asJson() throws IOException {
            return new JSONObject(asString());
        }
    }

    /**
     * Fancy wrapper for a string to string map with a factory method
     */
    public static class Params {
        private Map<String, String> params = new HashMap<>();

        //pass pairs of strings and you'll be fine
        @Nonnull
        @CheckReturnValue
        public static Params of(@Nonnull String... pairs) {
            if (pairs.length % 2 == 1) {
                log.warn("Passed an uneven number of args to the Params wrapper, this is a likely bug.");
            }
            Params result = new Params();
            MapUtils.putAll(result.params, pairs);
            return result;
        }
    }

    public static boolean isImage(Response response) {
        String type = response.header("Content-Type");
        return type != null
                && (type.equals("image/jpeg")
                || type.equals("image/png")
                || type.equals("image/gif")
                || type.equals("image/webp"));
    }
}
