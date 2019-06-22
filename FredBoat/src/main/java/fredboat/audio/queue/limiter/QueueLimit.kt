package fredboat.audio.queue.limiter

import fredboat.audio.player.GuildPlayer
import fredboat.audio.queue.AudioTrackContext
import fredboat.definitions.PermissionLevel
import fredboat.perms.PermsUtil
import kotlinx.coroutines.reactive.awaitSingle
import reactor.core.publisher.Mono

class QueueLimit(
        val name: String,
        private val limit: (AudioTrackContext, GuildPlayer, Int) -> Mono<QueueLimitStatus>,

        val errorCode: QueueLimiterEnum,
        val errorMessage: (AudioTrackContext, QueueLimiterEnum) -> Mono<String>
) {
    suspend fun isAllowed(atc: AudioTrackContext, player: GuildPlayer, preemptive: Int): QueueLimitStatus {

        // DJ and above are not affected by Limits
        if (PermsUtil.checkPerms(PermissionLevel.DJ, atc.member))
            return QueueLimitStatus(true, atc)

        // Dynamic execution of the limit
        return limit(atc, player, preemptive).awaitSingle()
    }
}