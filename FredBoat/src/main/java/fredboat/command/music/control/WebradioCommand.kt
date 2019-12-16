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

import fredboat.audio.player.GuildPlayer
import fredboat.audio.player.PlayerLimiter
import fredboat.audio.player.VideoSelectionCache
import fredboat.command.info.HelpCommand
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.ICommandRestricted
import fredboat.commandmeta.abs.IMusicCommand
import fredboat.definitions.PermissionLevel
import fredboat.main.Launcher
import fredboat.messaging.internal.Context
import fredboat.shared.constant.BotConstants
import fredboat.util.TextUtils
import fredboat.util.rest.TrackSearcher
import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import java.util.*

class WebradioCommand(private val playerLimiter: PlayerLimiter, private val trackSearcher: TrackSearcher,
                      private val videoSelectionCache: VideoSelectionCache,
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

        if(context.args[0].equals("list")){
            var map = listOfRadio(context)
            context.reply(map.keys.toString().replace(",", "\n").substring(1, map.keys.toString().length - 1))
        }

        if(!context.rawArgs.equals("list")){
            val map = listOfRadio(context)
            if(map.get(context.rawArgs) != null){
                val player = Launcher.botController.playerRegistry.getOrCreate(context.guild)
                val doc2 = Jsoup.connect(map[context.rawArgs]).get()
                val masthead = doc2.select("tr")[4].select("td")[1]
                player.queue(masthead.text(), context, isPriority)
                player.setPause(false)
            }
            else{
                context.reply("Webradio Not found")
            }
        }
        context.deleteMessage()
    }

    private suspend fun handleNoArguments(context: CommandContext, player: GuildPlayer?) {
        if (player == null || player.isQueueEmpty) {
            context.reply(context.i18n("playQueueEmpty")
                    .replace(";;play", context.prefix + context.command.name))
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

     fun listOfRadio(context: CommandContext) : TreeMap<String, String>{
        // Prints "Hello, World" to the terminal window.
        val doc = Jsoup.connect("http://fluxradios.blogspot.com/p/flux-radios-francaise.html").get()
        val links = doc.select("ul:nth-child(39)")
        val map = TreeMap<String, String>()
        for (link in links) {
            val vartmp = link.getElementsByTag("li").select("a[href]")
            for (test in vartmp) {
                map[test.text()] = test.attr("href")
            }
        }
        return map
    }

    override fun help(context: Context): String {
        val usage = "{0}{1} <url> OR {0}{1} <search-term>\n#"
        return usage + context.i18nFormat(if (!isPriority) "helpPlayCommand" else "helpPlayNextCommand", BotConstants.DOCS_URL)
    }

    companion object {

        private val log = LoggerFactory.getLogger(WebradioCommand::class.java)
        private val JOIN_COMMAND = JoinCommand("")
        private const val FILE_PREFIX = "file://"
    }
}
