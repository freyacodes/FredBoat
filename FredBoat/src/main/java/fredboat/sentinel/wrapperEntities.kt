package fredboat.sentinel

import com.fredboat.sentinel.entities.*
import fredboat.audio.lavalink.SentinelLavalink
import fredboat.audio.lavalink.SentinelLink
import fredboat.audio.player.GuildPlayer
import fredboat.audio.player.PlayerRegistry
import fredboat.config.property.AppConfig
import fredboat.main.getBotController
import fredboat.perms.IPermissionSet
import fredboat.perms.NO_PERMISSIONS
import fredboat.perms.Permission
import fredboat.perms.PermissionSet
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.AmqpRejectAndDontRequeueException
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import java.util.stream.Stream
import kotlin.streams.toList

typealias RawGuild = com.fredboat.sentinel.entities.Guild
typealias RawMember = com.fredboat.sentinel.entities.Member
typealias RawUser = com.fredboat.sentinel.entities.User
typealias RawTextChannel = com.fredboat.sentinel.entities.TextChannel
typealias RawVoiceChannel = com.fredboat.sentinel.entities.VoiceChannel
typealias RawRole = com.fredboat.sentinel.entities.Role

private val MEMBER_MENTION_PATTERN = Pattern.compile("<@!?([0-9]+)>", Pattern.DOTALL)
private val CHANNEL_MENTION_PATTERN = Pattern.compile("<#([0-9]+)>", Pattern.DOTALL)

@Service
class WrapperEntityBeans(appConfigParam: AppConfig, lavalinkParam: SentinelLavalink, playerRegistryParam: PlayerRegistry) {
    init {
        appConfig = appConfigParam
        lavalink = lavalinkParam
        playerRegistry = playerRegistryParam
    }
}

private val log: Logger = LoggerFactory.getLogger("wrapperEntities")
private lateinit var appConfig: AppConfig
private lateinit var lavalink: SentinelLavalink
private lateinit var playerRegistry: PlayerRegistry

@Suppress("PropertyName")
abstract class Guild(raw: RawGuild) : SentinelEntity {

    override val id = raw.id

    protected lateinit var _name: String
    val name: String get() = _name

    protected var _owner: Member? = null // Discord has a history of null owners
    val owner: Member? get() = _owner

    protected var _members = ConcurrentHashMap<Long, InternalMember>()
    val members: Map<Long, Member> get() = _members

    protected var _roles = ConcurrentHashMap<Long, Role>()
    val roles: Map<Long, Role> get() = _roles

    protected var _textChannels = ConcurrentHashMap<Long, TextChannel>()
    val textChannels: Map<Long, TextChannel> get() = _textChannels

    protected var _voiceChannels = ConcurrentHashMap<Long, VoiceChannel>()
    val voiceChannels: Map<Long, VoiceChannel> get() = _voiceChannels

    protected var _stale = false
    /** This is true if we are present in this [Guild]*/
    val selfPresent: Boolean
        get() = !_stale

    /* Helper properties */

    val selfMember: Member
        get() = _members[sentinel.selfUser.id] ?: throw AmqpRejectAndDontRequeueException("Unable to find self in guild")
    val shardId: Int
        get() = ((id shr 22) % appConfig.shardCount.toLong()).toInt()
    val shardString: String
        get() = "[$shardId/${appConfig.shardCount}]"
    val link: SentinelLink
        get() = lavalink.getLink(this)
    val existingLink: SentinelLink?
        get() = lavalink.getExistingLink(this)
    val info: Mono<GuildInfo>
        get() = sentinel.getGuildInfo(this)

    /** The routing key for the associated Sentinel */
    val routingKey: String
        get() = sentinel.tracker.getKey(shardId)

    val guildPlayer: GuildPlayer? get() =  playerRegistry.getExisting(this)
    fun getOrCreateGuildPlayer() = playerRegistry.getOrCreate(this)

    fun getMember(id: Long): Member? = _members[id]
    fun getRole(id: Long): Role? = _roles[id]
    fun getTextChannel(id: Long): TextChannel? = _textChannels[id]
    fun getVoiceChannel(id: Long): VoiceChannel? = _voiceChannels[id]
    fun isMember(user: User) = members.containsKey(user.id)
    override fun equals(other: Any?): Boolean = other is Guild && id == other.id
    override fun hashCode() = id.hashCode()
    override fun toString() = "[G:$name:$id]"
}

