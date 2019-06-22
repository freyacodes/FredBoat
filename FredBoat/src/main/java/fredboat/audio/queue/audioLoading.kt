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

package fredboat.audio.queue

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import fredboat.audio.player.GuildPlayer
import fredboat.audio.player.trackCount
import fredboat.audio.queue.limiter.errored
import fredboat.audio.queue.limiter.isPlaylistDisabledError
import fredboat.audio.queue.limiter.playlistDisabledError
import fredboat.audio.queue.limiter.successful
import fredboat.audio.queue.handlers.IQueueHandler
import fredboat.audio.source.PlaylistImportSourceManager
import fredboat.audio.source.PlaylistImporter
import fredboat.audio.source.SpotifyPlaylistSourceManager
import fredboat.feature.metrics.Metrics
import fredboat.util.TextUtils
import fredboat.util.extension.escapeAndDefuse
import fredboat.util.localMessageBuilder
import fredboat.util.ratelimit.Ratelimiter
import fredboat.util.rest.YoutubeAPI
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.reactor.mono
import org.apache.commons.lang3.tuple.ImmutablePair
import org.apache.commons.lang3.tuple.Pair
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.regex.Pattern

class AudioLoader(private val ratelimiter: Ratelimiter, internal val queueHandler: IQueueHandler,
                  private val playerManager: AudioPlayerManager, internal val player: GuildPlayer,
                  internal val youtubeAPI: YoutubeAPI) {
    private val identifierQueue = ConcurrentLinkedQueue<IdentifierContext>()
    @Volatile
    private var isLoading = false

    companion object {
        private val log = LoggerFactory.getLogger(AudioLoader::class.java)

        //Matches a timestamp and the description
        internal val SPLIT_DESCRIPTION_PATTERN = Pattern.compile("(.*?)[( \\[]*((?:\\d?\\d:)?\\d?\\d:\\d\\d)[) \\]]*(.*)")
        private const val QUEUE_TRACK_LIMIT = 10000
    }

    fun loadAsync(ic: IdentifierContext) {

        if (ratelimitIfSlowLoadingPlaylistAndAnnounce(ic)) {
            identifierQueue.add(ic)
            if (!isLoading) {
                loadNextAsync()
            }
        }
    }

    internal fun loadNextAsync() {
        val context = identifierQueue.poll()
        try {
            if (context != null) {
                isLoading = true

                if (player.trackCount >= QUEUE_TRACK_LIMIT) {
                    context.replyWithName(context.i18nFormat("loadQueueTrackLimit", QUEUE_TRACK_LIMIT))
                    isLoading = false
                    return
                }

                playerManager.loadItem(context.identifier, ResultHandler(this, context))
            } else {
                isLoading = false
            }
        } catch (th: Throwable) {
            context?.apply { handleThrowable(this, th) }
            if (context == null) log.error("Error while loading track", th)
            isLoading = false
        }

    }

    /**
     * If the requested item is a slow loading playlist that we know of, check for rate limits and announce to the user
     * that it might take a while to gather it.
     *
     * @return false if the user is not allowed to load the playlist, true if he is
     */
    private fun ratelimitIfSlowLoadingPlaylistAndAnnounce(ic: IdentifierContext): Boolean {
        val playlistInfo = getSlowLoadingPlaylistData(ic.identifier)

        if (playlistInfo == null)
        //not a slow loading playlist
            return true
        else {
            if (ratelimiter.isRatelimited(ic, playlistInfo, playlistInfo.totalTracks)) {
                return false
            }

            //inform user we are possibly about to do nasty time consuming work
            if (playlistInfo.totalTracks > 50) {
                ic.replyWithName(ic.i18nFormat("loadAnnouncePlaylist",
                        playlistInfo.name, playlistInfo.totalTracks))
            }
            return true
        }
    }

    /**
     * this function needs to be updated if we add more manual playlist loaders
     * currently it only covers the Wastebin and Spotify playlists
     *
     * @param identifier the very same identifier that the playlist loaders will be presented with if we asked them to
     * load a playlist
     * @return null if it's not a playlist that we manually parse, some data about it if it is
     */
    private fun getSlowLoadingPlaylistData(identifier: String): PlaylistInfo? {

        var playlistInfo: PlaylistInfo? = null
        var pi: PlaylistImporter? = playerManager.source(SpotifyPlaylistSourceManager::class.java)
        if (pi != null) {
            playlistInfo = pi.getPlaylistDataBlocking(identifier)
        }

        if (playlistInfo == null) {
            pi = playerManager.source(PlaylistImportSourceManager::class.java)
            if (pi != null) {
                playlistInfo = pi.getPlaylistDataBlocking(identifier)
            }
        }

        //can be null
        return playlistInfo
    }

    internal fun handleThrowable(ic: IdentifierContext, th: Throwable) {
        try {
            if (th is FriendlyException) {
                if (th.severity == FriendlyException.Severity.COMMON) {
                    ic.reply(ic.i18nFormat("loadErrorCommon", ic.identifier, th.message!!))
                } else {
                    ic.reply(ic.i18nFormat("loadErrorSusp", ic.identifier))
                    val exposed = if (th.cause == null) th else th.cause
                    TextUtils.handleException("Failed to load a track", exposed, ic)
                }
            } else {
                ic.reply(ic.i18n("loadErrorSusp"))
                TextUtils.handleException("Failed to load a track", th, ic)
            }
        } catch (e: Exception) {
            log.error("Error when trying to handle another error", th)
            log.error("DEBUG", e)
        }
    }

}

