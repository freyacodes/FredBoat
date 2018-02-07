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

import fredboat.main.BotController;
import fredboat.audio.player.LavalinkManager;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import lavalink.client.io.LavalinkSocket;
import net.dv8tion.jda.core.entities.Guild;

import javax.annotation.Nonnull;

public class GetNodeCommand extends Command implements ICommandRestricted {

    public GetNodeCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        if (!LavalinkManager.ins.isEnabled()) {
            context.replyWithName("Lavalink is not enabled.");
            return;
        }

        Guild guild = null;
        if (context.hasArguments()) {
            try {
                long guildId = Long.parseUnsignedLong(context.args[0]);
                guild = BotController.INS.getShardManager().getGuildById(guildId);
            } catch (NumberFormatException ignored) {
            }
            if (guild == null) {
                context.reply("Guild not found for id " + context.args[0]);
                return;
            }
        } else {
            guild = context.guild;
        }
        LavalinkSocket node = LavalinkManager.ins.getLavalink().getLink(guild).getNode();

        String reply = String.format("Guild %s id `%s` lavalink socket: `%s`",
                context.guild.getName(), context.guild.getIdLong(), String.valueOf(node));

        //sensitive info, send it by DM
        context.replyPrivate(reply,
                __ -> context.replyWithName("Sent you a DM with the data."),
                t -> context.replyWithName("Could not DM you, adjust your privacy settings.")
        );
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1}\n#Show information about the currently assigned lavalink node of this guild.";
    }

    @Nonnull
    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.BOT_ADMIN;
    }
}
