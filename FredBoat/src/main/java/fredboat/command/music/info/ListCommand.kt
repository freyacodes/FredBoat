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

import fredboat.audio.player.*
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.IMusicCommand
import fredboat.commandmeta.abs.JCommand
import fredboat.definitions.RepeatMode
import fredboat.main.getBotController
import fredboat.messaging.internal.Context
import fredboat.util.TextUtils
import fredboat.util.localMessageBuilder
import org.slf4j.LoggerFactory

class ListCommand(name: String, vararg aliases: String) : JCommand(name, *aliases), IMusicCommand {

    companion object {
        private val log = LoggerFactory.getLogger(ListCommand::class.java)
        private const val PAGE_SIZE = 10
    }

    override fun onInvoke(context: CommandContext) {
        val player = getBotController().playerRegistry.getExisting(context.guild)

        if (player == null || player.isQueueEmpty) {
            context.reply(context.i18n("npNotPlaying"))
            return
        }

        val mb = localMessageBuilder()

        var page = 1
        if (context.hasArguments()) {
            try {
                page = Integer.valueOf(context.args[0])
            } catch (ignored: NumberFormatException) {
            }

        }

        val tracksCount = player.trackCount
        val maxPages = Math.ceil(tracksCount.toDouble() - 1.0).toInt() / PAGE_SIZE + 1

        page = Math.max(page, 1)
        page = Math.min(page, maxPages)

        var i = (page - 1) * PAGE_SIZE
        var listEnd = (page - 1) * PAGE_SIZE + PAGE_SIZE
        listEnd = Math.min(listEnd, tracksCount)

        val numberLength = Integer.toString(listEnd).length

        val sublist = player.getTracksInRange(i, listEnd)

        if (player.isShuffle) {
            mb.append(context.i18n("listShowShuffled"))
            mb.append("\n")
            if (player.repeatMode == RepeatMode.OFF)
                mb.append("\n")
        }

        if (player.isRoundRobin)
            mb.append(context.i18n("listShowRoundRobin")).append("\n")

        if (player.repeatMode == RepeatMode.SINGLE) {
            mb.append(context.i18n("listShowRepeatSingle"))
            mb.append("\n")
        } else if (player.repeatMode == RepeatMode.ALL) {
            mb.append(context.i18n("listShowRepeatAll"))
            mb.append("\n")
        }

        mb.append(context.i18nFormat("listPageNum", page, maxPages))
        mb.append("\n")
        mb.append("\n")

        for (atc in sublist) {
            var status = " "
            if (i == 0) {
                status = if (player.isPlaying) " \\▶" else " \\\u23F8" //Escaped play and pause emojis
            }
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

        //Now add a timestamp for how much is remaining
        val timestamp = TextUtils.formatTime(player.totalRemainingMusicTimeMillis)

        val streams = player.streamsCount
        val numTracks = tracksCount - streams

        val desc: String

        if (numTracks == 0L) {
            //We are only listening to streams
            desc = context.i18nFormat(if (streams == 1L) "listStreamsOnlySingle" else "listStreamsOnlyMultiple",
                    streams, if (streams == 1L)
                context.i18n("streamSingular")
            else
                context.i18n("streamPlural"))
        } else {

            desc = context.i18nFormat(if (numTracks == 1L) "listStreamsOrTracksSingle" else "listStreamsOrTracksMultiple",
                    numTracks, if (numTracks == 1L)
                context.i18n("trackSingular")
            else
                context.i18n("trackPlural"), timestamp, if (streams == 0L)
                ""
            else
                context.i18nFormat("listAsWellAsLiveStreams", streams, if (streams == 1L)
                    context.i18n("streamSingular")
                else
                    context.i18n("streamPlural")))
        }

        mb.append("\n").append(desc)

        context.reply(mb.build())

    }

    override fun help(context: Context): String {
        return "{0}{1} (page)\n#" + context.i18n("helpListCommand")
    }
}
