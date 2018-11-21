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

package fredboat.command.info

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary
import fredboat.agent.FredBoatAgent
import fredboat.commandmeta.CommandManager
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.IInfoCommand
import fredboat.feature.I18n
import fredboat.main.Launcher
import fredboat.main.getBotController
import fredboat.messaging.internal.Context
import fredboat.sentinel.RawUser
import fredboat.util.AppInfo
import fredboat.util.DiscordUtil
import fredboat.util.TextUtils
import kotlinx.coroutines.experimental.reactive.awaitSingle
import java.text.MessageFormat
import java.util.*

class UptimeCommand(
        name: String,
        selfUser: RawUser,
        vararg aliases: String
) : Command(name, *aliases), IInfoCommand {

    init {
        botId = selfUser.id
    }

    override suspend fun invoke(context: CommandContext) {
        context.reply(getUptime(context))
    }

    override fun help(context: Context): String {
        return "{0}{1}\n#Shows uptime of this bot."
    }

    companion object {
        private var botId = 0L

        suspend fun getUptime(context: Context?): String {
            context?.sendTyping()

            val totalSecs = (System.currentTimeMillis() - Launcher.START_TIME) / 1000
            val days = (totalSecs / (60 * 60 * 24)).toInt()
            val hours = (totalSecs / (60 * 60) % 24).toInt()
            val mins = (totalSecs / 60 % 60).toInt()
            val secs = (totalSecs % 60).toInt()

            val i18n: ResourceBundle = context?.getI18n() ?: I18n.DEFAULT.props

            var str = MessageFormat.format(i18n.getString("uptimeParagraph"),
                    days, hours, mins, secs) + "\n"

            return str
        }
    }
}
