/*
 * MIT License
 *
 * Copyright (c) 2017 Frederik Ar. Mikkelsen
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
 *
 */

package fredboat.command.config

import com.fredboat.sentinel.entities.author
import com.fredboat.sentinel.entities.coloredEmbed
import com.fredboat.sentinel.entities.field
import fredboat.command.info.HelpCommand
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.IConfigCommand
import fredboat.db.api.GuildSettingsRepository
import fredboat.definitions.PermissionLevel
import fredboat.messaging.internal.Context
import fredboat.perms.Permission
import fredboat.perms.PermsUtil
import fredboat.sentinel.*
import fredboat.shared.constant.BotConstants
import fredboat.util.ArgumentUtil
import fredboat.util.TextUtils
import fredboat.util.extension.addFooter
import fredboat.util.extension.escapeAndDefuse
import fredboat.util.extension.escapeMarkdown
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import java.util.*

class PermissionsCommand(
        private val permissionLevel: PermissionLevel,
        private val repo: GuildSettingsRepository,
        name: String,
        vararg aliases: String
) : Command(name, *aliases), IConfigCommand {

    override suspend fun invoke(context: CommandContext) {
        if (!context.hasArguments()) {
            HelpCommand.sendFormattedCommandHelp(context)
            return
        }

        when (context.args[0]) {
            "del", "delete", "remove", "rem", "rm" -> {
                if (!PermsUtil.checkPermsWithFeedback(PermissionLevel.ADMIN, context)) return

                if (context.args.size < 2) {
                    HelpCommand.sendFormattedCommandHelp(context)
                    return
                }

                remove(context)
            }
            "add" -> {
                if (!PermsUtil.checkPermsWithFeedback(PermissionLevel.ADMIN, context)) return

                if (context.args.size < 2) {
                    HelpCommand.sendFormattedCommandHelp(context)
                    return
                }

                add(context)
            }
            "list", "ls" -> list(context)
            else -> HelpCommand.sendFormattedCommandHelp(context)
        }
    }

    suspend fun remove(context: CommandContext) {
        val guild = context.guild
        val invoker = context.member
        //remove the first argument aka add / remove etc to get a nice search term
        val term = context.rawArgs.replaceFirst(context.args[0].toRegex(), "").trim { it <= ' ' }

        val search = mutableListOf<IMentionable>()
        search.addAll(ArgumentUtil.fuzzyRoleSearch(guild, term))
        search.addAll(ArgumentUtil.fuzzyMemberSearch(guild, term, false))

        val selected = ArgumentUtil.checkSingleFuzzySearchResult(search, context, term) ?: return
        val discordPerms = invoker.getPermissions(channel = null).awaitFirst()

        val settings = repo.fetch(context.guild.id).awaitSingle()
        val gp = settings.permissions

        if (!gp.getForEnum(permissionLevel).contains(mentionableToId(selected))) {
            context.replyWithName(context.i18nFormat("permsNotAdded", "`" + mentionableToName(selected) + "`", "`$permissionLevel`"))
            return
        }

        val newList = gp.getForEnum(permissionLevel).toMutableList()
        newList.remove(mentionableToId(selected))

        if (permissionLevel == PermissionLevel.ADMIN
                && permissionLevel < PermissionLevel.BOT_ADMIN
                && discordPerms hasNot Permission.ADMINISTRATOR
                && !PermsUtil.checkList(newList, invoker)) {
            context.replyWithName(context.i18n("permsFailSelfDemotion"))
            return
        }

        context.replyWithName(context.i18nFormat("permsRemoved", mentionableToName(selected), permissionLevel))

        gp.setForEnum(permissionLevel, newList)
        repo.update(settings).subscribe()
    }

    suspend fun add(context: CommandContext) {
        val guild = context.guild
        //remove the first argument aka add / remove etc to get a nice search term
        val term = context.rawArgs.replaceFirst(context.args[0].toRegex(), "").trim { it <= ' ' }

        val list = ArrayList<IMentionable>()
        list.addAll(ArgumentUtil.fuzzyRoleSearch(guild, term))
        list.addAll(ArgumentUtil.fuzzyMemberSearch(guild, term, false))

        val selected = ArgumentUtil.checkSingleFuzzySearchResult(list, context, term) ?: return

        val settings = repo.fetch(context.guild.id).awaitSingle()
        val gp = settings.permissions
        if (gp.getForEnum(permissionLevel).contains(mentionableToId(selected))) {
            context.replyWithName(context.i18nFormat("permsAlreadyAdded", "`" + TextUtils.escapeMarkdown(mentionableToName(selected)) + "`", "`$permissionLevel`"))
            return
        }

        val newList = gp.getForEnum(permissionLevel).toMutableList()
        newList.add(mentionableToId(selected))

        context.replyWithName(context.i18nFormat("permsAdded", TextUtils.escapeMarkdown(mentionableToName(selected)), permissionLevel))

        gp.setForEnum(permissionLevel, newList)
        repo.update(settings).subscribe()
    }

    suspend fun list(context: CommandContext) {
        val guild = context.guild
        val invoker = context.member
        val settings = repo.fetch(context.guild.id).awaitSingle()

        val mentionables = idsToMentionables(guild, settings.permissions.getForEnum(permissionLevel))

        var roleMentions = ""
        var memberMentions = ""

        for (mentionable in mentionables) {
            if (mentionable is Role) {
                roleMentions = if (mentionable.isPublicRole) {
                    "$roleMentions@everyone\n" // Prevents ugly double double @@
                } else {
                    // Lazy guilds update breaks display of roles in embeds
                    "$roleMentions@${mentionable.name.escapeAndDefuse()}\n"
                }
            } else {
                memberMentions = memberMentions + mentionable.asMention + "\n"
            }
        }

        val invokerPerms = PermsUtil.getPerms(invoker)
        val invokerHas = PermsUtil.checkPerms(permissionLevel, invoker)

        if (roleMentions.isEmpty()) roleMentions = "<none>"
        if (memberMentions.isEmpty()) memberMentions = "<none>"

        val embed = coloredEmbed {
            title = context.i18nFormat("permsListTitle", permissionLevel)
            author {
                name = invoker.effectiveName
                iconUrl = invoker.info.awaitSingle().iconUrl
            }
            field {
                title = "Roles"
                body = roleMentions
                inline = true
            }
            field {
                title = "members"
                body = memberMentions
                inline = true
            }
            field {
                title = invoker.effectiveName.escapeMarkdown()
                body = (if (invokerHas) ":white_check_mark:" else ":x:") + " (" + invokerPerms + ")"
            }
        }.addFooter(guild.selfMember)

        context.reply(embed)
    }

    private fun mentionableToId(mentionable: IMentionable): Long {
        return if (mentionable is SentinelEntity) {
            mentionable.id
        } else {
            throw IllegalArgumentException()
        }
    }

    private fun mentionableToName(mentionable: IMentionable): String {
        return when (mentionable) {
            is Role -> mentionable.name
            is Member -> mentionable.name
            else -> throw IllegalArgumentException()
        }
    }

    private fun idsToMentionables(guild: Guild, list: List<Long>): List<IMentionable> =
            list.flatMap<Long, IMentionable> { id ->
                guild.getRole(id)?.apply {
                    return@flatMap listOf(this)
                }
                return@flatMap listOfNotNull(guild.getMember(id))
            }

    override fun help(context: Context): String {
        val usage = "{0}{1} add <role/user>\n{0}{1} del <role/user>\n{0}{1} list\n#"
        return usage + context.i18nFormat("helpPerms", permissionLevel) + "\n" + BotConstants.DOCS_PERMISSIONS_URL
    }

}