/** Has public members we want to hide */
class InternalGuild(raw: RawGuild) : Guild(raw) {

    /** Last time we really needed this [Guild].
     *  If this value becomes too old, the [Guild] may be invalidated.
     *  Refreshed on command invocation
     */
    var lastUsed: Long = System.currentTimeMillis()

    /**
     * Sentinel caches voice server updates, so that we may restart without renewing it.
     * This property may store the voice server update that Sentinel has at the time of subscribing/updating.
     */
    var cachedVsu: VoiceServerUpdate? = null

    init {
        update(raw)
        cachedVsu = raw.voiceServerUpdate // Must only be set on subscribing, unless setting to null
        try {
            // Any old GuildPlayer needs to be aware of the new guild object
            val player: GuildPlayer? = getBotController().playerRegistry.getExisting(this)
            if (player != null) {
                player.guild = this
                player.linkPostProcess()
            }
        } catch (e: Exception) {
            log.error("Failed to update GuildPlayer after subscribe", e)
        }

        if (raw.voiceServerUpdate != null) log.info("Received cached VSU for $this")
    }

    fun update(raw: RawGuild) {
        if (id != raw.id) throw AmqpRejectAndDontRequeueException("Attempt to update $id with the data of ${raw.id}")

        _name = raw.name

        // Note: Roles must be loaded first as members rely on them. Then members, then channels
        _roles = raw.roles.map { InternalRole(this, it) }.associateByTo(ConcurrentHashMap()) { it.id }
        _members = raw.members.map { InternalMember(this, it) }.associateByTo(ConcurrentHashMap()) { it.id }
        _textChannels = raw.textChannels.map { InternalTextChannel(this, it) }.associateByTo(ConcurrentHashMap()) { it.id }
        _voiceChannels = raw.voiceChannels.map { InternalVoiceChannel(this, it) }.associateByTo(ConcurrentHashMap()) { it.id }

        val rawOwner = raw.owner
        _owner = if (rawOwner != null) members[rawOwner] else null

    }

    fun handleMemberAdd(member: RawMember) {
        _members[member.id] = InternalMember(this, member)
    }

    fun handleMemberRemove(id: Long) {
        _members.remove(id)
    }

    fun removeMemberFromAllVoiceChannels(memberId: Long) {
        _voiceChannels.forEach {
            (it.value as InternalVoiceChannel).removeMember(memberId)
        }
    }

    fun onSelfLeaving() {
        _stale = true
    }

    fun handlePermissionsUpdate(update: ChannelPermissionsUpdate) {
        update.changes.forEach { id, perms ->
            val idL = id.toLong()
            val channel: Channel? = getTextChannel(idL) ?: getVoiceChannel(idL)

            if (channel == null) {
                log.warn("Got permission update for unknown guild $id")
                return@forEach
            }

            when (channel) {
                is InternalTextChannel -> channel.updatePerms(perms)
                is InternalVoiceChannel -> channel.updatePerms(perms)
                else -> throw IllegalArgumentException()
            }
        }
    }

}

@Suppress("PropertyName")
abstract class Member(val guild: Guild, raw: RawMember) : IMentionable, SentinelEntity {
    override val id = raw.id
    val isBot = raw.bot

    protected lateinit var _name: String
    val name: String get() = _name

    protected lateinit var _discrim: String
    val discrim: String get() = _discrim

    protected var _nickname: String? = null
    val nickname: String? get() = _nickname

    val voiceChannel: VoiceChannel? get() = guild.voiceChannels.values.find { it.members.contains(this) }

    protected var _roles = mutableListOf<Role>()
    val roles: List<Role> get() = _roles // Cast to immutable

    /* Convenience properties */
    val effectiveName: String get() = if (_nickname != null) _nickname!! else _name
    /** True if this [Member] is our bot */
    val isUs: Boolean get() = id == sentinel.selfUser.id
    override val asMention: String get() = "<@$id>"
    val user: User
        get() = User(RawUser(
                id,
                name,
                discrim,
                isBot
        ))
    val info: Mono<MemberInfo> get() = sentinel.getMemberInfo(this)
    val raw: RawMember get() =
        RawMember(id, name, nickname ?: "", discrim, guild.id, isBot, roles.map { id }, voiceChannel?.id)

