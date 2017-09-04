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
import fredboat.audio.player.AbstractPlayer;
import fredboat.audio.player.LavalinkManager;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.IMaintenanceCommand;
import fredboat.perms.PermissionLevel;
import fredboat.perms.PermsUtil;
import fredboat.util.TextUtils;
import lavalink.client.io.Lavalink;
import lavalink.client.io.LavalinkLoadBalancer;
import lavalink.client.io.LavalinkSocket;
import lavalink.client.io.RemoteStats;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.List;

public class NodesCommand extends Command implements IMaintenanceCommand {

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        if (LavalinkManager.ins.isEnabled()) {
            handleLavalink(guild, channel, invoker, message, args);
        } else {
            handleLavaplayer(guild, channel, invoker, message, args);
        }

    }

    @SuppressWarnings("StringConcatenationInLoop")
    private void handleLavalink(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        Lavalink lavalink = LavalinkManager.ins.getLavalink();
        if (args.length >= 2 && !args[1].equals("host")) {
            LavalinkSocket socket = lavalink.getNodes().get(Integer.parseInt(args[1]));


            channel.sendMessage("```json\n" + socket.getStats().getAsJson().toString(4) + "\n```").queue();
            return;
        }

        boolean showHosts = false;
        if (args.length >= 2 && args[1].equals("host")) {
            if (PermsUtil.checkPermsWithFeedback(PermissionLevel.BOT_ADMIN, invoker, channel)) {
                showHosts = true;
            } else {
                return;
            }
        }

        String str = "```";

        int i = 0;
        for (LavalinkSocket socket : lavalink.getNodes()) {
            RemoteStats stats = socket.getStats();
            str += "Socket #" + i + "\n";

            if (showHosts) {
                str += "Address: " + socket.getRemoteUri() + "\n";
            }

            if (stats == null) {
                str += "No stats have been received from this node! Is the node down?";
                str += "\n";
                str += "\n";
                i++;
                continue;
            }

            str += stats.getPlayingPlayers() + " playing players\n";
            str += stats.getLavalinkLoad() * 100f + "% lavalink load\n";
            str += stats.getSystemLoad() * 100f + "% system load\n";
            str += stats.getMemUsed() / 1000000 + "MB/" + stats.getMemReservable() / 1000000 + "MB memory\n";

            str += "\n";

            str += stats.getAvgFramesSentPerMinute() + " player average frames sent\n";
            str += stats.getAvgFramesNulledPerMinute() + " player average frames nulled\n";
            str += stats.getAvgFramesDeficitPerMinute() + " player average frames deficit\n";

            str += "\n";

            str += LavalinkLoadBalancer.getPenalties(socket).toString();

            str += "\n";
            str += "\n";

            i++;
        }

        str += "```";
        channel.sendMessage(str).queue();
    }

    private void handleLavaplayer(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        AudioPlayerManager pm = AbstractPlayer.getPlayerManager();
        List<RemoteNode> nodes = pm.getRemoteNodeRegistry().getNodes();
        boolean showHost = false;

        if (args.length == 2 && args[1].equals("host")) {
            if (PermsUtil.isUserBotOwner(invoker.getUser())) {
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
