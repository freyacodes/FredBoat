package fredboat.command.music.control

import com.fredboat.sentinel.entities.coloredEmbed
import com.fredboat.sentinel.entities.field
import fredboat.audio.player.GuildPlayer
import fredboat.command.info.HelpCommand
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.ICommandRestricted
import fredboat.commandmeta.abs.IMusicCommand
import fredboat.definitions.PermissionLevel
import fredboat.main.Launcher
import fredboat.messaging.internal.Context
import fredboat.sentinel.Guild
import fredboat.sentinel.Member
import fredboat.util.TextUtils
import java.util.*

class VoteShuffleCommand(name: String, vararg aliases: String, private val isUnvote: Boolean = false) : Command(name, *aliases), IMusicCommand, ICommandRestricted {

    override val minimumPerms: PermissionLevel
        get() = PermissionLevel.USER

    override suspend fun invoke(context: CommandContext) {

        // No point to allow voteshuffle if you are not in the vc at all
        // as votes only count as long are you are in the vc
        // While you can join another vc and then voteshuffle i don't think this will be common
        if (context.member.voiceChannel == null) {
            context.reply(context.i18n("playerUserNotInChannel"))
            return
        }
        val player = Launcher.botController.playerRegistry.getExisting(context.guild)
        if (player == null || player.isQueueEmpty) {
            context.reply(context.i18n("playQueueEmpty"))
            return
        }

        if (isOnCooldown(context.guild)) {
            return
        } else {
            guildIdToLastShuffle[context.guild.id] = System.currentTimeMillis()
        }

        if (!context.hasArguments()) {
            val response = if (isUnvote) removeVoteWithResponse(context) else addVoteWithResponse(context)

            val actualMinSkip = if (player.humanUsersInCurrentVC.size < 3) 1.0f else MIN_SHUFFLE_PERCENTAGE
            val skipPercentage = getShufflePercentage(context.guild, player)
            if (skipPercentage >= actualMinSkip) {
                if(!player.isQueueEmpty){
                    val shufflePerc = "`" + TextUtils.formatPercent(skipPercentage.toDouble()) + "`"
                    context.reply(response + "\n" + context.i18nFormat("voteShuffleShuffling", shufflePerc))
                    player.reshuffle()
                    guildShuffleVote.remove(player.guildId)
                }else{
                    context.reply(context.i18n("playQueueEmpty"))
                }
            } else {
                val shufflePerc = "`" + TextUtils.formatPercent(skipPercentage.toDouble()) + "`"
                val minShufflePerc = "`" + TextUtils.formatPercent(actualMinSkip.toDouble()) + "`"
                context.reply(response + "\n" + context.i18nFormat("voteShuffleNotEnough", shufflePerc, minShufflePerc))
            }

        } else if (context.args[0].toLowerCase() == "list") {
            displayVoteList(context, player)
        } else {
            HelpCommand.sendFormattedCommandHelp(context)
        }
    }

    private fun isOnCooldown(guild: Guild): Boolean {
        val currentTIme = System.currentTimeMillis()
        return currentTIme - guildIdToLastShuffle.getOrDefault(guild.id, 0L) <= SHUFFLE_COOLDOWN
    }

    private fun addVoteWithResponse(context: CommandContext): String {

        val user = context.user
        var voters: MutableSet<Long>? = guildShuffleVote[context.guild.id]?.toMutableSet()

        if (voters == null) {
            voters = HashSet()
            voters.add(user.id)
            guildShuffleVote[context.guild.id] = voters
            return context.i18n("voteShuffleAdded")
        }

        return if (voters.contains(user.id)) {
            context.i18n("voteShuffleAlreadyVoted")
        } else {
            voters.add(user.id)
            guildShuffleVote[context.guild.id] = voters
            context.i18n("voteShuffleAdded")
        }
    }

    private fun removeVoteWithResponse(context: CommandContext): String {
        val user = context.user
        var voters: MutableSet<Long>? = guildShuffleVote[context.guild.id]?.toMutableSet()

        if (voters == null) {
            voters = HashSet()
            return context.i18n("voteShuffleNotFound")
        }
        return if (voters.contains(user.id)) {
            voters.remove(user.id)
            guildShuffleVote[context.guild.id] = voters
            context.i18n("voteShuffleRemoved")
        } else {
            context.i18n("voteShuffleNotFound")
        }
    }

    private fun getShufflePercentage(guild: Guild, player: GuildPlayer): Float {
        val vcMembers = player.humanUsersInCurrentVC
        var votes = 0

        for (vcMember in vcMembers) {
            if (hasVoted(guild, vcMember)) {
                votes++
            }
        }
        val percentage = votes * 1.0f / vcMembers.size

        return if (java.lang.Float.isNaN(percentage)) {
            0f
        } else {
            percentage
        }

    }

    private fun hasVoted(guild: Guild, member: Member): Boolean {
        val voters = guildShuffleVote[guild.id] ?: return false
        return voters.contains(member.user.id)
    }

    private fun displayVoteList(context: CommandContext, player: GuildPlayer) {
        val voters = guildShuffleVote[context.guild.id]

        if (voters == null || voters.isEmpty()) {
            context.reply(context.i18n("voteShuffleEmbedNoVotes"))
            return
        }

        //split them up into two fields which makes the info look nicely condensed in the client
        val field1 = StringBuilder()
        val field2 = StringBuilder()
        for ((i, userId) in voters.withIndex()) {
            var field = field1
            if (i % 2 == 1) field = field2

            val member = context.guild.getMember(userId)
            if (member != null) {
                field.append("| ").append(TextUtils.escapeAndDefuse(member.effectiveName)).append("\n")
            }
        }
        context.reply(coloredEmbed {
            title = context.i18nFormat("voteShuffleEmbedVoters", voters.size, player.humanUsersInCurrentVC.size)
            field("", field1.toString(), true)
            field("", field2.toString(), true)
        })
    }

    override fun help(context: Context): String {
        return if (isUnvote) {
            "{0}{1}\n#" + context.i18n("helpUnvoteShuffle")
        } else {
            "{0}{1} OR {0}{1} list\n#" + context.i18n("helpVoteShuffle")
        }
    }

    companion object {

        private val guildIdToLastShuffle = HashMap<Long, Long>()
        private const val SHUFFLE_COOLDOWN = 500

        var guildShuffleVote: MutableMap<Long, Set<Long>> = HashMap()
        private const val MIN_SHUFFLE_PERCENTAGE = 0.5f
    }
}
