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

package fredboat.util.rest

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist
import fredboat.config.property.AppConfig
import fredboat.db.api.SearchResultRepository
import fredboat.db.transfer.SearchResult
import fredboat.db.transfer.SearchResultId
import fredboat.definitions.SearchProvider
import fredboat.feature.metrics.Metrics
import fredboat.feature.togglz.FeatureFlags
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.ArrayList
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Component
class TrackSearcher(@param:Qualifier("searchAudioPlayerManager") private val audioPlayerManager: AudioPlayerManager,
                    private val youtubeAPI: YoutubeAPI, private val repository: SearchResultRepository, private val appConfig: AppConfig) {

    @Throws(TrackSearcher.SearchingException::class)
    suspend fun searchForTracks(query: String, providers: List<SearchProvider>): AudioPlaylist {
        return searchForTracks(query, DEFAULT_CACHE_MAX_AGE, DEFAULT_TIMEOUT, providers)
    }

    /**
     * @param query         The search term
     * @param timeoutMillis How long to wait for each lavaplayer search to answer
     * @param providers     Providers that shall be used for the search. They will be used in the order they are provided, the
     * result of the first successful one will be returned
     * @return The result of the search, or an empty list.
     * @throws SearchingException If none of the search providers could give us a result, and there was at least one SearchingException thrown by them
     */
    @Throws(TrackSearcher.SearchingException::class)
    suspend fun searchForTracks(query: String, cacheMaxAge: Long, timeoutMillis: Int, providers: List<SearchProvider>?): AudioPlaylist {
        Metrics.searchRequests.inc()

        val provs = ArrayList<SearchProvider>()
        if (providers == null || providers.isEmpty()) {
            log.warn("No search provider provided, defaulting to youtube -> soundcloud.")
            provs.add(SearchProvider.YOUTUBE)
            provs.add(SearchProvider.SOUNDCLOUD)
        } else {
            provs.addAll(providers)
        }

        var searchingException: SearchingException? = null

        for (provider in provs) {
            //1. cache
            val cacheResult = fromCache(provider, query, cacheMaxAge)
            if (cacheResult != null && !cacheResult.tracks.isEmpty()) {
                log.debug("Loaded search result {} {} from cache", provider, query)
                Metrics.searchHits.labels("cache").inc()
                return cacheResult
            }

            //2. lavaplayer todo break up this beautiful construction of ifs and exception handling in a better readable one?
            if (provider != SearchProvider.YOUTUBE || System.currentTimeMillis() > youtubeCooldownUntil) {
                try {
                    val lavaplayerResult = SearchResultHandler()
                            .searchSync(audioPlayerManager, provider, query, timeoutMillis)
                    if (!lavaplayerResult.tracks.isEmpty()) {
                        log.debug("Loaded search result {} {} from lavaplayer", provider, query)
                        // got a search result? cache and return it
                        repository.update(SearchResult(provider, query, lavaplayerResult)).subscribe()
                        Metrics.searchHits.labels("lavaplayer-" + provider.name.toLowerCase()).inc()
                        return lavaplayerResult
                    }
                } catch (e: Http503Exception) {
                    if (provider == SearchProvider.YOUTUBE) {
                        log.warn("Got a 503 from Youtube. Not hitting it with searches it for {} minutes", TimeUnit.MILLISECONDS.toMinutes(DEFAULT_YOUTUBE_COOLDOWN))
                        youtubeCooldownUntil = System.currentTimeMillis() + DEFAULT_YOUTUBE_COOLDOWN
                    }
                    searchingException = e
                } catch (e: SearchingException) {
                    searchingException = e
                }

            }

            //3. optional: youtube api
            if (provider == SearchProvider.YOUTUBE && (appConfig.isPatronDistribution || appConfig.isDevDistribution)) {
                try {
                    val youtubeApiResult = youtubeAPI.search(query, MAX_RESULTS, audioPlayerManager.source(YoutubeAudioSourceManager::class.java))
                    if (!youtubeApiResult.tracks.isEmpty()) {
                        log.debug("Loaded search result {} {} from Youtube API", provider, query)
                        // got a search result? cache and return it
                        repository.update(SearchResult(provider, query, youtubeApiResult)).subscribe()
                        Metrics.searchHits.labels("youtube-api").inc()
                        return youtubeApiResult
                    }
                } catch (e: SearchingException) {
                    searchingException = e
                }

            }
        }

        //did we run into searching exceptions that made us end up here?
        if (searchingException != null) {
            Metrics.searchHits.labels("exception").inc()
            throw searchingException
        }
        //no result with any of the search providers
        Metrics.searchHits.labels("empty").inc()
        return BasicAudioPlaylist("Search result for: $query", emptyList(), null, true)
    }

    /**
     * @param provider   the search provider that shall be used for this search
     * @param searchTerm the searchTerm to search for
     */
    private suspend fun fromCache(provider: SearchProvider, searchTerm: String, cacheMaxAge: Long): AudioPlaylist? {
        try {
            val id = SearchResultId(provider, searchTerm)
            val result = repository.fetch(id).awaitSingle()

            // If the cache entry is old evict it from DB and return null
            if (result.timestamp + cacheMaxAge < System.currentTimeMillis()) {
                repository.remove(result.id).subscribe()

                return null
            }

            return result.getSearchResult()

        } catch (e: Exception) {
            //could be a database issue, could be a serialization issue. better to catch them all here and "orderly" return
            log.warn("Could not retrieve cached search result from database.", e)
            return null
        }

    }

    open class SearchingException : Exception {

        constructor(message: String) : super(message)

        constructor(message: String, cause: Exception) : super(message, cause)

        companion object {
            private const val serialVersionUID = -1020150337258395420L
        }
    }

    //creative name...
    class Http503Exception(message: String, cause: Exception) : SearchingException(message, cause) {

        companion object {
            private const val serialVersionUID = -2698566544845714550L
        }
    }

    private class SearchResultHandler : AudioLoadResultHandler {

        internal var exception: Exception? = null
        internal var result: AudioPlaylist? = null

        /**
         * @return The result of the search (which may be empty but not null).
         */
        @Throws(TrackSearcher.SearchingException::class)
        internal fun searchSync(audioPlayerManager: AudioPlayerManager, provider: SearchProvider, query: String, timeoutMillis: Int): AudioPlaylist {
            var searchProvider = provider
            if (FeatureFlags.FORCE_SOUNDCLOUD_SEARCH.isActive) {
                searchProvider = SearchProvider.SOUNDCLOUD
            }

            log.debug("Searching {} for {}", searchProvider, query)
            try {
                audioPlayerManager.loadItem(searchProvider.prefix + query, this)
                        .get(timeoutMillis.toLong(), TimeUnit.MILLISECONDS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (e: ExecutionException) {
                exception = e
            } catch (e: TimeoutException) {
                throw SearchingException(String.format("Searching provider %s for %s timed out after %sms",
                        searchProvider.name, query, timeoutMillis))
            }

            if (exception != null) {
                if (exception is FriendlyException && exception!!.cause != null) {
                    val messageOfCause = exception!!.cause!!.message
                    if (messageOfCause!!.contains("java.io.IOException: Invalid status code for search response: 503")) {
                        throw Http503Exception("Lavaplayer search returned a 503", exception as FriendlyException)
                    }
                }

                val message = String.format("Failed to search provider %s for query %s with exception %s.",
                        searchProvider, query, exception!!.message)
                throw SearchingException(message, exception!!)
            }

            if (result == null) {
                throw SearchingException(String.format("Result from provider %s for query %s is unexpectedly null", searchProvider, query))
            }

            return result as AudioPlaylist
        }

        override fun trackLoaded(audioTrack: AudioTrack) {
            exception = UnsupportedOperationException("Can't load a single track when we are expecting a playlist!")
        }

        override fun playlistLoaded(audioPlaylist: AudioPlaylist) {
            result = audioPlaylist
        }

        override fun noMatches() {
            result = BasicAudioPlaylist("No matches", emptyList(), null, true)
        }

        override fun loadFailed(e: FriendlyException) {
            exception = e
        }
    }

    companion object {

        const val MAX_RESULTS = 5
        val DEFAULT_CACHE_MAX_AGE = TimeUnit.HOURS.toMillis(48)
        const val PUNCTUATION_REGEX = "[.,/#!$%^&*;:{}=\\-_`~()\"\']"
        private const val DEFAULT_TIMEOUT = 3000

        private val log = LoggerFactory.getLogger(TrackSearcher::class.java)

        //give youtube a break if we get flagged and keep getting 503s
        private val DEFAULT_YOUTUBE_COOLDOWN = TimeUnit.MINUTES.toMillis(10) // 10 minutes
        private var youtubeCooldownUntil: Long = 0
    }
}
