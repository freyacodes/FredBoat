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
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import net.dv8tion.jda.core.entities.Guild;

import javax.annotation.Nonnull;

public class UnpauseCommand extends Command implements IMusicCommand, ICommandRestricted {

    private static final JoinCommand JOIN_COMMAND = new JoinCommand();

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        Guild guild = context.guild;
        GuildPlayer player = PlayerRegistry.getOrCreate(guild);
        player.setCurrentTC(context.channel);
        if (player.isQueueEmpty()) {
            context.reply(context.i18n("unpauseQueueEmpty"));
        } else if (!player.isPaused()) {
            context.reply(context.i18n("unpausePlayerNotPaused"));
        } else if (player.getHumanUsersInCurrentVC().isEmpty() && player.isPaused() && LavalinkManager.ins.getConnectedChannel(guild) != null) {
            context.reply(context.i18n("unpauseNoUsers"));
        } else if (LavalinkManager.ins.getConnectedChannel(context.guild) == null) {
            // When we just want to continue playing, but the user is not in a VC
            JOIN_COMMAND.onInvoke(context);
            if(LavalinkManager.ins.getConnectedChannel(guild) != null || guild.getAudioManager().isAttemptingToConnect()) {
                player.play();
                context.reply(context.i18n("unpauseSuccess"));
            }
        } else {
            player.play();
            context.reply(context.i18n("unpauseSuccess"));
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1}\n#" + context.i18n("helpUnpauseCommand");
    }

    @Nonnull
    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.DJ;
    }
}
