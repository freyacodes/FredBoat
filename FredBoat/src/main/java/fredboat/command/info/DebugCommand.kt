/*
 *
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
 */

package fredboat.command.info

import com.fredboat.sentinel.entities.Embed
import com.fredboat.sentinel.entities.coloredEmbed
import com.fredboat.sentinel.entities.field
import fredboat.audio.player.GuildPlayer
import fredboat.audio.player.humanUsersInCurrentVC
import fredboat.audio.player.trackCount
import fredboat.audio.player.voiceChannel
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.ICommandRestricted
import fredboat.commandmeta.abs.IInfoCommand
import fredboat.definitions.PermissionLevel
import fredboat.main.Launcher
import fredboat.messaging.internal.Context
import fredboat.perms.Permission
import fredboat.perms.PermsUtil
import fredboat.sentinel.Guild
import fredboat.sentinel.getGuild
import fredboat.util.extension.asCodeBlock

class DebugCommand(name: String, vararg aliases: String) : Command(name, *aliases), IInfoCommand, ICommandRestricted {

    override val minimumPerms: PermissionLevel
        get() = PermissionLevel.ADMIN

    override suspend fun invoke(context: CommandContext) {
        val guild: Guild? = if (!context.hasArguments()) {
            context.guild
        } else {
            try {
                if (!PermsUtil.checkPermsWithFeedback(PermissionLevel.BOT_ADMIN, context)) {
                    return
                }
                getGuild(context.args[0].toLong())
            } catch (ignored: NumberFormatException) {
                null
            }

        }

        if (guild != null) {
            context.reply(getDebugEmbed(Launcher.botController.playerRegistry.awaitPlayer(guild)))
        } else {
            context.replyWithName(String.format("There is no guild with id `%s`.", context.args[0]))
        }
    }

    private fun getDebugEmbed(player: GuildPlayer) = coloredEmbed {
        title = "Debug information for \"${player.guild.name}\""
        addLavaLinkDebug(player)
        addPlayerDebug(player)
        addVoiceChannelDebug(player)
        addAllTextChannelDebug(player.guild)
        addAllVoiceChannelDebug(player.guild)
    }

    private fun Embed.addLavaLinkDebug(player: GuildPlayer) = field {
        title = "**LavaLink Debug**"
        body = "State: " + player.player.link.state
        body = body.asCodeBlock()
    }

    private fun Embed.addVoiceChannelDebug(player: GuildPlayer) = field {
        title = "**VoiceChannel Debug**"
        body = "Current vc: null"

        val vc = player.voiceChannel
        if (vc != null) {
            val vcUsers = player.humanUsersInCurrentVC
            val str = StringBuilder()
            for (user in vcUsers) {
                str.append(user.effectiveName).append(" ")
            }
            body = "Current vc: ${vc.name}\nHumans in vc:\n$str"

        }
    }

    private fun Embed.addPlayerDebug(player: GuildPlayer) = field {
        title = "**Player Debug**"
        body = ("IsPlaying:  " + player.isPlaying + "\n"
                + "Shuffle:    " + player.isShuffle + "\n"
                + "Repeat:     " + player.repeatMode + "\n")
        if (player.isPlaying) {
            body += "Queue size: " + player.trackCount
        }
        body = body.asCodeBlock()
    }

    private fun Embed.addAllTextChannelDebug(guild: Guild) = field {
        title = "**TextChannel Permissions - Can Talk**"
        val builder = StringBuilder()
        guild.textChannels.forEach { _, channel ->
            builder.append(if (channel.canTalk()) "+ " else "- ")
                    .append("#")
                    .append(channel.name)
                    .append("\n")
        }
        body = builder.toString().asCodeBlock("diff")
    }

    private fun Embed.addAllVoiceChannelDebug(guild: Guild) = field {
        title = "**VoiceChannel Permissions - Can connect and speak**"
        val builder = StringBuilder()

        guild.voiceChannels.forEach { _, channel ->
            if (channel.checkOurPermissions(Permission.VOICE_CONNECT + Permission.VOICE_CONNECT)) {
                builder.append("+ ")
            } else {
                builder.append("- ")
            }
            builder.append(channel.name).append("\n")
        }
        body = builder.toString().asCodeBlock("diff")
    }

    override fun help(context: Context): String {
        return "{0}{1} OR {0}{1} <guildId>\n#Display debug information for the selected guild"
    }
}
