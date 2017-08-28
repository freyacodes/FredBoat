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
import fredboat.db.entity.SearchResult;
import fredboat.feature.togglz.FeatureFlags;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class SearchUtil {

    private static final Logger log = LoggerFactory.getLogger(SearchUtil.class);

    private static final AudioPlayerManager PLAYER_MANAGER = initPlayerManager();
    private static final int DEFAULT_TIMEOUT = 3000;
    private static final long CACHE_MAX_AGE = TimeUnit.HOURS.toMillis(24); //24 hours

    public static final String PUNCTUATION_REGEX = "[.,/#!$%^&*;:{}=\\-_`~()\"\']";

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

    public static AudioPlaylist searchForTracks(SearchProvider provider, String query) {
        return searchForTracks(provider, query, DEFAULT_TIMEOUT);
    }

    public static AudioPlaylist searchForTracks(SearchProvider provider, String query, int timeout) {
        return new SearchResultHandler().searchSync(provider, query, timeout);
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

    static class SearchResultHandler implements AudioLoadResultHandler {

        Throwable throwable;
        AudioPlaylist result;
        final Object toBeNotified = new Object();

        AudioPlaylist searchSync(SearchProvider provider, String query, int timeout) {
            if (FeatureFlags.FORCE_SOUNDCLOUD_SEARCH.isActive()) {
                provider = SearchProvider.SOUNDCLOUD;
            }

            result = SearchResult.load(PLAYER_MANAGER, provider, query, CACHE_MAX_AGE);
            if (result != null) {
                log.debug("Loaded search result {} {} from cache", provider, query);
                return result;
            }

            try {
                synchronized (toBeNotified) {
                    PLAYER_MANAGER.loadItem(provider.getPrefix() + query, this);
                    toBeNotified.wait(timeout);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("Was interrupted while searching", e);
            }

            if(throwable != null) {
                throw new RuntimeException("Failed to search!", throwable);
            }

            if (result != null) {
                new SearchResult(PLAYER_MANAGER, provider, query, result).save();
            }
            return result;
        }

        @Override
        public void trackLoaded(AudioTrack audioTrack) {
            throwable = new UnsupportedOperationException("Can't load a single track when we are expecting a playlist!");
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
            throwable = e;
            synchronized (toBeNotified) {
                toBeNotified.notify();
            }
        }
    }
}
