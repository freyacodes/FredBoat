package fredboat.agent

import com.fredboat.sentinel.entities.GuildUnsubscribeRequest
import fredboat.audio.lavalink.SentinelLavalink
import fredboat.audio.player.PlayerRegistry
import fredboat.db.mongo.PlayerRepository
import fredboat.db.mongo.convertAndSave
import fredboat.sentinel.GuildCache
import fredboat.sentinel.InternalGuild
import lavalink.client.io.Link
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.TimeUnit

@Controller
class GuildCacheInvalidationAgent(
        val guildCache: GuildCache,
        private val playerRegistry: PlayerRegistry,
        private val playerRepository: PlayerRepository,
        private val lavalink: SentinelLavalink
) : FredBoatAgent("cache-invalidator", 5, TimeUnit.MINUTES) {

    companion object {
        private const val TIMEOUT_MILLIS: Long = 10 * 60 * 1000 // 10 minutes
        private val log: Logger = LoggerFactory.getLogger(GuildCacheInvalidationAgent::class.java)
        lateinit var INSTANCE: GuildCacheInvalidationAgent
    }

    init {
        @Suppress("LeakingThis")
        INSTANCE = this
    }

    override fun doRun() {
        val keysToRemove = mutableListOf<InternalGuild>()
        guildCache.cache.forEach { _, guild ->
            if (!guild.shouldInvalidate()) return@forEach
            keysToRemove.add(guild)
        }
        keysToRemove.forEach {
            try {
                invalidateGuild(it)
            } catch (e: Exception) {
                log.error("Exception while invalidating guild $it")
            }
        }
    }

    private fun InternalGuild.shouldInvalidate(): Boolean {
        // Has this guild been used recently?
        if (lastUsed + TIMEOUT_MILLIS > System.currentTimeMillis()) return false

        // Are we connected to voice?
        if (link.state == Link.State.CONNECTED) return false

        // Are we playing music?
        if (this.guildPlayer?.isPlaying == true) return false

        // If not then invalidate
        return true
    }

    fun invalidateGuild(guild: InternalGuild) {
        val mono = playerRegistry.getExisting(guild)?.let {
            playerRepository.convertAndSave(it).timeout(Duration.ofSeconds(60))
        } ?: Mono.empty()
        mono.subscribe {
            try {
                playerRegistry.destroyPlayer(guild)
                lavalink.getExistingLink(guild)?.destroy()
            } catch (e: Exception) {
                log.error("Got exception when invaliding GuildPlayer and Link for {}", guild)
            }
            guild.sentinel.sendAndForget(guild.routingKey, GuildUnsubscribeRequest(guild.id))
            guildCache.cache.remove(guild.id)
        }
    }

}