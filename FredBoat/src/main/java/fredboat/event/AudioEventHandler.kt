package fredboat.event

import com.fredboat.sentinel.entities.VoiceServerUpdate
import fredboat.audio.lavalink.SentinelLavalink
import fredboat.audio.player.PlayerRegistry
import fredboat.audio.player.getHumanUsersInVC
import fredboat.audio.player.humanUsersInCurrentVC
import fredboat.audio.player.voiceChannel
import fredboat.config.property.AppConfig
import fredboat.db.api.GuildSettingsRepository
import fredboat.feature.I18n
import fredboat.sentinel.Member
import fredboat.sentinel.VoiceChannel
import org.springframework.stereotype.Component

@Component
class AudioEventHandler(
        private val appConfig: AppConfig,
        private val playerRegistry: PlayerRegistry,
        private val lavalink: SentinelLavalink,
        private val guildSettingsRepository: GuildSettingsRepository
) : SentinelEventHandler() {

    override fun onVoiceJoin(channel: VoiceChannel, member: Member) {
        checkForAutoResume(channel, member)

        if (member.isUs) {
            getLink(channel).setChannel(channel.id.toString())
        } else if (member.guild.guildPlayer?.isPlaying == true &&
                member.guild.guildPlayer?.voiceChannel == member.voiceChannel) {
            lavalink.activityMetrics.logListener(member)
        }
    }

    override fun onVoiceLeave(channel: VoiceChannel, member: Member) {
        checkForAutoPause(channel)
        if (!member.isUs) return
        getLink(channel).onDisconnected()
    }

    override fun onVoiceMove(oldChannel: VoiceChannel, newChannel: VoiceChannel, member: Member) {
        checkForAutoResume(newChannel, member)
        checkForAutoPause(oldChannel)
        if (!member.isUs) return
        getLink(newChannel).setChannel(newChannel.id.toString())
    }

    override fun onVoiceServerUpdate(voiceServerUpdate: VoiceServerUpdate) =
            lavalink.onVoiceServerUpdate(voiceServerUpdate)

    private fun getLink(channel: VoiceChannel) = lavalink.getLink(channel.guild.idString)

    private fun checkForAutoPause(channelLeft: VoiceChannel) {
        if (appConfig.continuePlayback) return

        val player = playerRegistry.getExisting(channelLeft.guild.id) ?: return

        //are we in the channel that someone left from?
        val currentVc = player.voiceChannel
        if (currentVc != null && currentVc.id != channelLeft.id) {
            return
        }

        if (currentVc.getHumanUsersInVC().isEmpty() && !player.isPaused) {
            player.pause()
            player.activeTextChannel?.send(I18n.get(channelLeft.guild).getString("eventUsersLeftVC"))?.subscribe()
        }
    }

    private fun checkForAutoResume(joinedChannel: VoiceChannel, joined: Member) {
        val guild = joinedChannel.guild
        val player = playerRegistry.getExisting(guild) ?: return

        //ignore bot users that aren't us joining / moving
        if (joined.isBot && !joined.isUs)
            return

        if (player.isPaused
                && player.playingTrack != null
                && joinedChannel.members.contains(guild.selfMember)
                && player.humanUsersInCurrentVC.isNotEmpty()) {
            guildSettingsRepository.fetch(guild.id).subscribe {
                if (it.autoResume) {
                    player.setPause(false)
                    player.activeTextChannel?.send(I18n.get(guild).getString("eventAutoResumed"))?.subscribe()
                }
            }
        }
    }

}