package fredboat.audio.queue.limiter

import fredboat.audio.player.GuildPlayer
import fredboat.audio.player.getUserTrackCount
import fredboat.audio.player.trackCount
import fredboat.audio.queue.AudioPlaylistContext
import fredboat.audio.queue.AudioTrackContext
import fredboat.db.api.GuildSettingsRepository
import fredboat.feature.metrics.Metrics
import kotlinx.coroutines.reactive.awaitSingle
import reactor.core.publisher.Mono

class QueueLimiter(repository: GuildSettingsRepository) {
    private val queueLimits: ArrayList<QueueLimit> = arrayListOf()

    init {
        queueLimits.add(QueueLimit("NoPlaylist", { atc, _, _ ->
            repository.fetch(atc.guildId).map { QueueLimitStatus(atc !is AudioPlaylistContext || it.allowPlaylist, atc) }
        }, QueueLimiterEnum.PLAYLIST_DISABLED, { context, status ->
            Mono.just(context.i18n(status.i18n))
        }))

        queueLimits.add(QueueLimit("UserTrackLimit", { atc, player, preemptive ->
            repository.fetch(atc.guildId).map { QueueLimitStatus(it.userMaxTrackCount == null || it.userMaxTrackCount!! > player.getUserTrackCount(atc.userId) + preemptive, atc) }
        }, QueueLimiterEnum.USER_TRACK_LIMIT_EXCEEDED, { context, status ->
            repository.fetch(context.guild.id).map { context.i18nFormat(status.i18n, it.userMaxTrackCount ?: "UNLIMITED") }
        }))

        queueLimits.add(QueueLimit("TrackLimit", { atc, player, preemptive ->
            repository.fetch(atc.guildId).map { QueueLimitStatus(it.maxTrackCount == null || it.maxTrackCount!! > player.trackCount + preemptive, atc) }
        }, QueueLimiterEnum.TRACK_LIMIT_EXCEEDED, { context, status ->
            repository.fetch(context.guild.id).map { context.i18nFormat(status.i18n, it.maxTrackCount ?: "UNLIMITED") }
        }))

        queueLimits.add(QueueLimit("TrackLength", { atc, _, _ ->
            repository.fetch(atc.guildId).map { QueueLimitStatus(it.maxTrackLength == null || it.maxTrackLength!! > atc.effectiveDuration, atc) }
        }, QueueLimiterEnum.TRACK_LENGTH_EXCEEDED, { context, status ->
            repository.fetch(context.guild.id).map { context.i18nFormat(status.i18n, it.maxTrackLength ?: "UNLIMITED") }
        }))
    }

    suspend fun isQueueLimited(atc: AudioTrackContext, player: GuildPlayer, preemptive: Int = 0): QueueLimitStatus {
        var status = QueueLimitStatus(true, atc)

        for (limit in queueLimits) {
            status = limit.isAllowed(atc, player, preemptive)

            if (!status.canQueue) {
                Metrics.queuePrevented.labels(limit.name).inc()

                status.errorCode = limit.errorCode
                status.errorMessage = limit.errorMessage(atc, limit.errorCode).awaitSingle()
                break
            }
        }

        return status
    }

    suspend fun isQueueLimited(tracks: List<AudioPlaylistContext>, player: GuildPlayer): List<QueueLimitStatus> {
        return tracks.mapIndexed { i, atc -> isQueueLimited(atc, player, i) }
    }
}