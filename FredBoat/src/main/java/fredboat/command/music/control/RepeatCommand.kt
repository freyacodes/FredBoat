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

package fredboat.command.music.control

import fredboat.command.info.HelpCommand
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.ICommandRestricted
import fredboat.commandmeta.abs.IMusicCommand
import fredboat.definitions.PermissionLevel
import fredboat.definitions.RepeatMode
import fredboat.main.getBotController
import fredboat.messaging.internal.Context

class RepeatCommand(name: String, vararg aliases: String) : Command(name, *aliases), IMusicCommand, ICommandRestricted {

    override val minimumPerms: PermissionLevel
        get() = PermissionLevel.DJ

    override suspend fun invoke(context: CommandContext) {
        if (!context.hasArguments()) {
            HelpCommand.sendFormattedCommandHelp(context)
            return
        }

        val desiredRepeatMode: RepeatMode
        val userInput = context.args[0]
        desiredRepeatMode = when (userInput) {
            "off", "out" -> RepeatMode.OFF
            "single", "one", "track" -> RepeatMode.SINGLE
            "all", "list", "queue" -> RepeatMode.ALL
            "help" -> {
                HelpCommand.sendFormattedCommandHelp(context)
                return
            }
            else -> {
                HelpCommand.sendFormattedCommandHelp(context)
                return
            }
        }

        getBotController().playerRegistry.awaitPlayer(context.guild).repeatMode = desiredRepeatMode

        when (desiredRepeatMode) {
            RepeatMode.OFF -> context.reply(context.i18n("repeatOff"))
            RepeatMode.SINGLE -> context.reply(context.i18n("repeatOnSingle"))
            RepeatMode.ALL -> context.reply(context.i18n("repeatOnAll"))
        }
    }

    override fun help(context: Context): String {
        val usage = "{0}{1} single|all|off\n#"
        return usage + context.i18n("helpRepeatCommand")
    }
}
