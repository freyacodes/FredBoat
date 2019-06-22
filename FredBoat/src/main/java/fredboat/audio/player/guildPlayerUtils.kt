package fredboat.audio.player

import com.fredboat.sentinel.entities.ShardStatus
import com.google.common.collect.Lists
import fredboat.audio.queue.AudioTrackContext
import fredboat.audio.queue.handlers.IQueueHandler
import fredboat.commandmeta.MessagingException
import fredboat.commandmeta.abs.CommandContext
import fredboat.definitions.PermissionLevel
import fredboat.feature.I18n
import fredboat.perms.Permission
import fredboat.perms.PermsUtil
import fredboat.sentinel.Member
import fredboat.sentinel.VoiceChannel
import org.apache.commons.lang3.tuple.ImmutablePair
import org.apache.commons.lang3.tuple.Pair
import org.bson.types.ObjectId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.streams.toList

private val log: Logger = LoggerFactory.getLogger(GuildPlayer::class.java)

val GuildPlayer.trackCount: Int
    get() {
        var trackCount = queueHandler.size
        if (player.playingTrack != null) trackCount++
        return trackCount
    }

/** Live streams are considered to have a length of 0 */
val GuildPlayer.totalRemainingMusicTimeMillis: Long
    get() {
        var millis = queueHandler.totalDuration

        val currentTrack = if (player.playingTrack != null) internalContext else null
        if (currentTrack != null && !currentTrack.track.info.isStream) {
            millis += Math.max(0, currentTrack.effectiveDuration - position)
        }
        return millis
    }

val GuildPlayer.streamsCount: Long
    get() {
        var streams = queueHandler.streamCount.toLong()
        val atc = if (player.playingTrack != null) internalContext else null
        if (atc != null && atc.track.info.isStream) streams++
        return streams
    }

val GuildPlayer.voiceChannel: VoiceChannel?
    get() = guild.selfMember.voiceChannel

/**
 * @return Users who are not bots
 */
val GuildPlayer.humanUsersInCurrentVC: List<Member>
    get() = voiceChannel.getHumanUsersInVC()

val GuildPlayer.isQueueEmpty: Boolean
    get() {
        log.trace("isQueueEmpty()")

        return player.playingTrack == null && queueHandler.isEmpty
    }

val GuildPlayer.trackCountInHistory: Int
    get() = historyQueue.size

val GuildPlayer.isHistoryQueueEmpty: Boolean
    get() = historyQueue.isEmpty()

fun GuildPlayer.getUserTrackCount(userId: Long): Int {
    var trackCount = queueHandler.queue.filter { it.userId == userId }.size
    if (player.playingTrack != null && internalContext?.userId == userId) trackCount++
    return trackCount
}

fun GuildPlayer.joinChannel(usr: Member) {
    joinChannel(usr.voiceChannel)
}

fun GuildPlayer.joinChannel(targetChannel: VoiceChannel?) {
    if (targetChannel == null) {
        throw MessagingException(I18n.get(guild).getString("playerUserNotInChannel"))
    }
    if (targetChannel == voiceChannel) {
        // already connected to the channel
        return
    }

    val guild = targetChannel.guild
    val permissions = targetChannel.ourEffectivePermissions

    if (permissions hasNot Permission.VIEW_CHANNEL) {
        val i18n = I18n.get(guild).getString("permissionMissingBot")
        throw MessagingException("$i18n ${Permission.VIEW_CHANNEL.uiName}")
    }

    if (permissions hasNot Permission.VOICE_CONNECT && guild.selfMember.voiceChannel != targetChannel) {
        throw MessagingException(I18n.get(guild).getString("playerJoinConnectDenied"))
    }

    if (permissions hasNot Permission.VOICE_SPEAK) {
        throw MessagingException(I18n.get(guild).getString("playerJoinSpeakDenied"))
    }

    if (targetChannel.userLimit > 0
            && targetChannel.userLimit <= targetChannel.members.size
            && permissions hasNot Permission.VOICE_MOVE_OTHERS) {
        throw MessagingException(String.format("The channel you want me to join is full!" +
                " Please free up some space, or give me the permission to **%s** to bypass the limit.", //todo i18n
                Permission.VOICE_MOVE_OTHERS.uiName))
    }

    val link = lavalink.getLink(guild)

    if (link.state == ShardStatus.CONNECTED && guild.selfMember.voiceChannel?.members?.contains(guild.selfMember) == false) {
        log.warn("Link is ${link.state} but we are not in its channel. Assuming our session expired...")
        link.onDisconnected()
    }

    try {
        link.connect(targetChannel)
        log.info("Connected to voice channel $targetChannel")
    } catch (e: Exception) {
        log.error("Failed to join voice channel {}", targetChannel, e)
    }
}

