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

import fredboat.audio.player.LavalinkManager;
import fredboat.command.util.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.perms.PermissionLevel;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.net.URI;
import java.net.URISyntaxException;

public class NodeAdminCommand extends Command implements ICommandRestricted {
    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        if (!LavalinkManager.ins.isEnabled()) {
            channel.sendMessage("Lavalink is disabled").queue();
        }

        switch (args[1]) {
            case "del":
            case "delete":
            case "remove":
            case "rem":
            case "rm":
                remove(channel, args);
                break;
            case "add":
                add(channel, args);
                break;
            case "list":
            default:
                HelpCommand.sendFormattedCommandHelp(message);
                break;
        }
    }

    private void remove(TextChannel channel, String[] args) {
        int key = Integer.valueOf(args[2]);
        LavalinkManager.ins.getLavalink().removeNode(key);
        channel.sendMessage("Removed node #" + key).queue();
    }

    private void add(TextChannel channel, String[] args) {
        URI uri;
        try {
            uri = new URI(args[2]);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        String password = args[3];
        LavalinkManager.ins.getLavalink().addNode(uri, password);
        channel.sendMessage("Added node: " + uri.toString()).queue();
    }

    @Override
    public String help(Guild guild) {
        return "{0}{1}\n#Add or remove lavalink nodes.";
    }

    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.BOT_ADMIN;
    }
}
