/*
 *
 * MIT License
 *
 * Copyright (c) 2017-2018 Frederik Ar. Mikkelsen
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

package fredboat.db.repositories.impl.rest;

import com.google.gson.Gson;
import fredboat.db.entity.cache.SearchResult;
import fredboat.db.repositories.api.SearchResultRepo;
import fredboat.util.rest.Http;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * Created by napster on 17.02.18.
 */
public class RestSearchResultRepo extends RestRepo<SearchResult.SearchResultId, SearchResult> implements SearchResultRepo {

    public static final String PATH = "/searchresult";

    public RestSearchResultRepo(String apiBasePath, Http http, Gson gson) {
        super(apiBasePath + PATH, SearchResult.class, http, gson);
    }

    @Nullable
    @Override
    public SearchResult getMaxAged(SearchResult.SearchResultId id, long maxAgeMillis) {
        try {
            String url = path + "/getmaxaged";
            Http.SimpleRequest getMaxAged = http.post(url, gson.toJson(id), "application/json")
                    .url(url, Http.Params.of("millis", Long.toString(maxAgeMillis)));
            return gson.fromJson(getMaxAged.asString(), SearchResult.class);
        } catch (IOException e) {  //todo decide on error handling strategy
            log.error("Could not get search result", e);
            return null;
        }
    }
}
