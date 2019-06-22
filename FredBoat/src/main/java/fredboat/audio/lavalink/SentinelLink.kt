package fredboat.audio.lavalink

import com.fredboat.sentinel.entities.AudioQueueRequest
import com.fredboat.sentinel.entities.AudioQueueRequestEnum.*
import fredboat.perms.InsufficientPermissionException
import fredboat.perms.Permission.*
import fredboat.sentinel.Guild
import fredboat.sentinel.VoiceChannel
import lavalink.client.io.Link
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SentinelLink(val lavalink: SentinelLavalink, guildId: String) : Link(lavalink, guildId) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SentinelLink::class.java)
        private const val MIN_RETRY_INTERVAL = 200_000L
    }

    private var lastRetryTime = 0L

    private val routingKey: String
            get() {
                val sId = ((guildId.toLong() shr 22) % lavalink.appConfig.shardCount.toLong()).toInt()
                return lavalink.sentinel.tracker.getKey(sId)
            }

    override fun removeConnection() =
            lavalink.sentinel.sendAndForget(routingKey, AudioQueueRequest(REMOVE, guildId.toLong()))

    override fun queueAudioConnect(channelId: Long) =
            lavalink.sentinel.sendAndForget(routingKey, AudioQueueRequest(QUEUE_CONNECT, guildId.toLong(), channelId))

    public override fun queueAudioDisconnect() =
            lavalink.sentinel.sendAndForget(routingKey, AudioQueueRequest(QUEUE_DISCONNECT, guildId.toLong()))

    fun connect(channel: VoiceChannel, skipIfSameChannel: Boolean = true) {
        if (channel.guild.id != guild)
            throw IllegalArgumentException("The provided VoiceChannel is not a part of the Guild that this AudioManager " +
                    "handles. Please provide a VoiceChannel from the proper Guild")

        val perms = channel.ourEffectivePermissions

        if (perms hasNot VOICE_CONNECT && perms hasNot VOICE_MOVE_OTHERS)
            throw InsufficientPermissionException(VOICE_CONNECT, "We do not have permission to join $channel")
        perms.assertHas(VOICE_SPEAK, "We do not have permission to speak in $channel")

        // Do nothing if we are already connected to that channel
        val alreadyInChannel = channel.members.any { it.isUs } && super.getChannel() == channel.id.toString()
        if (skipIfSameChannel && alreadyInChannel) return

        if (channel.userLimit > 1 // Is there a user limit?
                && channel.userLimit <= channel.members.size // Is that limit reached?
        ){
            perms.assertHas(VOICE_MOVE_OTHERS, "$channel already has [${channel.members.size}/${channel.userLimit}] " +
                    "members, and we don't have $VOICE_MOVE_OTHERS to bypass the limit.")
        }

        state = Link.State.CONNECTING
        queueAudioConnect(channel.id)
    }

    fun getChannel(guild: Guild): VoiceChannel? {
        val id = channel ?: return null
        val vc = guild.getVoiceChannel(id.toLong())

        if (vc != null) return vc

        log.warn("Lavalink appears to be connected to vc $id, which doesn't seem to exist in $guild")
        return null
    }

    override fun onVoiceWebSocketClosed(code: Int, reason: String, byRemote: Boolean) {
        val by = if (byRemote) "Discord" else "LLS"
        log.info("{}: Lavalink voice WS closed by {}, code {}: {}", guild, by, code, reason)
        if (code == 4006) { // Session expired
            if (System.currentTimeMillis() - lastRetryTime > MIN_RETRY_INTERVAL) {
                val vc = channel
                if (vc == null) {
                    log.error("{}: Attempted to reconnect after code 4006, but channel is null. Race condition?", guild)
                    return
                }
                log.info("{}: Queuing new voice connection after expired session from Sentinel", guild)
                lastRetryTime = System.currentTimeMillis()
                queueAudioDisconnect()
                queueAudioConnect(vc.toLong())
            } else {
                log.warn("{}: Got WS close code $code twice within $MIN_RETRY_INTERVAL ms, disconnecting " +
                        "to prevent bouncing and getting stuck...", guild)
                disconnect()
            }
        }
    }

}