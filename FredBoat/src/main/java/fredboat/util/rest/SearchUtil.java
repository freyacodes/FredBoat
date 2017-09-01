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

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import fredboat.Config;
import fredboat.FredBoat;
import fredboat.db.DatabaseNotReadyException;
import fredboat.db.entity.SearchResult;
import fredboat.feature.togglz.FeatureFlags;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SearchUtil {

    public static final long DEFAULT_CACHE_MAX_AGE = TimeUnit.HOURS.toMillis(24); //24 hours
    public static final String PUNCTUATION_REGEX = "[.,/#!$%^&*;:{}=\\-_`~()\"\']";

    private static final Logger log = LoggerFactory.getLogger(SearchUtil.class);

    private static final AudioPlayerManager PLAYER_MANAGER = initPlayerManager();
    private static final int DEFAULT_TIMEOUT = 3000;

    private static AudioPlayerManager initPlayerManager() {
        DefaultAudioPlayerManager manager = new DefaultAudioPlayerManager();
        YoutubeAudioSourceManager youtubeAudioSourceManager = new YoutubeAudioSourceManager();
        youtubeAudioSourceManager.configureRequests(config -> RequestConfig.copy(config)
                .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                .build());
        manager.registerSourceManager(youtubeAudioSourceManager);
        manager.registerSourceManager(new SoundCloudAudioSourceManager());
        return manager;
    }

    public static AudioPlaylist searchForTracks(String query, List<SearchProvider> providers) throws SearchingException {
        return searchForTracks(query, DEFAULT_CACHE_MAX_AGE, DEFAULT_TIMEOUT, providers);
    }

    /**
     * @param query         The search term
     * @param cacheMaxAge   Age of acceptable results from cache. See {@link fredboat.db.entity.SearchResult#load} for details.
     * @param timeoutMillis How long to wait for each lavaplayer search to answer
     * @param providers     Providers that shall be used for the search. They will be used in the order they are provided, the
     *                      result of the first successful one will be returned
     * @return The result of the search, or an empty list.
     * @throws SearchingException If none of the search providers could give us a result, and there was at least one SearchingException thrown by them
     */
    public static AudioPlaylist searchForTracks(String query, long cacheMaxAge, int timeoutMillis, List<SearchProvider> providers)
            throws SearchingException {

        List<SearchProvider> provs = new ArrayList<>();
        if (providers == null || providers.size() == 0) {
            log.warn("No search provider provided, defaulting to youtube -> soundcloud.");
            provs.add(SearchProvider.YOUTUBE);
            provs.add(SearchProvider.SOUNDCLOUD);
        } else {
            provs.addAll(providers);
        }

        SearchingException exception = null;

        for (SearchProvider provider : provs) {
            //1. cache
            AudioPlaylist cacheResult = fromCache(provider, query, cacheMaxAge);
            if (cacheResult != null && !cacheResult.getTracks().isEmpty()) {
                log.debug("Loaded search result {} {} from cache", provider, query);
                return cacheResult;
            }

            //2. lavaplayer
            try {
                AudioPlaylist lavaplayerResult = new SearchResultHandler().searchSync(provider, query, timeoutMillis);
                if (!lavaplayerResult.getTracks().isEmpty()) {
                    log.debug("Loaded search result {} {} from lavaplayer", provider, query);
                    // got a search result? cache and return it
                    FredBoat.executor.submit(() -> new SearchResult(PLAYER_MANAGER, provider, query, lavaplayerResult).save());
                    return lavaplayerResult;
                }
            } catch (SearchingException e) {
                exception = e;
            }

            //3. optional: youtube api
            if (provider == SearchProvider.YOUTUBE &&
                    (Config.CONFIG.isPatronDistribution() || Config.CONFIG.isDevDistribution())) {
                try {
                    AudioPlaylist youtubeApiResult = YoutubeAPI.search(query, 5, PLAYER_MANAGER.source(YoutubeAudioSourceManager.class));
                    if (!youtubeApiResult.getTracks().isEmpty()) {
                        log.debug("Loaded search result {} {} from Youtube API", provider, query);
                        // got a search result? cache and return it
                        FredBoat.executor.submit(() -> new SearchResult(PLAYER_MANAGER, provider, query, youtubeApiResult).save());
                        return youtubeApiResult;
                    }
                } catch (SearchingException e) {
                    exception = e;
                }
            }
        }

        //did we run into searching exceptions that made us end up here?
        if (exception != null) {
            throw exception;
        }
        //no result with any of the search providers
        return new BasicAudioPlaylist("Search result for: " + query, Collections.emptyList(), null, true);
    }

    private static AudioPlaylist fromCache(SearchProvider provider, String query, long cacheMaxAge) {
        try {
            return SearchResult.load(PLAYER_MANAGER, provider, query, cacheMaxAge);
        } catch (DatabaseNotReadyException ignored) {
            log.warn("Could not retrieve cached search result from database.");
            return null;
        }
    }

    public enum SearchProvider {
        YOUTUBE("ytsearch:"),
        SOUNDCLOUD("scsearch:");

        private String prefix;

        SearchProvider(String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return prefix;
        }
    }

    public static class SearchingException extends Exception {
        private static final long serialVersionUID = -1020150337258395420L;

        public SearchingException(String message) {
            super(message);
        }

        public SearchingException(String message, Exception cause) {
            super(message, cause);
        }
    }

    static class SearchResultHandler implements AudioLoadResultHandler {

        Exception exception;
        AudioPlaylist result;
        final Object toBeNotified = new Object();

        /**
         * @return The result of the search (which may be empty but not null).
         */
        AudioPlaylist searchSync(SearchProvider provider, String query, int timeoutMillis) throws SearchingException {
            if (FeatureFlags.FORCE_SOUNDCLOUD_SEARCH.isActive()) {
                provider = SearchProvider.SOUNDCLOUD;
            }

            try {
                synchronized (toBeNotified) {
                    PLAYER_MANAGER.loadItem(provider.getPrefix() + query, this);
                    toBeNotified.wait(timeoutMillis);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (exception != null) {
                String message = String.format("Failed to search provider %s for query %s with exception %s.",
                        provider, query, exception.getMessage());
                throw new SearchingException(message, exception);
            }

            if (result == null) {
                throw new SearchingException(String.format("Result from provider %s for query %s is unexpectedly null", provider, query));
            }

            return result;
        }

        @Override
        public void trackLoaded(AudioTrack audioTrack) {
            exception = new UnsupportedOperationException("Can't load a single track when we are expecting a playlist!");
            synchronized (toBeNotified) {
                toBeNotified.notify();
            }
        }

        @Override
        public void playlistLoaded(AudioPlaylist audioPlaylist) {
            result = audioPlaylist;
            synchronized (toBeNotified) {
                toBeNotified.notify();
            }

        }

        @Override
        public void noMatches() {
            synchronized (toBeNotified) {
                toBeNotified.notify();
            }
        }

        @Override
        public void loadFailed(FriendlyException e) {
            exception = e;
            synchronized (toBeNotified) {
                toBeNotified.notify();
            }
        }
    }
}
