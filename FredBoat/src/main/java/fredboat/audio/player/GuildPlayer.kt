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

package fredboat.audio.player

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.sedmelluq.discord.lavaplayer.track.TrackMarker
import fredboat.audio.lavalink.SentinelLavalink
import fredboat.audio.lavalink.SentinelLink
import fredboat.audio.queue.*
import fredboat.audio.queue.limiter.QueueLimitStatus
import fredboat.audio.queue.limiter.QueueLimiter
import fredboat.audio.queue.handlers.*
import fredboat.command.music.control.VoteSkipCommand
import fredboat.commandmeta.MessagingException
import fredboat.commandmeta.abs.CommandContext
import fredboat.db.api.GuildSettingsRepository
import fredboat.definitions.RepeatMode
import fredboat.sentinel.Guild
import fredboat.sentinel.InternalGuild
import fredboat.sentinel.TextChannel
import fredboat.util.TextUtils
import fredboat.util.extension.escapeAndDefuse
import fredboat.util.ratelimit.Ratelimiter
import fredboat.util.rest.YoutubeAPI
import fredboat.ws.emptyPlayerInfo
import fredboat.ws.toPlayerInfo
import lavalink.client.player.IPlayer
import lavalink.client.player.LavalinkPlayer
import lavalink.client.player.event.PlayerEventListenerAdapter
import org.bson.types.ObjectId
import org.json.JSONObject
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.Consumer
import javax.annotation.CheckReturnValue

