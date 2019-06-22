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

import fredboat.command.info.HelpCommand
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.ICommandRestricted
import fredboat.commandmeta.abs.IConfigCommand
import fredboat.db.api.GuildSettingsRepository
import fredboat.db.transfer.GuildSettings
import fredboat.definitions.PermissionLevel
import fredboat.messaging.internal.Context
import fredboat.perms.PermsUtil
import fredboat.util.TextUtils
import fredboat.util.localMessageBuilder
import org.apache.commons.lang3.StringUtils
import java.util.function.Predicate

private typealias Validator = Predicate<String>

class ConfigCommand(name: String, private val repo: GuildSettingsRepository, vararg aliases: String) : Command(name, *aliases), IConfigCommand, ICommandRestricted {

    override val minimumPerms: PermissionLevel
        get() = PermissionLevel.BASE

    private val configOptions: MutableList<ConfigOption> = mutableListOf()

    init {
        val booleanPredicate = Predicate<String> { it.equals("true", true) || it.equals("false", true) }
        val nullableIntPredicate = Predicate<String> { (it.toIntOrNull() != null && it.toInt() > -1) || StringUtils.equalsAnyIgnoreCase(it, "none", "unlimited") }
        val nullableLongPredicate = Predicate<String> { StringUtils.equalsAnyIgnoreCase(it, "none", "unlimited") || TextUtils.parseTimeString(it) != 0L }

        configOptions.add(ConfigOption(
                "track_announce",
                booleanPredicate,
                { it.trackAnnounce.toString() },
                { gs, value -> gs.trackAnnounce = value.toBoolean() }))

        configOptions.add(ConfigOption(
                "auto_resume",
                booleanPredicate,
                { it.trackAnnounce.toString() },
                { gs, value -> gs.trackAnnounce = value.toBoolean() }))

        configOptions.add(ConfigOption(
                "allow_playlist",
                booleanPredicate,
                { it.allowPlaylist.toString() },
                { gs, v -> gs.allowPlaylist = v.toBoolean() }))

        configOptions.add(ConfigOption(
                "max_tracks",
                nullableIntPredicate,
                { it.maxTrackCount?.toString() ?: "UNLIMITED" },
                { gs, v -> gs.maxTrackCount = if (v.equals("unlimited", true)) null else v.toInt() }))

        configOptions.add(ConfigOption(
                "max_user_tracks",
                nullableIntPredicate,
                { it.userMaxTrackCount?.toString() ?: "UNLIMITED" },
                { gs, v ->
                    gs.userMaxTrackCount = if (StringUtils.equalsAnyIgnoreCase(v, "none", "unlimited"))
                        null
                    else
                        v.toInt()
                }))

        configOptions.add(ConfigOption(
                "max_track_length",
                nullableLongPredicate,
                { if (it.maxTrackLength != null) TextUtils.formatTime(it.maxTrackLength!!) else "UNLIMITED" },
                { gs, v ->
                    gs.maxTrackLength = if (StringUtils.equalsAnyIgnoreCase(v, "none", "unlimited"))
                        null
                    else
                        TextUtils.parseTimeString(v)
                }))
    }

    override suspend fun invoke(context: CommandContext) {
        if (!context.hasArguments()) {
            printConfig(context)
        } else {
            setConfig(context)
        }
    }

    private fun printConfig(context: CommandContext) {
        repo.fetch(context.guild.id).subscribe {
            val mb = localMessageBuilder().append(context.i18nFormat("configNoArgs", context.guild.name)).append("\n")

            for (config in configOptions) {
                mb.append("${config.name} = ").append(config.getter(it)).append("\n")
            }

            context.reply(mb.append("```").build())
        }
    }

    private suspend fun setConfig(context: CommandContext) {
        if (!PermsUtil.checkPermsWithFeedback(PermissionLevel.ADMIN, context)) {
            return
        }

        if (context.args.size != 2) {
            HelpCommand.sendFormattedCommandHelp(context)
            return
        }

        val key = context.args[0]
        val value = context.args[1]

        val config = configOptions.firstOrNull { it.name == key }
        if (config == null) {
            context.replyWithName(context.i18n("configUnknownKey"))
            return
        }

        if (!config.validator.test(value)) {
            context.replyWithName(context.i18n("configValueTypeInvalid"))
        }

        config.update(repo, context, value).subscribe {
            context.replyWithName("`$name` ${context.i18nFormat("configSetTo", value)}")
        }
    }

    override fun help(context: Context): String {
        val usage = "{0}{1} OR {0}{1} <key> <value>\n#"
        return usage + context.i18n("helpConfigCommand")
    }
}

private data class ConfigOption(
        val name: String,
        val validator: Validator,
        val getter: (GuildSettings) -> String,
        val setter: (GuildSettings, String) -> Unit
) {
    fun update(repo: GuildSettingsRepository, context: CommandContext, value: String) = repo.fetch(context.guild.id)
            .doOnSuccess { setter(it, value) }
            .let { repo.update(it) }

    override fun toString(): String {
        return name
    }
}
