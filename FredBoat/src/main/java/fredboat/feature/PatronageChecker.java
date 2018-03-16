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

package fredboat.feature;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import fredboat.config.property.AppConfig;
import fredboat.main.BotController;
import fredboat.util.rest.CacheUtil;
import io.prometheus.client.guava.cache.CacheMetricsCollector;
import net.dv8tion.jda.core.entities.Guild;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class PatronageChecker {

    private static final Logger log = LoggerFactory.getLogger(PatronageChecker.class);

    private final LoadingCache<String, Status> cache = CacheBuilder.newBuilder()
            .recordStats()
            .expireAfterWrite(120, TimeUnit.MINUTES)
            .build(new Loader());

    // Meant to clear denial status fast so patrons can fix their patronage
    private final ScheduledExecutorService denialCleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        thread.setName("patreon-denial-cleaner");
        return thread;
    });
    private final AppConfig appConfig;

    // Pay attention to how we also clear the status early if we get an exception
    public PatronageChecker(CacheMetricsCollector cacheMetrics, AppConfig appConfig) {
        this.appConfig = appConfig;
        denialCleaner.scheduleAtFixedRate(
                () -> cache.asMap().replaceAll(
                        (__, status) -> status.isValid() || status.isCausedByError() ? status : null
                )
                , 0, 1, TimeUnit.MINUTES);

        log.info("Began patronage checker");
        cacheMetrics.addCache("patronageChecker", cache);
    }

    public Status getStatus(Guild guild) {
        return CacheUtil.getUncheckedUnwrapped(cache, guild.getId());
    }

    public class Status {

        private final boolean valid;
        private final String reason;
        private final boolean causedByError;

        private Status(JSONObject json) {
            valid = json.getBoolean("valid");
            reason = json.getString("reason");
            causedByError = false;
        }

        private Status() {
            valid = true;
            reason = null;
            causedByError = true;
        }

        public boolean isValid() {
            return valid;
        }

        public String getReason() {
            return reason;
        }

        private boolean isCausedByError() {
            return causedByError;
        }
    }

    private class Loader extends CacheLoader<String, Status> {

        @SuppressWarnings("NullableProblems")
        @Override
        public Status load(String key) {
            //TODO prevent selfhosters from running this?
            try {
                return new Status(
                        BotController.HTTP.get(appConfig.isPatronDistribution()
                                ? "https://patronapi.fredboat.com/api/drm/" + key
                                : "http://localhost:4500/api/drm/" + key)
                                .asJson()
                );
            } catch (Exception e) {
                log.error("Caught exception while verifying patron status", e);
                return new Status(); // Valid status, expires early
            }
        }
    }

    @Override
    protected void finalize() {
        denialCleaner.shutdown();
    }

}