class GuildPlayer(
        val lavalink: SentinelLavalink,
        var guild: Guild,
        private val musicTextChannelProvider: MusicTextChannelProvider,
        audioPlayerManager: AudioPlayerManager,
        private val guildSettingsRepository: GuildSettingsRepository,
        ratelimiter: Ratelimiter,
        youtubeAPI: YoutubeAPI
) : PlayerEventListenerAdapter() {

    val queueHandler: IQueueHandler = RepeatableQueueHandler(this)
    private val audioLoader = AudioLoader(ratelimiter, queueHandler, audioPlayerManager, this, youtubeAPI)
    private val queueLimiter = QueueLimiter(guildSettingsRepository)
    val guildId = guild.id
    val player: LavalinkPlayer = lavalink.getLink(guild.id.toString()).player
    var internalContext: AudioTrackContext? = null

    private var onPlayHook: Consumer<AudioTrackContext>? = null
    private var onErrorHook: Consumer<Throwable>? = null
    @Volatile
    private var lastLoadedTrack: AudioTrackContext? = null
    val historyQueue = ConcurrentLinkedQueue<AudioTrackContext>()

    companion object {
        private val log = LoggerFactory.getLogger(GuildPlayer::class.java)
        private const val MAX_HISTORY_SIZE = 20
    }

    /**
     * @return The text channel currently used for music commands.
     *
     * May return null if the channel was deleted.
     */
    val activeTextChannel: TextChannel?
        get() {
            if (!guild.selfPresent) return null
            return musicTextChannelProvider.getMusicTextChannel(guild)
        }

    var repeatMode: RepeatMode
        get() = if (queueHandler is IRepeatableQueueHandler)
            queueHandler.repeat
        else
            RepeatMode.OFF
        set(repeatMode) = if (queueHandler is IRepeatableQueueHandler) {
            queueHandler.repeat = repeatMode
        } else {
            throw UnsupportedOperationException("Can't repeat " + queueHandler.javaClass)
        }

    var isRoundRobin : Boolean
        get() = queueHandler is IRoundRobinQueueHandler && queueHandler.roundRobin
        set(value) = if (queueHandler is IRoundRobinQueueHandler) {
            queueHandler.roundRobin = value
        } else {
            throw UnsupportedOperationException("Can't round robin " + queueHandler.javaClass)
        }

    var isShuffle: Boolean
        get() = queueHandler is IShufflableQueueHandler && queueHandler.shuffle
        set(shuffle) = if (queueHandler is IShufflableQueueHandler) {
            queueHandler.shuffle = shuffle
            internalContext?.isPriority = false
        } else {
            throw UnsupportedOperationException("Can't shuffle " + queueHandler.javaClass)
        }

    private fun getTrackAnnounceStatus(): Mono<Boolean> {
            if (guild.selfPresent) {
                 return guildSettingsRepository.fetch(guild.id).map { it.trackAnnounce }
            }

            return Mono.just(false)
        }

    val playingTrack: AudioTrackContext?
        get() {
            return if (player.playingTrack == null && internalContext == null) {
                queueHandler.peek()
            } else internalContext
        }

    //the unshuffled playlist
    //Includes currently playing track, which comes first
    val remainingTracks: List<AudioTrackContext>
        get() {
            log.trace("getRemainingTracks()")
            val list = ArrayList<AudioTrackContext>()
            val atc = playingTrack
            if (atc != null) {
                list.add(atc)
            }

            list.addAll(queueHandler.queue.toList())
            return list
        }

    var volume: Float
        get() = player.volume.toFloat() / 100
        set(vol) {
            player.volume = (vol * 100).toInt()
        }

    val isPlaying: Boolean
        get() = player.playingTrack != null && !player.isPaused

    val isPaused: Boolean
        get() = player.isPaused

    val position: Long
        get() = player.trackPosition

    init {
        log.debug("Constructing GuildPlayer({})", guild)
        onPlayHook = Consumer { this.announceTrack(it) }
        onErrorHook = Consumer { this.handleError(it) }
        @Suppress("LeakingThis")
        player.addListener(this)
        linkPostProcess()
    }

    private fun announceTrack(atc: AudioTrackContext) {
        val activeTextChannel = activeTextChannel

        getTrackAnnounceStatus().subscribe {
            if (it && !isPaused && repeatMode != RepeatMode.SINGLE) {
                activeTextChannel?.send(atc.i18nFormat(
                        "trackAnnounce",
                        atc.effectiveTitle.escapeAndDefuse(),
                        atc.member.effectiveName.escapeAndDefuse()
                ))?.subscribe()
            }
        }
    }

    private fun handleError(t: Throwable) {
        if (t !is MessagingException) {
            log.error("Guild player error", t)
        }
        val activeTextChannel = activeTextChannel
        activeTextChannel?.send("Something went wrong!\n${t.message}")?.subscribe()
    }

    fun queueAsync(identifier: String, context: CommandContext, isPriority: Boolean = false) {
        val ic = IdentifierContext(identifier, context.textChannel, context.member)
        ic.isPriority = isPriority

        joinChannel(context.member)

        audioLoader.loadAsync(ic)
    }

    fun queueAsync(ic: IdentifierContext) {
        joinChannel(ic.member)

        audioLoader.loadAsync(ic)
    }

    fun queue(atc: AudioTrackContext) {
        if (!guild.selfPresent) throw IllegalStateException("Attempt to queue track in a guild we are not present in")

        val member = guild.getMember(atc.userId)
        if (member != null) {
            joinChannel(member)
        }

        queueHandler.add(atc)
        if (isPlaying) updateClients()
    }

    /** Add a bunch of tracks to the track provider */
    fun queueAll(tracks: Collection<AudioTrackContext>) {
        queueHandler.addAll(tracks)
    }

    @CheckReturnValue
    suspend fun queueLimited(atc: AudioTrackContext): QueueLimitStatus {
        val status = queueLimiter.isQueueLimited(atc, this)

        // only queue if track was not limited
        if (status.canQueue) {
            queue(atc)
        }

        return status
    }

    suspend fun queueLimited(tracks: List<AudioPlaylistContext>): List<QueueLimitStatus> {
        val states = queueLimiter.isQueueLimited(tracks, this)

        queueAll(states.filter { it.canQueue }.map { it.atc })
        return states
    }


    override fun toString(): String {
        return "[GP:$guildId]"
    }

    fun reshuffle() {
        if (queueHandler is IShufflableQueueHandler) {
            queueHandler.reshuffle()
            internalContext?.isPriority = false
            updateClients()
        } else {
            throw UnsupportedOperationException("Can't reshuffle " + queueHandler.javaClass)
        }
    }

    fun skipTracks(trackIds: Collection<ObjectId>) {
        var skipCurrentTrack = false

        val toRemove = ArrayList<ObjectId>()
        val playing = if (player.playingTrack != null) internalContext else null
        for (trackId in trackIds) {
            if (playing != null && trackId == playing.trackId) {
                //Should be skipped last, in respect to PlayerEventListener
                skipCurrentTrack = true
            } else {
                toRemove.add(trackId)
            }
        }

        if (toRemove.size > 0) queueHandler.removeById(toRemove)
        if (skipCurrentTrack) skip()
    }

    override fun onTrackStart(player: IPlayer?, track: AudioTrack?) {
        voteSkipCleanup()
        super.onTrackStart(player, track)
    }

    fun destroy() {
        queueHandler.clear()
        stop()
        player.removeListener(this)
        player.link.destroy()
        log.info("Player for $guildId was destroyed.")
        lavalink.userSessionHandler.sendLazy(guildId) { emptyPlayerInfo }
    }

    private fun voteSkipCleanup() {
        VoteSkipCommand.guildSkipVotes.remove(guildId)
    }

    /**
     * Invoked when subscribing to this player's guild, with an already existing guild
     */
    fun linkPostProcess() {
        val iGuild = guild as InternalGuild
        val vsu = iGuild.cachedVsu
        val slink = player.link as SentinelLink
        if (vsu != null) {
            iGuild.cachedVsu = null
            slink.onVoiceServerUpdate(JSONObject(vsu.raw), vsu.sessionId)
            log.info("Using cached VOICE_SERVER_UPDATE for $guild")

            val vc = guild.selfMember.voiceChannel
            if (vc == null)
                log.warn("Using cached VOICE_SERVER_UPDATE, but it doesn't appear like we are in a voice channel!")
            else
                slink.setChannel(vc.idString)
        } else {
            val vc = slink.getChannel(guild) ?: return
            slink.connect(vc, skipIfSameChannel = false)
        }
        updateClients()
    }

    fun play() {
        log.trace("play()")

        if (player.isPaused) {
            player.isPaused = false
        }
        if (player.playingTrack == null) {
            logListeners()
            loadAndPlay()
        }

    }

    fun setPause(pause: Boolean) {
        log.trace("setPause({})", pause)

        if (pause) {
            player.isPaused = true
        } else {
            player.isPaused = false
            play()
        }
        updateClients()
    }

    fun pause() = setPause(true)

    /**
     * Clear the tracklist and stop the current track
     */
    fun stop() {
        log.trace("stop()")

        queueHandler.clear()
        stopTrack()
    }

    /**
     * Skip the current track
     */
    fun skip() {
        log.trace("skip()")

        queueHandler.onSkipped()
        stopTrack()
    }

    /**
     * Stop the current track.
     */
    fun stopTrack() {
        log.trace("stopTrack()")

        internalContext = null
        player.stopTrack()
    }

    override fun onTrackEnd(player: IPlayer?, track: AudioTrack?, endReason: AudioTrackEndReason?) {
        log.debug("onTrackEnd({} {} {}) called", track!!.info.title, endReason!!.name, endReason.mayStartNext)

        if (endReason == AudioTrackEndReason.FINISHED || endReason == AudioTrackEndReason.STOPPED) {
            updateHistoryQueue()
            loadAndPlay()
        } else if (endReason == AudioTrackEndReason.CLEANUP) {
            log.info("Track " + track.identifier + " was cleaned up")
        } else if (endReason == AudioTrackEndReason.LOAD_FAILED) {
            if (onErrorHook != null)
                onErrorHook!!.accept(MessagingException("Track `" + TextUtils.escapeAndDefuse(track.info.title) + "` failed to load. Skipping..."))
            queueHandler.onSkipped()
            loadAndPlay()
        } else {
            log.warn("Track " + track.identifier + " ended with unexpected reason: " + endReason)
        }
    }

    //request the next track from the track provider and start playing it
    private fun loadAndPlay() {
        log.trace("loadAndPlay()")

        val atc = queueHandler.take()
        lastLoadedTrack = atc
        atc?.let { playTrack(it) }
        updateClients()
    }

    private fun updateHistoryQueue() {
        val lastTrack = lastLoadedTrack
        if (lastTrack == null) {
            log.warn("No lastLoadedTrack in $this after track end")
            return
        }
        if (historyQueue.size == MAX_HISTORY_SIZE) {
            historyQueue.poll()
        }
        historyQueue.add(lastTrack)
    }

    /**
     * Plays the provided track.
     *
     * Silently playing a track will not trigger the onPlayHook (which announces the track usually)
     */
    private fun playTrack(trackContext: AudioTrackContext, silent: Boolean = false) {
        log.trace("playTrack({})", trackContext.effectiveTitle)

        internalContext = trackContext
        player.playTrack(trackContext.track)
        trackContext.track.position = trackContext.startPosition

        if (trackContext is SplitAudioTrackContext) {
            //Ensure we don't step over our bounds
            log.info("Start: ${trackContext.startPosition} End: ${trackContext.startPosition + trackContext.effectiveDuration}")

            trackContext.track.setMarker(
                    TrackMarker(trackContext.startPosition + trackContext.effectiveDuration,
                            TrackEndMarkerHandler(this, trackContext)))
        }

        if (!silent && onPlayHook != null) onPlayHook!!.accept(trackContext)
    }

    override fun onTrackException(player: IPlayer?, track: AudioTrack, exception: Exception?) {
        log.error("Lavaplayer encountered an exception while playing {}",
                track.identifier, exception)
    }

    override fun onTrackStuck(player: IPlayer?, track: AudioTrack, thresholdMs: Long) {
        log.error("Lavaplayer got stuck while playing {}",
                track.identifier)
    }

    fun seekTo(position: Long) {
        if (internalContext!!.track.isSeekable) {
            player.seekTo(position)
            updateClients()
        } else {
            throw MessagingException(internalContext!!.i18n("seekDeniedLiveTrack"))
        }
    }

    private fun logListeners() {
        humanUsersInCurrentVC.forEach { lavalink.activityMetrics.logListener(it) }
    }

    private fun updateClients() {
        lavalink.userSessionHandler.sendLazy(guildId) { toPlayerInfo() }
    }
}
