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
 */

package fredboat.audio.source

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.*
import fredboat.audio.queue.PlaylistInfo
import fredboat.definitions.SearchProvider
import fredboat.util.rest.SpotifyAPIWrapper
import fredboat.util.rest.TrackSearcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.reactor.mono
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.DataInput
import java.io.DataOutput
import java.io.IOException
import java.util.ArrayList
import java.util.Arrays
import java.util.concurrent.*
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created by napster on 08.03.17.
 *
 *
 * Loads playlists from Spotify playlist links.
 *
 * todo bulk load the songs from the search cache (remote db connections are slow when loading one by one)
 *
 * @author napster
 */
class SpotifySourceManager(private val trackSearcher: TrackSearcher, private val spotifyAPIWrapper: SpotifyAPIWrapper) : AudioSourceManager, PlaylistImporter {

     companion object {

        var CACHE_DURATION = TimeUnit.DAYS.toMillis(30)// 1 month;

        private val log = LoggerFactory.getLogger(SpotifySourceManager::class.java)

        //https://regex101.com/r/AEWyxi/3
        private val PLAYLIST_PATTERN = Pattern.compile("https?://.*\\.spotify\\.com(.*)/playlist/([^?/\\s]*)")
        private val ALBUM_PATTERN = Pattern.compile("https?://.*\\.spotify\\.com(.*)/album/([^?/\\s]*)")

        //Take care when deciding on upping the core pool size: The threads may hog database connections when loading an uncached playlist.
        // Upping the threads will also fire search requests more aggressively against Youtube which is probably better avoided.
        var loader = Executors.newScheduledThreadPool(1)

        private val searchProviders = Arrays.asList(SearchProvider.YOUTUBE, SearchProvider.SOUNDCLOUD)

        val LinkType = listOf("Album", "Playlist")
        }

    override fun getSourceName(): String {
        return "spotify_playlist_import"
    }

    override fun loadItem(manager: DefaultAudioPlayerManager, ar: AudioReference): AudioItem? {

        val data = parse(ar.identifier) ?: return null
        val spotifyListId = data[0]
        val listType = data[1]

        val plData: PlaylistInfo
        var listName: String?
        try {
            when (listType) {
                "Playlist" -> {
            plData = spotifyAPIWrapper.getPlaylistDataBlocking(spotifyListId)
                     listName = plData.name
                }
                "Album" -> {
                    plData = spotifyAPIWrapper.getAlbumDataBlocking(spotifyListId)
                    listName = plData.name
                }
                else -> throw IllegalStateException("Invalid link type: " + listType)
            }
        } catch (e: Exception) {
            log.warn("Could not retrieve playlist $spotifyListId", e)
            throw FriendlyException("Couldn't load playlist. Either Spotify is down or the playlist does not exist.", FriendlyException.Severity.COMMON, e)
        }

        if (listName == null || "" == listName) listName = "Spotify List"
        val tracksTotal = plData.totalTracks

        val trackList = ArrayList<AudioTrack>()
        val trackListSearchTerms: List<String>

        try {
            when (listType) {
                "Playlist" -> trackListSearchTerms = spotifyAPIWrapper.getPlaylistTracksSearchTermsBlocking(spotifyListId)
                "Album" -> trackListSearchTerms = spotifyAPIWrapper.getAlbumTracksSearchTermsBlocking(spotifyListId)
                else -> throw IllegalStateException("Invalid link type: " + listType)
            }
        } catch (e: Exception) {
            log.warn("Could not retrieve track(s) for list $spotifyListId", e)
            throw FriendlyException("Couldn't load spotify track(s). Either Spotify is down or the item does not exist.", FriendlyException.Severity.COMMON, e)
        }

        log.info("Retrieved playlist data for $listName from Spotify, loading up $tracksTotal tracks")

        //build a task list
        val taskList = ArrayList<CompletableFuture<AudioTrack>>()
        for (s in trackListSearchTerms) {
            //remove all punctuation
            val query = s.replace(TrackSearcher.PUNCTUATION_REGEX.toRegex(), "")

            taskList.add(GlobalScope.mono { searchSingleTrack(query) }.toFuture())
        }

        //build a tracklist from that task list
        for (futureTrack in taskList) {
            try {
                val audioItem = futureTrack.get()
                        ?: continue //skip the track if we couldn't find it
                trackList.add(audioItem)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            } catch (ignored: ExecutionException) {
                //this is fine, loop will go for the next item
            }

        }
        return BasicAudioPlaylist(listName, trackList, null, true)
    }

    /**
     * Searches all available searching sources for a single track.
     *
     *
     * Will go Youtube > SoundCloud > return null
     * This could probably be moved to SearchUtil
     *
     * @param query Term that shall be searched
     * @return An AudioTrack likely corresponding to the query term or null.
     */
    private suspend fun searchSingleTrack(query: String): AudioTrack? {
        try {
            val list = trackSearcher.searchForTracks(query, CACHE_DURATION, 60000, searchProviders)
            //didn't find anything
            return if (list == null || list.tracks.isEmpty()) {
                null
            } else list.tracks[0]

            //pick topmost result, and hope it's what the user wants to listen to
            //having users pick tracks like they can do for individual searches would be ridiculous for playlists with
            //dozens of tracks. youtube search is probably good enough for this
            //
            //testcase:   Rammstein playlists; high quality Rammstein vids are really rare on Youtube.
            //            https://open.spotify.com/user/11174036433/playlist/0ePRMvD3Dn3zG31A8y64xX
            //result:     lots of low quality (covers, pitched up/down, etc) tracks loaded.
            //conclusion: there's room for improvement to this whole method
        } catch (e: TrackSearcher.SearchingException) {
            //youtube & soundcloud not available
            return null
        }

    }

    override fun isTrackEncodable(track: AudioTrack): Boolean = false

    override fun encodeTrack(track: AudioTrack, output: DataOutput) {
        throw UnsupportedOperationException("This source manager is only for loading playlists")
    }

    override fun decodeTrack(trackInfo: AudioTrackInfo, input: DataInput): AudioTrack {
        throw UnsupportedOperationException("This source manager is only for loading playlists")
    }

    override fun shutdown() {

    }

    /**
     * @return null or a string array containing playlistId at [0] of the requested playlist
     */
    private fun parse(identifier: String): Array<String?>? {
        val result = arrayOfNulls<String>(2)
        val playlist = PLAYLIST_PATTERN.matcher(identifier)
        val album = ALBUM_PATTERN.matcher(identifier)

        when {
            album.find() -> {
                result[0] = album.group(2)
                result[1] = LinkType[0]
            }

            playlist.find() -> {
                result[0] = playlist.group(2)
                result[1] = LinkType[1]
        }

            else -> return null
        }

        log.debug("matched spotify link. listId: " + result[0] + " type: " + result[1])
        return result
    }

    override fun getPlaylistDataBlocking(identifier: String): PlaylistInfo? {

        val data = parse(identifier) ?: return null
        val spotifyListId = data[0]
        val listType = data[1]

        try {
            when (listType) {
                "Playlist" -> return spotifyAPIWrapper.getPlaylistDataBlocking(spotifyListId)
                "Album" -> return spotifyAPIWrapper.getAlbumDataBlocking(spotifyListId)
                else -> throw IllegalStateException("Invalid link type: " + listType)
            }
        } catch (e: Exception) {
            log.warn("Could not retrieve playlist $spotifyListId", e)
            throw FriendlyException("Couldn't load playlist. Either Spotify is down or the playlist does not exist.", FriendlyException.Severity.COMMON, e)
        }
    }
}