/*
 * MIT License
 *
 * Copyright (c) 2017-2018 Frederik Ar. Mikkelsen
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
 */

package fredboat.event

import com.fredboat.sentinel.entities.LifecycleEventEnum.*
import com.fredboat.sentinel.entities.ShardLifecycleEvent
import fredboat.agent.GuildCacheInvalidationAgent
import fredboat.audio.player.PlayerRegistry
import fredboat.audio.player.voiceChannel
import fredboat.config.property.AppConfig
import fredboat.sentinel.Guild
import fredboat.sentinel.GuildCache
import fredboat.util.DiscordUtil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Stream
import kotlin.streams.toList

/**
 * Created by napster on 24.02.18.
 */
@Component
class ShardLifecycleHandler(
        private val playerRegistry: PlayerRegistry,
        private val appConfig: AppConfig,
        private val guildCache: GuildCache
) : SentinelEventHandler() {

    companion object {
        private val log = LoggerFactory.getLogger(ShardLifecycleHandler::class.java)
    }

    private val channelsToRejoin = ConcurrentHashMap<Int, MutableList<ChannelReference>>()

    override fun onShardLifecycle(event: ShardLifecycleEvent) {
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (event.change) {
            READIED -> resolveSyncIssues(event).subscribe {
                readyReviveHandling(event) // A revive should cause READIED, though never a RECONNECTED
            }
            RECONNECTED -> resolveSyncIssues(event).subscribe()
            SHUTDOWN -> shutdownReviveHandling(event)
        }
    }

    /**
     * After [READIED] or [RECONNECTED], we may have to resubscribe. Guild state may have changed between sessions.
     */
    private fun resolveSyncIssues(event: ShardLifecycleEvent): Flux<Void> {
        val sId = event.shard.id
        val affectedGuilds = guildCache.cache.values.filter {
            DiscordUtil.getShardId(it.id, appConfig) == sId
        }
        log.info("Synchronizing subscribed guilds after receiving ${event.change} for ${event.shard}. " +
                "${affectedGuilds.size} guilds are affected")

        val counter = AtomicInteger(0)
        val monos = affectedGuilds.asSequence().map { guild ->
            guildCache.get(guild.id, skipCache = true)
                    .then() // We don't care for the guild
                    .retry(2)
                    .doOnSuccess { counter.incrementAndGet() }
                    .onErrorResume { e ->
                        Mono.from {
                            log.error("Exception while re-syncing guild. We are forced to unsubscribe", e)
                            GuildCacheInvalidationAgent.INSTANCE.invalidateGuild(guild)
                        }
                    }
        }.toMutableList()

        return Flux.merge(monos).doFinally {
            log.info("Synchronized [${counter.get()}/${affectedGuilds.size}] guilds in ${event.shard} (Signal: $it)")
        }
    }

    private fun shutdownReviveHandling(event: ShardLifecycleEvent) {
        try {
            val shardId = event.shard.id
            channelsToRejoin[shardId] = playerRegistry.playingPlayers.stream()
                    .filter { DiscordUtil.getShardId(it.guildId, appConfig) == shardId }
                    .flatMap {
                        val channel = it.voiceChannel ?: return@flatMap Stream.empty<ChannelReference>()
                        return@flatMap Stream.of(ChannelReference(channel.guild, channel.id))
                    }.toList().toMutableList()
        } catch (ex: Exception) {
            log.error("Caught exception while saving channels to revive shard {}", event.shard, ex)
        }
    }

    /** Rejoin old channels if revived */
    private fun readyReviveHandling(event: ShardLifecycleEvent) {
        val channels = channelsToRejoin.computeIfAbsent(event.shard.id) { ArrayList(it) }
        val toRejoin = ArrayList(channels)
        channels.clear()//avoid situations where this method is called twice with the same channels

        toRejoin.forEach { ref ->
            val channel = ref.guild.getVoiceChannel(ref.channelId) ?: return@forEach
            playerRegistry.getOrCreate(channel.guild).subscribe { player ->
                channel.connect()
                player.play()
            }
        }
    }

    data class ChannelReference(
            val guild: Guild,
            val channelId: Long
    )
}
