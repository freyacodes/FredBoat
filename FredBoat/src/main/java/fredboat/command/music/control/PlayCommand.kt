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
import fredboat.audio.player.GuildPlayer
import fredboat.audio.player.PlayerLimiter
import fredboat.audio.player.VideoSelectionCache
import fredboat.command.info.HelpCommand
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
import fredboat.util.localMessageBuilder
import fredboat.util.rest.TrackSearcher
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory

class PlayCommand(private val playerLimiter: PlayerLimiter, private val trackSearcher: TrackSearcher,
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

        if (!context.msg.attachments.isEmpty()) {
            val player = Launcher.botController.playerRegistry.getOrCreate(context.guild)

            for (atc in context.msg.attachments) {
                player.queue(atc, context, isPriority)
            }

            player.setPause(false)

            return
        }

        if (!context.hasArguments()) {
            val player = Launcher.botController.playerRegistry.getExisting(context.guild)
            handleNoArguments(context, player)
            return
        }

        if (TextUtils.isSplitSelect(context.rawArgs)) {
            SelectCommand.select(context, videoSelectionCache)
            return
        }

        var url = StringUtils.strip(context.args[0], "<>")
        //Search youtube for videos and let the user select a video
        if (!url.startsWith("http") && !url.startsWith(FILE_PREFIX)) {
            searchForVideos(context)
            return
        }
        if (url.startsWith(FILE_PREFIX)) {
            url = url.replaceFirst(FILE_PREFIX.toRegex(), "") //LocalAudioSourceManager does not manage this itself
        }

        val player = Launcher.botController.playerRegistry.getOrCreate(context.guild)
        player.queue(url, context, isPriority)
        player.setPause(false)

        context.deleteMessage()
    }

    private suspend fun handleNoArguments(context: CommandContext, player: GuildPlayer?) {
        if (player == null || player.isQueueEmpty) {
            context.reply(context.i18nFormat("playQueueEmpty", context.prefix, context.command.name))
        } else if (player.isPlaying && !isPriority) {
            context.reply(context.i18n("playAlreadyPlaying"))
        } else if (player.humanUsersInCurrentVC.isEmpty() && context.guild.selfMember.voiceChannel != null) {
            context.reply(context.i18n("playVCEmpty"))
        } else if (context.guild.selfMember.voiceChannel == null) {
            // When we just want to continue playing, but the user is not in a VC
            JOIN_COMMAND.invoke(context)
            if (context.guild.selfMember.voiceChannel != null) {
                player.play()
                context.reply(context.i18n("playWillNowPlay"))
            }
        } else if (isPriority) {
            HelpCommand.sendFormattedCommandHelp(context)
        } else {
            player.play()
            context.reply(context.i18n("playWillNowPlay"))
        }
    }

    private fun searchForVideos(context: CommandContext) {
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

                val builder = localMessageBuilder()
                builder.append(context.i18nFormat("playSelectVideo", TextUtils.escapeMarkdown(context.prefix)))

                var i = 1
                for (track in selectable) {
                    builder.append("\n**")
                            .append(i.toString())
                            .append(":** ")
                            .append(TextUtils.escapeAndDefuse(track.info.title))
                            .append(" (")
                            .append(TextUtils.formatTime(track.info.length))
                            .append(")")

                    i++
                }

                outMsg.edit(context.textChannel, builder.build()).subscribe()
                videoSelectionCache.put(outMsg.messageId, context, selectable, isPriority)
            }
        }
    }

    override fun help(context: Context): String {
        val usage = "{0}{1} <url> OR {0}{1} <search-term>\n#"
        return usage + context.i18nFormat(if (!isPriority) "helpPlayCommand" else "helpPlayNextCommand", BotConstants.DOCS_URL)
    }

    companion object {

        private val log = LoggerFactory.getLogger(PlayCommand::class.java)
        private val JOIN_COMMAND = JoinCommand("")
        private const val FILE_PREFIX = "file://"
    }
}
