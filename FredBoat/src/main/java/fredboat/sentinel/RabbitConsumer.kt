package fredboat.sentinel

import com.fredboat.sentinel.entities.*
import fredboat.config.SentryConfiguration
import fredboat.event.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.amqp.AmqpRejectAndDontRequeueException
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
@RabbitListener(queues = ["#{eventQueue}"], errorHandler = "rabbitListenerErrorHandler", concurrency = "50")
class RabbitConsumer(
        private val guildCache: GuildCache,
        private val sentinelTracker: SentinelTracker,
        private val sentinelSessionController: SentinelSessionController,
        eventLogger: EventLogger,
        guildHandler: GuildEventHandler,
        audioHandler: AudioEventHandler,
        messageHandler: MessageEventHandler,
        shardReviveHandler: ShardLifecycleHandler
) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RabbitConsumer::class.java)
    }
    private val shardStatuses = ConcurrentHashMap<Int, ShardStatus>()
    private val eventHandlers: List<SentinelEventHandler> = listOf(
            eventLogger,
            guildHandler,
            audioHandler,
            messageHandler,
            shardReviveHandler
    )

    @RabbitHandler(isDefault = true)
    fun default(msg: Any) = log.warn("Unhandled event $msg")

    @RabbitHandler
    fun onHello(hello: SentinelHello) = sentinelTracker.onHello(hello)

    /* Shard lifecycle */

    @RabbitHandler
    fun receive(event: ShardStatusChange) {
        event.shard.apply {
            log.info("Shard [$id / $total] status ${shardStatuses.getOrDefault(id, "<new>")} => $status")
            shardStatuses[id] = status
        }
        eventHandlers.forEach { it.onShardStatusChange(event) }
    }

    @RabbitHandler
    fun receive(event: ShardLifecycleEvent) {
        eventHandlers.forEach { it.onShardLifecycle(event) }
    }

    /* Guild events */

    @RabbitHandler
    fun receive(event: GuildJoinEvent) {
        log.info("Joined guild ${event.guild}")
        getGuild(event.guild) { guild ->
            eventHandlers.forEach { it.onGuildJoin(guild) }
        }

    }

    @RabbitHandler
    fun receive(event: GuildLeaveEvent) {
        log.info("Left guild ${event.guild}")
        guildCache.getIfCached(event.guild)?.let {
            (it as InternalGuild).onSelfLeaving()
            guildCache.cache.remove(event.guild)
        }
        val instant = Instant.ofEpochMilli(event.joinTime)
        eventHandlers.forEach { it.onGuildLeave(event.guild, instant) }
    }

    /* Voice events */

    @RabbitHandler
    fun receive(event: VoiceJoinEvent) {
        val guild = guildCache.getIfCached(event.guild) ?: return
        val channel = guild.getVoiceChannel(event.channel)
        val member = guild.getMember(event.member)

        if (channel == null) throw AmqpRejectAndDontRequeueException("Got VoiceJoinEvent for unknown channel ${event.channel}")
        if (member == null) throw AmqpRejectAndDontRequeueException("Got VoiceJoinEvent for unknown member ${event.member}")
        (channel as InternalVoiceChannel).handleVoiceJoin(member)

        eventHandlers.forEach { it.onVoiceJoin(channel, member) }
    }

    @RabbitHandler
    fun receive(event: VoiceLeaveEvent) {
        val guild = guildCache.getIfCached(event.guild) ?: return
        val channel = guild.getVoiceChannel(event.channel)
        val member = guild.getMember(event.member)

        (guild as InternalGuild).removeMemberFromAllVoiceChannels(event.member)
        if (channel == null) throw AmqpRejectAndDontRequeueException("Got VoiceLeaveEvent for unknown channel ${event.channel}")
        if (member == null) throw AmqpRejectAndDontRequeueException("Got VoiceLeaveEvent for unknown member ${event.member}")

        eventHandlers.forEach { it.onVoiceLeave(channel, member) }
    }

    @RabbitHandler
    fun receive(event: VoiceMoveEvent) {
        val guild = guildCache.getIfCached(event.guild) ?: return
        val old = guild.getVoiceChannel(event.oldChannel)
        val new = guild.getVoiceChannel(event.newChannel)
        val member = guild.getMember(event.member)

        if (old == null) throw AmqpRejectAndDontRequeueException("Got VoiceMoveEvent for unknown old channel ${event.oldChannel}")
        if (new == null) throw AmqpRejectAndDontRequeueException("Got VoiceMoveEvent for unknown new channel ${event.newChannel}")
        if (member == null) throw AmqpRejectAndDontRequeueException("Got VoiceMoveEvent for unknown member ${event.member}")
        (new as InternalVoiceChannel).handleVoiceJoin(member)

        eventHandlers.forEach { it.onVoiceMove(old, new, member) }
    }

    @RabbitHandler
    fun receive(event: VoiceServerUpdate) {
        eventHandlers.forEach { it.onVoiceServerUpdate(event) }
    }

    /* Message events */

    @RabbitHandler
    fun receive(event: MessageReceivedEvent) {
        // Before execution set some variables that can help with finding traces that belong to each other
        MDC.putCloseable(SentryConfiguration.SENTRY_MDC_TAG_GUILD, event.guild.toString()).use {
            MDC.putCloseable(SentryConfiguration.SENTRY_MDC_TAG_CHANNEL, event.channel.toString()).use {
                MDC.putCloseable(SentryConfiguration.SENTRY_MDC_TAG_INVOKER, event.author.toString()).use {
                    eventHandlers.forEach { it.onGuildMessage(event) }
                }
            }
        }
    }

    @RabbitHandler
    fun receive(event: PrivateMessageReceivedEvent) {
        val author = User(event.author)

        // Before execution set some variables that can help with finding traces that belong to each other
        MDC.putCloseable(SentryConfiguration.SENTRY_MDC_TAG_GUILD, "PRIVATE").use {
            MDC.putCloseable(SentryConfiguration.SENTRY_MDC_TAG_INVOKER, author.id.toString()).use {
                eventHandlers.forEach { it.onPrivateMessage(author, event.content) }
            }
        }
    }

    @RabbitHandler
    fun receive(event: MessageDeleteEvent) {
        eventHandlers.forEach { it.onGuildMessageDelete(
                event.guild,
                event.channel,
                event.id
        ) }
    }

    /* Session */

    @RabbitHandler fun appendSession(event: AppendSessionEvent) = sentinelSessionController.appendSession(event)
    @RabbitHandler fun removeSession(event: RemoveSessionEvent) = sentinelSessionController.removeSession(event)

    /* Updates */

    @RabbitHandler
    fun guildUpdate(event: GuildUpdateEvent) {
        val cached = guildCache.getIfCached(event.guild.id) ?: return
        (cached as InternalGuild).update(event.guild)
    }

    @RabbitHandler
    fun updateMember(event: GuildMemberUpdate) {
        val member = guildCache.getIfCached(event.guild)?.getMember(event.member.id) ?: return
        (member as InternalMember).update(event.member)
    }

    @RabbitHandler
    fun updateRole(event: RoleUpdate) {
        val channel = guildCache.getIfCached(event.guild)?.getRole(event.role.id) ?: return
        (channel as InternalRole).update(event.role)
    }

    @RabbitHandler
    fun updateTextChannel(event: TextChannelUpdate) {
        val channel = guildCache.getIfCached(event.guild)?.getTextChannel(event.channel.id) ?: return
        (channel as InternalTextChannel).update(event.channel)
    }

    @RabbitHandler
    fun updateVoiceChannel(event: VoiceChannelUpdate) {
        val channel = guildCache.getIfCached(event.guild)?.getVoiceChannel(event.channel.id) ?: return
        (channel as InternalVoiceChannel).update(event.channel)
    }

    @RabbitHandler
    fun handleMemberAdd(event: GuildMemberJoinEvent) {
        (guildCache.getIfCached(event.guild) as? InternalGuild)?.handleMemberAdd(event.member)
    }

    @RabbitHandler
    fun handleMemberRemove(event: GuildMemberLeaveEvent) {
        val guild = (guildCache.getIfCached(event.guild) as? InternalGuild) ?: return
        guild.handleMemberRemove(event.member)
        guild.removeMemberFromAllVoiceChannels(event.member)
    }

    @RabbitHandler
    fun handlePermissionsUpdate(event: ChannelPermissionsUpdate) {
        (guildCache.getIfCached(event.guild) as? InternalGuild)?.handlePermissionsUpdate(event)
    }

}