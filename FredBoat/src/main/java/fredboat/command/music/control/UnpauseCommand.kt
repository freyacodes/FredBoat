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

import fredboat.audio.player.humanUsersInCurrentVC
import fredboat.audio.player.isQueueEmpty
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.ICommandRestricted
import fredboat.commandmeta.abs.IMusicCommand
import fredboat.definitions.PermissionLevel
import fredboat.main.Launcher
import fredboat.messaging.internal.Context

class UnpauseCommand(name: String, vararg aliases: String) : Command(name, *aliases), IMusicCommand, ICommandRestricted {

    override val minimumPerms: PermissionLevel
        get() = PermissionLevel.DJ

    override suspend fun invoke(context: CommandContext) {
        val guild = context.guild
        val player = Launcher.botController.playerRegistry.getExisting(guild)
        if (player == null || player.isQueueEmpty) {
            context.reply(context.i18n("unpauseQueueEmpty"))
        } else if (!player.isPaused) {
            context.reply(context.i18n("unpausePlayerNotPaused"))
        } else if (player.humanUsersInCurrentVC.isEmpty() && player.isPaused && guild.selfMember.voiceChannel != null) {
            context.reply(context.i18n("unpauseNoUsers"))
        } else if (guild.selfMember.voiceChannel == null) {
            // When we just want to continue playing, but the user is not in a VC
            JOIN_COMMAND.invoke(context)
            if (guild.selfMember.voiceChannel != null) {
                player.play()
                context.reply(context.i18n("unpauseSuccess"))
            }
        } else {
            player.play()
            context.reply(context.i18n("unpauseSuccess"))
        }
    }

    override fun help(context: Context): String {
        return "{0}{1}\n#" + context.i18n("helpUnpauseCommand")
    }

    companion object {

        private val JOIN_COMMAND = JoinCommand("")
    }
}
