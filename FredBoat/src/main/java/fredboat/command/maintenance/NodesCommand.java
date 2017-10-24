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
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IMaintenanceCommand;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import fredboat.perms.PermsUtil;
import fredboat.util.TextUtils;
import lavalink.client.io.Lavalink;
import lavalink.client.io.LavalinkLoadBalancer;
import lavalink.client.io.LavalinkSocket;
import lavalink.client.io.RemoteStats;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class NodesCommand extends Command implements IMaintenanceCommand {

    public NodesCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        if (LavalinkManager.ins.isEnabled()) {
            handleLavalink(context);
        } else {
            handleLavaplayer(context);
        }

    }

    @SuppressWarnings("StringConcatenationInLoop")
    private void handleLavalink(CommandContext context) {
        Lavalink lavalink = LavalinkManager.ins.getLavalink();
        if (context.hasArguments() && !context.args[0].equals("host")) {
            try {
                LavalinkSocket socket = lavalink.getNodes().get(Integer.parseInt(context.args[0]));
                context.reply(TextUtils.asCodeBlock(socket.getStats().getAsJson().toString(4), "json"));
                return;
            } catch (NumberFormatException | IndexOutOfBoundsException ignored) { //fallthrough
                context.replyWithName(String.format("No such node: `%s`, showing all nodes instead.", context.args[0]));
            }
        }

        boolean showHosts = false;
        if (context.hasArguments() && context.args[0].equals("host")) {
            if (PermsUtil.checkPermsWithFeedback(PermissionLevel.BOT_ADMIN, context)) {
                showHosts = true;
            } else {
                return;
            }
        }

        List<LavalinkSocket> nodes = lavalink.getNodes();
        if (nodes.isEmpty()) {
            context.replyWithName("There are no remote lavalink nodes registered.");
            return;
        }

        List<String> messages = new ArrayList<>();

        int i = 0;
        for (LavalinkSocket socket : nodes) {
            RemoteStats stats = socket.getStats();
            String str = "Socket:             #" + i + "\n";

            if (showHosts) {
                str += "Address:                 " + socket.getRemoteUri() + "\n";
            }

            if (stats == null) {
                str += "No stats have been received from this node! Is the node down?";
                str += "\n";
                str += "\n";
                i++;
                continue;
            }

            str += "Playing players:         " + stats.getPlayingPlayers() + "\n";
            str += "Lavalink load:           " + TextUtils.formatPercent(stats.getLavalinkLoad()) + "\n";
            str += "System load:             " + TextUtils.formatPercent(stats.getSystemLoad()) + " \n";
            str += "Memory:                  " + stats.getMemUsed() / 1000000 + "MB/" + stats.getMemReservable() / 1000000 + "MB\n";
            str += "---------------\n";
            str += "Average frames sent:     " + stats.getAvgFramesSentPerMinute() + "\n";
            str += "Average frames nulled:   " + stats.getAvgFramesNulledPerMinute() + "\n";
            str += "Average frames deficit:  " + stats.getAvgFramesDeficitPerMinute() + "\n";
            str += "---------------\n";
            LavalinkLoadBalancer.Penalties penalties = LavalinkLoadBalancer.getPenalties(socket);
            str += "Penalties Total:    " + penalties.getTotal() + "\n";
            str += "Player Penalty:          " + penalties.getPlayerPenalty() + "\n";
            str += "CPU Penalty:             " + penalties.getCpuPenalty() + "\n";
            str += "Deficit Frame Penalty:   " + penalties.getDeficitFramePenalty() + "\n";
            str += "Null Frame Penalty:      " + penalties.getNullFramePenalty() + "\n";
            str += "Raw: " + penalties.toString() + "\n";
            str += "---------------\n\n";

            messages.add(str);
            i++;
        }

        for (String str : messages) {
            context.reply(TextUtils.asCodeBlock(str));
        }
    }

    private void handleLavaplayer(CommandContext context) {
        AudioPlayerManager pm = AbstractPlayer.getPlayerManager();
        List<RemoteNode> nodes = pm.getRemoteNodeRegistry().getNodes();
        boolean showHost = false;

        if (context.hasArguments() && context.args[0].equals("host")) {
            if (PermsUtil.checkPerms(PermissionLevel.BOT_OWNER, context.invoker)) {
                showHost = true;
            } else {
                context.replyWithName("You do not have permission to view the hosts!");
            }
        }
        if (nodes.isEmpty()) {
            context.replyWithName("There are no remote lavaplayer nodes registered.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (RemoteNode node : nodes) {
            sb.append("Node ").append(i).append("\n");
            if (showHost) {
                sb.append(node.getAddress()).append("\n");
            }
            sb.append("Status: ")
                    .append(node.getConnectionState().toString())
                    .append("\nPlaying: ")
                    .append(node.getLastStatistics() == null ? "UNKNOWN" : node.getLastStatistics().playingTrackCount)
                    .append("\nCPU: ")
                    .append(node.getLastStatistics() == null ? "UNKNOWN" : TextUtils.formatPercent(node.getLastStatistics().systemCpuUsage))
                    .append("\n");

            sb.append(node.getBalancerPenaltyDetails());

            sb.append("\n\n");

            i++;
        }

        context.reply(TextUtils.asCodeBlock(sb.toString()));
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} OR {0}{1} host\n#Show information about the connected lava nodes.";
    }
}
