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

import fredboat.commandmeta.MessagingException
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.ICommandRestricted
import fredboat.commandmeta.abs.IMusicCommand
import fredboat.definitions.PermissionLevel
import fredboat.main.getBotController
import fredboat.messaging.internal.Context
import fredboat.shared.constant.BotConstants
import java.time.Duration

class VolumeCommand(name: String, vararg aliases: String) : Command(name, *aliases), IMusicCommand, ICommandRestricted {

    override val minimumPerms: PermissionLevel
        get() = PermissionLevel.DJ

    override suspend fun invoke(context: CommandContext) {

        if (getBotController().appConfig.distribution.volumeSupported()) {

            val player = getBotController().playerRegistry.awaitPlayer(context.guild)
            try {
                var volume = java.lang.Float.parseFloat(context.args[0]) / 100
                volume = Math.max(0f, Math.min(1.5f, volume))

                context.reply(context.i18nFormat("volumeSuccess",
                        Math.floor((player.volume * 100).toDouble()), Math.floor((volume * 100).toDouble())))

                player.volume = volume
            } catch (ex: NumberFormatException) {
                throw MessagingException(context.i18nFormat("volumeSyntax",
                        100, Math.floor((player.volume * 100).toDouble())))
            } catch (ex: ArrayIndexOutOfBoundsException) {
                throw MessagingException(context.i18nFormat("volumeSyntax", 100, Math.floor((player.volume * 100).toDouble())))
            }

        } else {
            val out = context.i18n("volumeApology") + "\n<" + BotConstants.DOCS_DONATE_URL + ">"
            context.replyImageMono("https://fred.moe/1vD.png", out)
                    .subscribe { (messageId) ->
                        context.textChannel.deleteMessage(messageId)
                                .delaySubscription(Duration.ofMinutes(2))
                                .subscribe()
                    }
        }
    }

    override fun help(context: Context): String {
        return "{0}{1} <0-150>\n#" + context.i18n("helpVolumeCommand")
    }
}
