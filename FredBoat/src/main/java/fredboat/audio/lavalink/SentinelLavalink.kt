package fredboat.audio.lavalink

import com.fredboat.sentinel.entities.VoiceServerUpdate
import fredboat.audio.player.PlayerRegistry
import fredboat.config.idString
import fredboat.config.property.AppConfig
import fredboat.config.property.LavalinkConfig
import fredboat.db.mongo.ActivityMetricsController
import fredboat.sentinel.Guild
import fredboat.sentinel.GuildCache
import fredboat.sentinel.Sentinel
import fredboat.util.DiscordUtil
import fredboat.ws.UserSessionHandler
import lavalink.client.io.Lavalink
import lavalink.client.io.metrics.LavalinkCollector
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.TimeoutException

@Suppress("ImplicitThis", "LeakingThis")
@Service
class SentinelLavalink(
        val sentinel: Sentinel,
        val guildCache: GuildCache,
        val appConfig: AppConfig,
        val activityMetrics: ActivityMetricsController,
        val userSessionHandler: UserSessionHandler,
        lavalinkConfig: LavalinkConfig
) : Lavalink<SentinelLink>(
        sentinel.selfUser.idString,
        appConfig.shardCount
) {

    lateinit var playerRegistry: PlayerRegistry
    private val resumeKey = "fredboat-${sentinel.selfUser.idString}"

    companion object {
        lateinit var INSTANCE: SentinelLavalink
        private const val DEFAULT_RESUME_TIMEOUT = 300 // 5 mins
        private val log: Logger = LoggerFactory.getLogger(SentinelLavalink::class.java)
    }

    init {
        INSTANCE = this
        setHoldEvents(true)
        lavalinkConfig.nodes.forEach {
            addNode(it.name, it.uri, it.password, resumeKey)
                    .setResuming(resumeKey, DEFAULT_RESUME_TIMEOUT)
        }
        LavalinkCollector(this).register<LavalinkCollector>()
    }

    override fun buildNewLink(guildId: String): SentinelLink {
        val shardId = DiscordUtil.getShardId(guildId.toLong(), appConfig)
        val shardMono = sentinel.tracker.awaitHello(shardId)
        val guildMono = guildCache.getGuildMono(guildId.toLong()).doOnSuccess { guild ->
            if (guild == null) {
                log.warn("Built link for non-existing guild. This should not happen.")
                return@doOnSuccess
            }
            playerRegistry.getOrCreate(guild).subscribe()
        }

        shardMono.timeout(Duration.ofSeconds(120))
                .onErrorMap(TimeoutException::class.java) {
                    RuntimeException("Attempted to resume Lavalink, but Sentinel hello timed out")
                }.then(guildMono)
                .subscribe()

        return SentinelLink(this, guildId)
    }

    fun getLink(guild: Guild) = getLink(guild.id.toString())
    fun getExistingLink(guild: Guild) = getExistingLink(guild.idString)

    fun onVoiceServerUpdate(update: VoiceServerUpdate) {
        val json = JSONObject(update.raw)
        val gId = json.getString("guild_id")
        val link = getLink(gId)

        link.onVoiceServerUpdate(json, update.sessionId)
    }
}