    fun isOwner() = this == guild.owner

    fun getPermissions(channel: Channel? = null): Mono<PermissionSet> {
        if (isOwner()) return PermissionSet(-1).toMono() // Owner perms are implied. -1 is all ones in two's compliement
        return when (channel) {
            null -> sentinel.checkPermissions(this, NO_PERMISSIONS)
                    .map { PermissionSet(it.effective) }
            else -> sentinel.checkPermissions(channel, this, NO_PERMISSIONS)
                    .map { PermissionSet(it.effective) }
        }
    }

    fun hasPermission(permissions: IPermissionSet, channel: Channel? = null): Mono<Boolean> {
        if (isOwner()) return true.toMono() // Owner perms are implied
        return when (channel) {
            null -> sentinel.checkPermissions(this, permissions)
                    .map { it.passed }
            else -> sentinel.checkPermissions(channel, this, permissions)
                    .map { it.passed }
        }
    }

    override fun equals(other: Any?): Boolean = other is Member && id == other.id
    override fun hashCode(): Int = id.hashCode()
    override fun toString() = "[$effectiveName#$discrim:$id in ${guild.id}]"

}

class InternalMember(guild: Guild, raw: RawMember) : Member(guild, raw) {

    init {
        update(raw)
    }

    fun update(raw: RawMember) {
        if (id != raw.id) throw AmqpRejectAndDontRequeueException("Attempt to update $id with the data of ${raw.id}")

        val newRoleList = mutableListOf<Role>()
        raw.roles.flatMapTo(newRoleList) {
            val role = guild.getRole(it)
            return@flatMapTo if (role != null) listOf(role) else emptyList()
        }
        _roles = newRoleList
        _name = raw.name
        _discrim = raw.discrim
        _nickname = raw.nickname
    }
}

/** Note: This is not cached or subject to updates */
class User(@Suppress("MemberVisibilityCanBePrivate") val raw: RawUser) : IMentionable, SentinelEntity {
    override val id: Long
        get() = raw.id
    val name: String
        get() = raw.name
    val discrim: String
        get() = raw.discrim
    val isBot: Boolean
        get() = raw.bot
    override val asMention: String
        get() = "<@$id>"

    fun sendPrivate(message: String) = sentinel.sendPrivateMessage(this, message)

    override fun equals(other: Any?): Boolean {
        return other is User && id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString() = "[U:$name:$id]"
}

@Suppress("PropertyName")
abstract class TextChannel(override val guild: Guild, raw: RawTextChannel) : Channel, IMentionable {
    override val id = raw.id

    protected lateinit var _name: String
    override val name: String get() = _name

    protected var _ourEffectivePermissions: Long = raw.ourEffectivePermissions
    override val ourEffectivePermissions: IPermissionSet get() = PermissionSet(_ourEffectivePermissions)

    override val asMention: String get() = "<#$id>"

    fun send(str: String): Mono<SendMessageResponse> = sentinel.sendMessage(guild.routingKey, this, str)
    fun send(embed: Embed): Mono<SendMessageResponse> = sentinel.sendMessage(guild.routingKey, this, embed)
    fun editMessage(messageId: Long, message: String) = sentinel.editMessage(this, messageId, message)
    fun deleteMessage(messageId: Long) = sentinel.deleteMessages(this, listOf(messageId))
    fun sendTyping() = sentinel.sendTyping(this)
    fun canTalk() = checkOurPermissions(Permission.MESSAGE_READ + Permission.MESSAGE_WRITE)


    override fun equals(other: Any?) = other is TextChannel && id == other.id
    override fun hashCode() = id.hashCode()
    override fun toString() = "[TC:$name:$id]"
}

class InternalTextChannel(override val guild: Guild, raw: RawTextChannel) : TextChannel(guild, raw) {

    init {
        update(raw)
    }

    fun update(raw: RawTextChannel) {
        if (id != raw.id) throw AmqpRejectAndDontRequeueException("Attempt to update $id with the data of ${raw.id}")

        _name = raw.name
        _ourEffectivePermissions = raw.ourEffectivePermissions
    }

