package fredboat.db.mongo

import fredboat.audio.player.GuildPlayer
import fredboat.audio.player.voiceChannel
import fredboat.audio.queue.SplitAudioTrackContext
import fredboat.db.transfer.BlacklistEntity
import fredboat.db.transfer.GuildSettings
import fredboat.db.transfer.SearchResult
import fredboat.db.transfer.SearchResultId
import lavalink.client.LavalinkUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.annotation.Id
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


interface PlayerRepository : ReactiveCrudRepository<MongoPlayer, Long>
interface ActivityRepository : ReactiveCrudRepository<Activity, Long>
interface InternalGuildSettingsRepository : ReactiveCrudRepository<GuildSettings, Long>
interface InternalBlacklistRepository: ReactiveCrudRepository<BlacklistEntity, Long>
interface InternalSearchResultRepository: ReactiveCrudRepository<SearchResult, SearchResultId>

private val log: Logger = LoggerFactory.getLogger(PlayerRepository::class.java)

fun PlayerRepository.convertAndSave(player: GuildPlayer): Mono<MongoPlayer> {
    return save(player.toMongo())
}

fun PlayerRepository.convertAndSaveAll(players: List<GuildPlayer>): Flux<MongoPlayer> {
    val entities = players.mapNotNull {
        try {
            return@mapNotNull it.toMongo()
        } catch (e: Exception) {
            log.error("Problem saving player state", e)
            return@mapNotNull null
        }
    }
    return saveAll(entities)
}

private fun GuildPlayer.toMongo() = MongoPlayer(
        guildId,
        isPlaying,
        isPaused,
        isShuffle,
        isRoundRobin,
        repeatMode.ordinal.toByte(),
        volume,
        playingTrack?.track?.position,
        voiceChannel?.id,
        activeTextChannel?.id,
        remainingTracks.map {
            if (it is SplitAudioTrackContext) {
                MongoTrack(it.trackId, LavalinkUtil.toBinary(it.track), it.member.id, it.startPosition, it.endPosition, it.effectiveTitle)
            } else {
                MongoTrack(it.trackId, LavalinkUtil.toBinary(it.track), it.member.id, null, null, it.effectiveTitle)
            }
        }
)

class Activity(
        @Id
        val id: Long,
        val listenerDays: List<Int>
) {
    constructor(id: Long, day: Int) : this(id, listOf(day))
}


