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

package fredboat.command.music.seeking

import fredboat.audio.player.isQueueEmpty
import fredboat.command.info.HelpCommand
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.ICommandRestricted
import fredboat.commandmeta.abs.IMusicCommand
import fredboat.definitions.PermissionLevel
import fredboat.main.getBotController
import fredboat.messaging.internal.Context
import fredboat.util.TextUtils

class RewindCommand(name: String, vararg aliases: String) : Command(name, *aliases), IMusicCommand, ICommandRestricted {

    override val minimumPerms: PermissionLevel
        get() = PermissionLevel.DJ

    override suspend fun invoke(context: CommandContext) {
        val player = getBotController().playerRegistry.getExisting(context.guild)

        if (player == null || player.isQueueEmpty) {
            context.replyWithName(context.i18n("queueEmpty"))
            return
        }

        if (!context.hasArguments()) {
            HelpCommand.sendFormattedCommandHelp(context)
            return
        }

        var t: Long
        try {
            t = TextUtils.parseTimeString(context.args[0])
        } catch (e: IllegalStateException) {
            HelpCommand.sendFormattedCommandHelp(context)
            return
        }

        val currentPosition = player.position
        //Ensure bounds
        t = Math.max(0, t)
        t = Math.min(currentPosition, t)

        player.seekTo(currentPosition - t)
        context.reply(context.i18nFormat("rewSuccess",
                TextUtils.escapeAndDefuse(player.playingTrack!!.effectiveTitle), TextUtils.formatTime(t)))
    }

    override fun help(context: Context): String {
        val usage = "{0}{1} [[hh:]mm:]ss\n#"
        val example = " {0}{1} 30"
        return usage + context.i18n("helpRewindCommand") + example
    }

}
