/*
 *
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
 */

package fredboat.command.config

import com.fredboat.sentinel.entities.coloredEmbed
import com.fredboat.sentinel.entities.field
import fredboat.command.info.HelpCommand
import fredboat.commandmeta.CommandInitializer
import fredboat.commandmeta.CommandRegistry
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.IConfigCommand
import fredboat.db.api.GuildSettingsRepository
import fredboat.definitions.Module
import fredboat.definitions.PermissionLevel
import fredboat.messaging.internal.Context
import fredboat.perms.PermsUtil
import fredboat.util.AsciiArtConstant.MAGICAL_LENNY
import fredboat.util.Emojis
import kotlinx.coroutines.reactive.awaitSingle

/**
 * Created by napster on 09.11.17.
 *
 * Turn modules on and off
 */
class ModulesCommand(name: String, private val repo: GuildSettingsRepository, vararg aliases: String) : Command(name, *aliases), IConfigCommand {

    companion object {
        private const val ENABLE = "enable"
        private const val DISABLE = "disable"
    }

    override suspend fun invoke(context: CommandContext) {

        if (!context.hasArguments()) {
            displayModuleStatus(context)
            return
        }
        if (!PermsUtil.checkPermsWithFeedback(PermissionLevel.ADMIN, context)) return

        //editing module status happens here

        val args = context.rawArgs.toLowerCase()

        val enable: Boolean
        enable = when {
            args.contains("enable") -> true
            args.contains("disable") -> false
            else -> {
                HelpCommand.sendFormattedCommandHelp(context)
                return
            }
        }

        val module = CommandRegistry.whichModule(args, context)
        if (module == null) {
            context.reply(context.i18nFormat("moduleCantParse",
                    context.prefix + context.command.name))
            return
        } else if (module.isLockedModule) {
            context.reply(context.i18nFormat("moduleLocked", context.i18n(module.translationKey))
                    + "\n" + MAGICAL_LENNY)
            return
        }

        repo.fetch(context.guild.id).doOnSuccess {
            it.get(module).enabled = enable

            if (enable) {
                context.reply((context.i18nFormat("moduleEnable", "**${context.i18n(module.translationKey)}**")
                        + "\n" + context.i18nFormat("moduleShowCommands",
                        "`" + context.prefix + CommandInitializer.COMMANDS_COMM_NAME
                                + " " + context.i18n(module.translationKey) + "`")))
            } else {
                context.reply(context.i18nFormat("moduleDisable", "**${context.i18n(module.translationKey)}**"))
            }

        }.let { repo.update(it) }.subscribe()
    }

    private suspend fun displayModuleStatus(context: CommandContext) {
        val settings = repo.fetch(context.guild.id).awaitSingle()
        val moduleStatusFormatter = { module: Module ->
            val goodOrBad = if (settings.get(module).enabled) Emojis.OK else Emojis.BAD
            goodOrBad + module.emoji + " " + context.i18n(module.translationKey)
        }
        var moduleStatus = ""

        if (PermsUtil.checkPerms(PermissionLevel.BOT_ADMIN, context.member)) {
            moduleStatus = (moduleStatusFormatter(Module.ADMIN) + " " + Emojis.LOCK + "\n"
                    + moduleStatusFormatter(Module.INFO) + " " + Emojis.LOCK + "\n"
                    + moduleStatusFormatter(Module.CONFIG) + " " + Emojis.LOCK + "\n")
        }

        moduleStatus += (moduleStatusFormatter(Module.MUSIC) + " " + Emojis.LOCK + "\n"
                + moduleStatusFormatter(Module.MOD) + "\n"
                + moduleStatusFormatter(Module.UTIL) + "\n"
                + moduleStatusFormatter(Module.FUN) + "\n")

        val howto = "`" + context.prefix + CommandInitializer.MODULES_COMM_NAME + " " + ENABLE + "/" + DISABLE + " <module>`"
        context.reply(coloredEmbed {
            field {
                title = context.i18n("moduleStatus")
                body = moduleStatus
            }
            field {
                body = context.i18nFormat("modulesHowTo", howto)
            }
        })
    }

    override fun help(context: Context): String {
        val usage = "{0}{1} OR {0}{1} enable/disable <module>\n#"
        return usage + context.i18n("helpModules")
    }
}
