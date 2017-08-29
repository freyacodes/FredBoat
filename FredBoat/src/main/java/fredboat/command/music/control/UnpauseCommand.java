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

package fredboat.command.music.control;

import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.LavalinkManager;
import fredboat.audio.player.PlayerRegistry;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.feature.I18n;
import fredboat.perms.PermissionLevel;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

public class UnpauseCommand extends Command implements IMusicCommand, ICommandRestricted {

    private static final JoinCommand JOIN_COMMAND = new JoinCommand();

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        GuildPlayer player = PlayerRegistry.get(guild);
        player.setCurrentTC(channel);
        if (player.isQueueEmpty()) {
            channel.sendMessage(I18n.get(guild).getString("unpauseQueueEmpty")).queue();
        } else if (!player.isPaused()) {
            channel.sendMessage(I18n.get(guild).getString("unpausePlayerNotPaused")).queue();
        } else if (player.getHumanUsersInVC().isEmpty() && player.isPaused() && LavalinkManager.ins.getConnectedChannel(guild) != null) {
            channel.sendMessage(I18n.get(guild).getString("unpauseNoUsers")).queue();
        } else if(LavalinkManager.ins.getConnectedChannel(guild) == null) {
            // When we just want to continue playing, but the user is not in a VC
            JOIN_COMMAND.onInvoke(guild, channel, invoker, message, new String[0]);
            if(LavalinkManager.ins.getConnectedChannel(guild) != null || guild.getAudioManager().isAttemptingToConnect()) {
                player.play();
                channel.sendMessage(I18n.get(guild).getString("unpauseSuccess")).queue();
            }
        } else {
            player.play();
            channel.sendMessage(I18n.get(guild).getString("unpauseSuccess")).queue();
        }
    }

    @Override
    public String help(Guild guild) {
        String usage = "{0}{1}\n#";
        return usage + I18n.get(guild).getString("helpUnpauseCommand");
    }

    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.DJ;
    }
}
