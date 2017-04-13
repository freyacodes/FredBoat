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

package fredboat.command.maintenance;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.remote.RemoteNode;
import fredboat.audio.AbstractPlayer;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.IMaintenanceCommand;
import fredboat.util.DiscordUtil;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.List;

public class NodesCommand extends Command implements IMaintenanceCommand {

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        AudioPlayerManager pm = AbstractPlayer.getPlayerManager();
        List<RemoteNode> nodes = pm.getRemoteNodeRegistry().getNodes();
        boolean showHost = false;

        if (args.length == 2 && args[1].equals("host")) {
            if (DiscordUtil.isUserBotOwner(invoker.getUser())) {
                showHost = true;
            } else {
                TextUtils.replyWithName(channel, invoker, "You do not have permission to view the hosts!");
            }
        }

        MessageBuilder mb = new MessageBuilder();
        mb.append("```\n");
        int i = 0;
        for (RemoteNode node : nodes) {
            mb.append("Node " + i + "\n");
            if (showHost) {
                mb.append(node.getAddress())
                        .append("\n");
            }
            mb.append("Status: ")
                    .append(node.getConnectionState().toString())
                    .append("\nPlaying: ")
                    .append(node.getLastStatistics() == null ? "UNKNOWN" : node.getLastStatistics().playingTrackCount)
                    .append("\nCPU: ")
                    .append(node.getLastStatistics() == null ? "UNKNOWN" : node.getLastStatistics().systemCpuUsage * 100 + "%")
                    .append("\n");

            mb.append(node.getBalancerPenaltyDetails());

            mb.append("\n\n");

            i++;
        }

        mb.append("```");
        channel.sendMessage(mb.build()).queue();
    }

    @Override
    public String help(Guild guild) {
        return "{0}{1} OR {0}{1} host\n#Show information about the connected lava nodes.";
    }
}
