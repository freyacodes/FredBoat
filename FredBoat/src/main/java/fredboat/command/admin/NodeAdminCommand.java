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

package fredboat.command.admin;

import fredboat.command.info.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.definitions.PermissionLevel;
import fredboat.main.Launcher;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermsUtil;
import fredboat.util.TextUtils;
import lavalink.client.io.Lavalink;
import lavalink.client.io.LavalinkLoadBalancer;
import lavalink.client.io.LavalinkSocket;
import lavalink.client.io.RemoteStats;

import javax.annotation.Nonnull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NodeAdminCommand extends Command implements ICommandRestricted {

    public NodeAdminCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        if (Launcher.getBotController().getAudioConnectionFacade().isLocal()) {
            context.reply("Lavalink is disabled");
            return;
        }
        if (!context.hasArguments()) {
            HelpCommand.sendFormattedCommandHelp(context);
            return;
        }
        switch (context.args[0]) {
            case "del":
            case "delete":
            case "remove":
            case "rem":
            case "rm":
                if (context.args.length < 2) {
                    HelpCommand.sendFormattedCommandHelp(context);
                } else {
                    remove(context);
                }
                break;
            case "add":
                if (context.args.length < 4) {
                    HelpCommand.sendFormattedCommandHelp(context);
                } else {
                    add(context);
                }
                break;
            case "show":
                if (context.args.length < 2) {
                    HelpCommand.sendFormattedCommandHelp(context);
                } else {
                    show(context);
                }
                break;
            case "list":
            default:
                list(context);
                break;
        }
    }

    private void remove(@Nonnull CommandContext context) {
        String name = context.args[1];
        List<LavalinkSocket> nodes = Launcher.getBotController().getAudioConnectionFacade().getLavalink().getNodes();
        int key = -1;
        for (int i = 0; i < nodes.size(); i++) {
            LavalinkSocket node = nodes.get(i);
            if (node.getName().equals(name)) {
                key = i;
            }
        }
        if (key < 0) {
            context.reply("No node with name " + name + " found.");
            return;
        }

        Launcher.getBotController().getAudioConnectionFacade().getLavalink().removeNode(key);
        context.reply("Removed node " + name);
    }

    private void add(@Nonnull CommandContext context) {
        String name = context.args[1];
        URI uri;
        try {
            uri = new URI(context.args[2]);
        } catch (URISyntaxException e) {
            context.reply(context.args[2] + " is not a valid URI");
            return;
        }

        String password = context.args[3];
        Launcher.getBotController().getAudioConnectionFacade().getLavalink().addNode(name, uri, password);
        context.reply("Added node: " + name + " @ " + uri.toString());
    }

    private void show(@Nonnull CommandContext context) {
        String name = context.args[1];
        List<LavalinkSocket> nodes = Launcher.getBotController().getAudioConnectionFacade().getLavalink().getNodes().stream()
                .filter(ll -> ll.getName().equals(name))
                .collect(Collectors.toList());

        if (nodes.isEmpty()) {
            context.reply("No such node: " + name + ", showing a list of all nodes instead");
            list(context);
            return;
        }


        RemoteStats stats = nodes.get(0).getStats();
        String out = "No stats have been received from this node! Is the node down?";
        if (stats != null) {
            out = TextUtils.asCodeBlock(stats.getAsJson().toString(4), "json");
        }
        context.reply(out);
    }

    private void list(@Nonnull CommandContext context) {
        Lavalink lavalink = Launcher.getBotController().getAudioConnectionFacade().getLavalink();


        boolean showHosts = false;
        if (context.hasArguments() && context.rawArgs.contains("host")) {
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

        for (LavalinkSocket socket : nodes) {
            RemoteStats stats = socket.getStats();
            String str = "Name:                " + socket.getName() + "\n";

            if (showHosts) {
                str += "Host:                    " + socket.getRemoteUri() + "\n";
            }

            if (stats == null) {
                str += "No stats have been received from this node! Is the node down?";
                str += "\n\n";
                messages.add(str);
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
        }

        if (showHosts) {
            for (String str : messages) {
                context.replyPrivate(TextUtils.asCodeBlock(str), null, null);
            }
            context.replyWithName("Sent you a DM with the data. If you did not receive anything, adjust your privacy settings so I can DM you.");
        } else {
            for (String str : messages) {
                context.reply(TextUtils.asCodeBlock(str));
            }
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} list (host)"
                + "\n{0}{1} show <name>"
                + "\n{0}{1} add <name> <uri> <pass>"
                + "\n{0}{1} remove <name>"
                + "\n#Show information about connected lavalink nodes, or add or remove lavalink nodes.";
    }

    @Nonnull
    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.BOT_ADMIN;
    }
}