private class ResultHandler(val loader: AudioLoader, val context: IdentifierContext) : AudioLoadResultHandler {

    override fun loadFailed(fe: FriendlyException) {
        Metrics.trackLoadsFailed.inc()
        loader.handleThrowable(context, fe)

        loader.loadNextAsync()
    }

    override fun trackLoaded(at: AudioTrack) {
        Metrics.tracksLoaded.inc()
        try {
            if (context.isSplit) {
                loadSplit(at, context)
            } else {
                at.position = context.position
                val atc = AudioTrackContext(at, context.member, context.isPriority)
                GlobalScope.mono { loader.player.queueLimited(atc) }.subscribe {
                    if (it.canQueue) {
                        context.reply(if (!loader.player.isPlaying)
                            context.i18nFormat("loadSingleTrackAndPlay", TextUtils.escapeAndDefuse(at.info.title))
                        else
                            context.i18nFormat(if (context.isPriority) "loadSingleTrackFirst" else "loadSingleTrack",
                                    TextUtils.escapeAndDefuse(at.info.title))
                        )
                        loader.player.play()
                    } else {
                        context.replyWithMention(it.errorMessage)
                    }
                }
            }
        } catch (th: Throwable) {
            loader.handleThrowable(context, th)
        }

        loader.loadNextAsync()
    }

    override fun playlistLoaded(ap: AudioPlaylist) {
        Metrics.tracksLoaded.inc((if (ap.tracks == null) 0 else ap.tracks.size).toDouble())
        try {
            if (context.isSplit) {
                context.reply(context.i18n("loadPlaySplitListFail"))
                loader.loadNextAsync()
                return
            }

            val toAdd = ArrayList<AudioPlaylistContext>()
            for (at in ap.tracks) {
                toAdd.add(AudioPlaylistContext(at, context.member, context.isPriority))
            }

            GlobalScope.mono { loader.player.queueLimited(toAdd) }.subscribe {
                if (it.isPlaylistDisabledError) {
                    context.replyWithMention(context.i18n(it.playlistDisabledError))
                    return@subscribe
                }

                val mb = localMessageBuilder().append(context.i18nFormat(
                        "loadListSuccess", it.successful.map { s -> s.atc }.size, ap.name))

                // TODO: maybe better way, this is to general
                if (it.errored.isNotEmpty()) {
                    mb.append("\n").append(context.i18nFormat("loadPlaylistGeneralError", "`${it.errored.size}`"))
                }

                if (it.successful.isNotEmpty()) {
                    loader.player.play()
                }
            }
        } catch (th: Throwable) {
            loader.handleThrowable(context, th)
        }

        loader.loadNextAsync()
    }

    override fun noMatches() {
        try {
            context.reply(context.i18nFormat("loadNoMatches", context.identifier))
        } catch (th: Throwable) {
            loader.handleThrowable(context, th)
        }

        loader.loadNextAsync()
    }

    private fun loadSplit(at: AudioTrack, ic: IdentifierContext) {
        if (at !is YoutubeAudioTrack) {
            ic.reply(ic.i18n("loadSplitNotYouTube"))
            return
        }

        val yv = loader.youtubeAPI.getVideoFromID(at.identifier, true)
        val desc = yv.description
        val m = AudioLoader.SPLIT_DESCRIPTION_PATTERN.matcher(desc)

        val pairs = ArrayList<Pair<Long, String>>()

        while (m.find()) {
            val timestamp: Long
            try {
                timestamp = TextUtils.parseTimeString(m.group(2))
            } catch (e: NumberFormatException) {
                continue
            }

            val title1 = m.group(1)
            val title2 = m.group(3)

            if (title1.length > title2.length) {
                pairs.add(ImmutablePair(timestamp, title1))
            } else {
                pairs.add(ImmutablePair(timestamp, title2))
            }
        }

        if (pairs.size < 2) {
            ic.reply(ic.i18n("loadSplitNotResolves"))
            return
        }

        val list = ArrayList<SplitAudioTrackContext>()

        for ((i, pair) in pairs.withIndex()) {
            val startPos: Long
            val endPos: Long

            if (i != pairs.size - 1) {
                // Not last
                startPos = pair.left
                endPos = pairs[i + 1].left
            } else {
                // Last
                startPos = pair.left
                endPos = at.getDuration()
            }

            val newAt = at.makeClone()
            newAt.position = startPos

            val atc = SplitAudioTrackContext(newAt, context.member, startPos, endPos, pair.right)

            list.add(atc)
        }

        GlobalScope.mono { loader.player.queueLimited(list) }.subscribe {
            var mb = localMessageBuilder().append(ic.i18n("loadFollowingTracksAdded")).append("\n")

            for (atc in it.filter { status -> status.canQueue }.map { status -> status.atc }) {
                mb.append("`[")
                        .append(TextUtils.formatTime(atc.effectiveDuration))
                        .append("]` ")
                        .append(atc.effectiveTitle.escapeAndDefuse())
                        .append("\n")
            }

            //This is pretty spammy .. let's use a shorter one
            if (mb.length > 800) {
                mb = localMessageBuilder()
                        .append(ic.i18nFormat("loadPlaylistTooMany", list.size))
            }

            context.reply(mb.build())

            if (it.any { status -> status.canQueue }) {
                loader.player.play()
            }
        }
    }
}