fun GuildPlayer.leaveVoiceChannelRequest(commandContext: CommandContext, silent: Boolean) {
    if (!silent) {
        val currentVc = commandContext.guild.selfMember.voiceChannel
        if (currentVc == null) {
            commandContext.reply(commandContext.i18n("playerNotInChannel"))
        } else {
            commandContext.reply(commandContext.i18nFormat("playerLeftChannel", currentVc.name))
        }
    }
    lavalink.getLink(guild).disconnect()
}

@Suppress("LocalVariableName")
fun GuildPlayer.getTracksInRange(start: Int, end: Int): List<AudioTrackContext> {
    // Make mutable
    var start_ = start
    var end_ = end

    val result = ArrayList<AudioTrackContext>()

    //adjust args for whether there is a track playing or not

    if (player.playingTrack != null) {
        if (start_ <= 0) {
            result.add(internalContext!!)
            end_--//shorten the requested range by 1, but still start at 0, since that's the way the trackprovider counts its tracks
        } else {
            //dont add the currently playing track, drop the args by one since the "first" track is currently playing
            start_--
            end_--
        }
    } else {
        //nothing to do here, args are fine to pass on
    }

    result.addAll((queueHandler as IQueueHandler).getInRange(start_, end_))
    return result
}

/** Similar to [getTracksInRange], but only gets the trackIds */
fun GuildPlayer.getTrackIdsInRange(start: Int, end: Int): List<ObjectId> = getTracksInRange(start, end).stream()
        .map { it.trackId }
        .toList()

fun VoiceChannel?.getHumanUsersInVC(): List<Member> {
    this ?: return emptyList()
    return this.members.stream()
            .filter { !it.isBot }
            .toList()
}

//Success, fail message
private suspend fun GuildPlayer.canMemberSkipTracks(member: Member, trackIds: Collection<ObjectId>): Pair<Boolean, String> {
    if (PermsUtil.checkPerms(PermissionLevel.DJ, member)) {
        return ImmutablePair(true, null)
    } else {
        //We are not a mod
        val userId = member.id

        //if there is a currently playing track, and the track is requested to be skipped, but not owned by the
        // requesting user, then currentTrackSkippable should be false
        var currentTrackSkippable = true
        val playingTrack = playingTrack
        if (playingTrack != null
                && trackIds.contains(playingTrack.trackId)
                && playingTrack.userId != userId) {

            currentTrackSkippable = false
        }

        return if (currentTrackSkippable && queueHandler.isUserTrackOwner(userId, trackIds)) { //check ownership of the queued tracks
            ImmutablePair(true, null)
        } else {
            ImmutablePair(false, I18n.get(guild).getString("skipDeniedTooManyTracks"))
        }
    }
}

suspend fun GuildPlayer.skipTracksForMemberPerms(context: CommandContext, trackIds: Collection<ObjectId>, successMessage: String) {
    val pair = canMemberSkipTracks(context.member, trackIds)

    if (pair.left) {
        context.reply(successMessage)
        skipTracks(trackIds)
    } else {
        context.replyWithName(pair.right)
    }
}

fun GuildPlayer.getTracksInHistory(start: Int, end: Int): List<AudioTrackContext> {
    val start2 = Math.max(start, 0)
    val end2 = Math.max(end, start)
    val historyList = ArrayList(historyQueue)

    return if (historyList.size >= end2) {
        Lists.reverse(ArrayList(historyQueue)).subList(start2, end2)
    } else {
        ArrayList()
    }
}