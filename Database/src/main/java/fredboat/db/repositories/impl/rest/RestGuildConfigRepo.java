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
import fredboat.db.entity.main.GuildConfig;
import fredboat.db.repositories.api.GuildConfigRepo;
import fredboat.util.rest.Http;
import io.prometheus.client.guava.cache.CacheMetricsCollector;

/**
 * Created by napster on 17.02.18.
 */
public class RestGuildConfigRepo extends CachedRestRepo<String, GuildConfig> implements GuildConfigRepo {

    public static final String PATH = "guildconfig/";

    public RestGuildConfigRepo(String apiBasePath, Http http, Gson gson, String auth) {
        super(apiBasePath + VERSION_PATH + PATH, GuildConfig.class, http, gson, auth);
    }

    @Override
    public RestGuildConfigRepo registerCacheStats(CacheMetricsCollector cacheMetrics, String name) {
        super.registerCacheStats(cacheMetrics, name);
        return this;
    }
}
