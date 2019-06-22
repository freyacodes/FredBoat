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

package fredboat.command.music.info

import fredboat.audio.player.getTracksInHistory

import fredboat.audio.player.isHistoryQueueEmpty
import fredboat.audio.player.trackCountInHistory
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.IMusicCommand
import fredboat.main.getBotController
import fredboat.messaging.internal.Context
import fredboat.util.TextUtils
import fredboat.util.localMessageBuilder
import java.text.MessageFormat
class HistoryCommand(name: String, vararg aliases: String) : Command(name, *aliases), IMusicCommand {

    companion object {
        private const val PAGE_SIZE = 10
    }

    override suspend fun invoke(context: CommandContext) {
        val player = getBotController().playerRegistry.getExisting(context.guild)

        if (player == null || player.isHistoryQueueEmpty) {
            context.reply(context.i18n("npNotInHistory"))
            return
        }

        var page = 1
        if (context.hasArguments()) {
            try {
                page = Integer.valueOf(context.args[0])
            } catch (ignored: NumberFormatException) {
            }

        }

        val tracksCount = player.trackCountInHistory
        val maxPages = Math.ceil(tracksCount.toDouble() - 1.0).toInt() / PAGE_SIZE + 1

        page = Math.max(page, 1)
        page = Math.min(page, maxPages)

        var i = (page - 1) * PAGE_SIZE
        var listEnd = (page - 1) * PAGE_SIZE + PAGE_SIZE
        listEnd = Math.min(listEnd, tracksCount)

        val numberLength = Integer.toString(listEnd).length

        val sublist = player.getTracksInHistory(i, listEnd)

        val mb = localMessageBuilder()
                .append(context.i18n("listShowHistory"))
                .append("\n")
                .append(MessageFormat.format(context.i18n("listPageNum"), page, maxPages))
                .append("\n")
                .append("\n")

        for (atc in sublist) {
            val status = " "

            val member = atc.member
            val username = member.effectiveName
            mb.code("[" +
                    TextUtils.forceNDigits(i + 1, numberLength)
                    + "]")
                    .append(status)
                    .append(context.i18nFormat("listAddedBy", TextUtils.escapeAndDefuse(atc.effectiveTitle),
                            TextUtils.escapeAndDefuse(username), TextUtils.formatTime(atc.effectiveDuration)))
                    .append("\n")

            if (i == listEnd) {
                break
            }

            i++
        }

        context.reply(mb.build())
    }

    override fun help(context: Context): String {
        val usage = "{0}{1} (page)\n#"
        return usage + context.i18n("helpHistoryCommand")
    }
}