    fun updatePerms(perms: Long) { _ourEffectivePermissions = perms }

}

@Suppress("PropertyName")
abstract class VoiceChannel(override val guild: Guild, raw: RawVoiceChannel) : Channel {
    override val id = raw.id

    protected lateinit var _name: String
    override val name: String get() = _name

    protected var _ourEffectivePermissions: Long = raw.ourEffectivePermissions
    override val ourEffectivePermissions: IPermissionSet get() = PermissionSet(_ourEffectivePermissions)

    protected var _userLimit = 0
    val userLimit: Int get() = _userLimit

    protected lateinit var _members: MutableList<Member>
    val members: List<Member> get() = _members

    fun connect() = SentinelLavalink.INSTANCE.getLink(guild).connect(this)
    override fun equals(other: Any?) = other is VoiceChannel && id == other.id
    override fun hashCode() = id.hashCode()
    override fun toString() = "[VC:$name:$id]"
}

class InternalVoiceChannel(override val guild: Guild, raw: RawVoiceChannel) : VoiceChannel(guild, raw) {

    init {
        update(raw)
    }

    fun update(raw: RawVoiceChannel) {
        if (id != raw.id) throw AmqpRejectAndDontRequeueException("Attempt to update $id with the data of ${raw.id}")

        _name = raw.name
        _ourEffectivePermissions = raw.ourEffectivePermissions
        _userLimit = raw.userLimit
        _members = raw.members.flatMapTo(Collections.synchronizedList(mutableListOf())) { id ->
            listOfNotNull(guild.getMember(id))
        }
    }

    /** Also handles moves */
    fun handleVoiceJoin(member: Member) {
        (guild as InternalGuild).removeMemberFromAllVoiceChannels(member.id) // For good measure
        _members.add(member)
    }

    fun removeMember(memberId: Long) {
        _members.removeIf { it.id == memberId }
    }

    fun updatePerms(perms: Long) { _ourEffectivePermissions = perms }
}

@Suppress("PropertyName")
abstract class Role(open val guild: Guild, raw: RawRole) : IMentionable, SentinelEntity {
    override val id: Long  = raw.id

    protected lateinit var _name: String
    val name: String get() = _name

    protected var _permissions = raw.permissions
    val permissions: PermissionSet get() = PermissionSet(_permissions)

    /** The @everyone role shares the ID of the guild */
    val isPublicRole: Boolean get() = id == guild.id
    override val asMention: String get() = "<@$id>"
    val info: Mono<RoleInfo> get() = sentinel.getRoleInfo(this)

    override fun equals(other: Any?) = other is Role && id == other.id
    override fun hashCode() = id.hashCode()
    override fun toString() = "[R:$name:$id]"
}

class InternalRole(override val guild: Guild, raw: RawRole) : Role(guild, raw) {
    init {
        update(raw)
    }

    fun update(raw: RawRole) {
        if (id != raw.id) throw AmqpRejectAndDontRequeueException("Attempt to update $id with the data of ${raw.id}")

        _name = raw.name
        _permissions = raw.permissions
    }
}

class Message(val guild: Guild, @Suppress("MemberVisibilityCanBePrivate") val raw: MessageReceivedEvent) : SentinelEntity {
    override val id: Long
        get() = raw.id
    val content: String
        get() = raw.content
    val member: Member // Maybe make this nullable?
        get() = guild.getMember(raw.author)!!
    val channel: TextChannel // Maybe make this nullable?
        get() = guild.getTextChannel(raw.channel)!!
    val mentionedMembers: List<Member>
    // Technically one could mention someone who isn't a member of the guild,
    // but we don't really care for that
        get() = MEMBER_MENTION_PATTERN.matcher(content)
                .results()
                .flatMap<Member> {
                    Stream.ofNullable(guild.getMember(it.group(1).toLong()))
                }
                .toList()
    val mentionedChannels: List<TextChannel>
        get() = CHANNEL_MENTION_PATTERN.matcher(content)
                .results()
                .flatMap<TextChannel> {
                    Stream.ofNullable(guild.getTextChannel(it.group(1).toLong()))
                }
                .toList()
    val attachments: List<String>
        get() = raw.attachments

    fun delete(): Mono<Unit> = sentinel.deleteMessages(channel, listOf(id))
    override fun toString() = "[${member.effectiveName} -> M:$id]"
}