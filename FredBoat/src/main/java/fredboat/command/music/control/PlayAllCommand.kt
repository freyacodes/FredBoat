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

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import fredboat.audio.player.PlayerLimiter
import fredboat.audio.player.VideoSelectionCache
import fredboat.audio.queue.AudioTrackContext
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.ICommandRestricted
import fredboat.commandmeta.abs.IMusicCommand
import fredboat.definitions.PermissionLevel
import fredboat.definitions.SearchProvider
import fredboat.main.Launcher
import fredboat.messaging.internal.Context
import fredboat.shared.constant.BotConstants
import fredboat.util.TextUtils
import fredboat.util.extension.edit
import fredboat.util.rest.TrackSearcher
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory

class PlayAllCommand(private val playerLimiter: PlayerLimiter, private val trackSearcher: TrackSearcher,
                     private val videoSelectionCache: VideoSelectionCache, private val searchProviders: List<SearchProvider>,
                     name: String, vararg aliases: String, private val isPriority: Boolean = false
) : Command(name, *aliases), IMusicCommand, ICommandRestricted {

    override val minimumPerms: PermissionLevel
        get() = if (isPriority) PermissionLevel.DJ else PermissionLevel.USER

    override suspend fun invoke(context: CommandContext) {
        if (context.member.voiceChannel == null) {
            context.reply(context.i18n("playerUserNotInChannel"))
            return
        }

        if (!playerLimiter.checkLimitResponsive(context, Launcher.botController.playerRegistry)) return

        if (!context.hasArguments()) {
            context.reply(context.i18n("playAllSearchNotGiven"))
            return
        }

        var url = StringUtils.strip(context.args[0], "<>")
        //Search youtube for videos and play them directly
        if (!url.startsWith("http") && !url.startsWith(FILE_PREFIX)) {
            searchAndPlayForVideos(context)
            return
        }
    }

    private fun searchAndPlayForVideos(context: CommandContext) {
        //Now remove all punctuation
        val query = context.rawArgs.replace(TrackSearcher.PUNCTUATION_REGEX.toRegex(), "")

        context.replyMono(context.i18n("playSearching").replace("{q}", query))
                .subscribe{ outMsg ->
            val list: AudioPlaylist?
            try {
                list = trackSearcher.searchForTracks(query, searchProviders)
            } catch (e: TrackSearcher.SearchingException) {
                context.reply(context.i18n("playYoutubeSearchError"))
                log.error("YouTube search exception", e)
                return@subscribe
            }

            if (list == null || list.tracks.isEmpty()) {
                outMsg.edit(
                        context.textChannel,
                        context.i18n("playSearchNoResults").replace("{q}", query)
                ).subscribe()

            } else {
                //Get at most 5 tracks
                val selectable = list.tracks.subList(0, Math.min(TrackSearcher.MAX_RESULTS, list.tracks.size))

                val oldSelection = videoSelectionCache.remove(context.member)
                oldSelection?.deleteMessage()

                videoSelectionCache.put(outMsg.messageId, context, selectable, isPriority)

                //Add musics in the queue
                val player = Launcher.botController.playerRegistry.getOrCreate(context.guild)
                val invoker = context.member
                val selection = videoSelectionCache[invoker]
                val selectedTracks = arrayOfNulls<AudioTrack>(TrackSearcher.MAX_RESULTS)
                val outputMsgBuilder = StringBuilder()

                if (selection == null) {
                    outMsg.edit(
                            context.textChannel,
                            context.i18n("playSearchNoResults").replace("{q}", query)
                    ).subscribe()
                } else {

                    for (i in 0 until TrackSearcher.MAX_RESULTS) {
                        selectedTracks[i] = selection.choices[i]

                        val msg = context.i18nFormat("selectSuccess", (i + 1),
                                TextUtils.escapeAndDefuse(selectedTracks[i]!!.info.title),
                                TextUtils.formatTime(selectedTracks[i]!!.info.length))

                        if (i < TrackSearcher.MAX_RESULTS) {
                            outputMsgBuilder.append("\n")
                        }
                        outputMsgBuilder.append(msg)

                        player.queue(AudioTrackContext(selectedTracks[i]!!, invoker, selection.isPriority), selection.isPriority)
                    }
                    videoSelectionCache.remove(invoker)

                    outMsg.edit(context.textChannel, outputMsgBuilder.toString()).subscribe()

                    player.setPause(false)
                    context.deleteMessage()
                }
            }
        }
    }

    override fun help(context: Context): String {
        val usage = "{0}{1} <search-term>\n#"
        return usage + context.i18nFormat(if (!isPriority) "helpPlayAllCommand" else "helpPlayAllTopCommand", BotConstants.DOCS_URL)
    }

    companion object {

        private val log = LoggerFactory.getLogger(PlayAllCommand::class.java)
        private val JOIN_COMMAND = JoinCommand("")
        private const val FILE_PREFIX = "file://"
    }
}