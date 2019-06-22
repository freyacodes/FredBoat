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

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import fredboat.audio.player.VideoSelectionCache
import fredboat.audio.queue.AudioTrackContext
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.ICommandRestricted
import fredboat.commandmeta.abs.IMusicCommand
import fredboat.definitions.PermissionLevel
import fredboat.feature.metrics.Metrics
import fredboat.main.Launcher
import fredboat.messaging.internal.Context
import fredboat.util.TextUtils
import org.apache.commons.lang3.StringUtils
import java.util.*

class SelectCommand(private val videoSelectionCache: VideoSelectionCache, name: String, vararg aliases: String)
    : Command(name, *aliases), IMusicCommand, ICommandRestricted {

    override val minimumPerms: PermissionLevel
        get() = PermissionLevel.USER

    override suspend fun invoke(context: CommandContext) {
        select(context, videoSelectionCache)
    }

    override fun help(context: Context): String {
        return "{0}{1} n OR {0}{2} n\n#" + context.i18n("helpSelectCommand")
    }

    companion object {
        internal suspend fun select(context: CommandContext, videoSelectionCache: VideoSelectionCache) {
            val invoker = context.member
            val selection = videoSelectionCache[invoker]
            if (selection == null) {
                context.reply(context.i18n("selectSelectionNotGiven"))
                return
            }

            try {
                //Step 1: Parse the issued command for numbers

                // LinkedHashSet to handle order of choices + duplicates
                val requestChoices = LinkedHashSet<Int>()

                // Combine all args and the command trigger if it is numeric
                var commandOptions = context.rawArgs
                if (StringUtils.isNumeric(context.trigger)) {
                    commandOptions = (context.trigger + " " + commandOptions).trim { it <= ' ' }
                }

                if (StringUtils.isNumeric(commandOptions)) {
                    requestChoices.add(Integer.valueOf(commandOptions))
                } else if (TextUtils.isSplitSelect(commandOptions)) {
                    requestChoices.addAll(TextUtils.getSplitSelect(commandOptions))
                }

                //Step 2: Use only valid numbers (usually 1-5)

                val validChoices = ArrayList<Int>()
                // Only include valid values which are 1 to <size> of the offered selection
                for (value in requestChoices) {
                    if (1 <= value && value <= selection.choices.size) {
                        validChoices.add(value)
                        Metrics.selectionChoiceChosen.labels(value.toString()).inc()
                    }
                }

                //Step 3: Make a selection based on the order of the valid numbers

                // any valid choices at all?
                if (validChoices.isEmpty()) {
                    throw NumberFormatException()
                } else {
                    if (validChoices.size > 1) {
                        Metrics.multiSelections.labels(Integer.toString(validChoices.size)).inc()
                    }
                    val selectedTracks = arrayOfNulls<AudioTrack>(validChoices.size)
                    val outputMsgBuilder = StringBuilder()
                    val player = Launcher.botController.playerRegistry.awaitPlayer(context.guild)
                    for (i in validChoices.indices) {
                        selectedTracks[i] = selection.choices[validChoices[i] - 1]

                        val msg = context.i18nFormat("selectSuccess", validChoices[i],
                                TextUtils.escapeAndDefuse(selectedTracks[i]!!.info.title),
                                TextUtils.formatTime(selectedTracks[i]!!.info.length))
                        if (i < validChoices.size) {
                            outputMsgBuilder.append("\n")
                        }
                        outputMsgBuilder.append(msg)

                        player.queueLimited(AudioTrackContext(selectedTracks[i]!!, invoker, selection.isPriority))
                        player.play()
                    }

                    videoSelectionCache.remove(invoker)
                    val tc = selection.context.textChannel
                    tc.editMessage(selection.message, outputMsgBuilder.toString()).subscribe()

                    player.setPause(false)
                    context.deleteMessage()
                }
            } catch (e: NumberFormatException) {
                context.reply(context.i18nFormat("selectInterval", selection.choices.size))
            } catch (e: ArrayIndexOutOfBoundsException) {
                context.reply(context.i18nFormat("selectInterval", selection.choices.size))
            }

        }
    }
}
