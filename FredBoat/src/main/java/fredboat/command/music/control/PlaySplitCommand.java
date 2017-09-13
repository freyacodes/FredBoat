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
import fredboat.audio.player.PlayerLimitManager;
import fredboat.audio.player.PlayerRegistry;
import fredboat.audio.queue.IdentifierContext;
import fredboat.command.util.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.feature.I18n;
import fredboat.perms.PermissionLevel;
import net.dv8tion.jda.core.entities.Guild;

public class PlaySplitCommand extends Command implements IMusicCommand, ICommandRestricted {


    @Override
    public void onInvoke(CommandContext context) {
        if (LavalinkManager.ins.isEnabled()) {
            context.reply("Human Fred MIGHT have broken this command." +
                    "Maybe it will be fixed in a later update." +
                    "Who knows ¯\\_(ツ)_/¯");
            return;
        }

        if (context.args.length < 2) {
            HelpCommand.sendFormattedCommandHelp(context);
            return;
        }

        if (!PlayerLimitManager.checkLimitResponsive(context)) return;

        IdentifierContext ic = new IdentifierContext(context.args[1], context.channel, context.invoker);
        ic.setSplit(true);

        GuildPlayer player = PlayerRegistry.get(context.guild);
        player.setCurrentTC(context.channel);
        player.queue(ic);
        player.setPause(false);

        try {
            context.deleteMessage();
        } catch (Exception ignored) {
        }
    }

    @Override
    public String help(Guild guild) {
        String usage = "{0}{1} <url>\n#";
        return usage + I18n.get(guild).getString("helpPlaySplitCommand");
    }

    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.USER;
    }
}
