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

package fredboat.command.music.seeking;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.PlayerRegistry;
import fredboat.audio.queue.AudioTrackContext;
import fredboat.command.util.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.feature.I18n;
import fredboat.perms.PermissionLevel;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.entities.Guild;

import java.text.MessageFormat;

public class ForwardCommand extends Command implements IMusicCommand, ICommandRestricted {

    @Override
    public void onInvoke(CommandContext context) {
        GuildPlayer player = PlayerRegistry.getExisting(context.guild);

        if(player == null || player.isQueueEmpty()) {
            context.replyWithName(I18n.get(context, "unpauseQueueEmpty"));
            return;
        }

        if (context.args.length == 1) {
            HelpCommand.sendFormattedCommandHelp(context);
            return;
        }

        long t;
        try {
            t = TextUtils.parseTimeString(context.args[1]);
        } catch (IllegalStateException e){
            HelpCommand.sendFormattedCommandHelp(context);
            return;
        }

        AudioTrackContext atc = player.getPlayingTrack();
        AudioTrack at = atc.getTrack();

        //Ensure bounds
        t = Math.max(0, t);
        t = Math.min(atc.getEffectiveDuration(), t);

        player.seekTo(player.getPosition() + t);
        context.reply(MessageFormat.format(I18n.get(context, "fwdSuccess"), atc.getEffectiveTitle(), TextUtils.formatTime(t)));
    }

    @Override
    public String help(Guild guild) {
        String usage = "{0}{1} [[hh:]mm:]ss\n#";
        String example = "  {0}{1} 2:30";
        return usage + I18n.get(guild).getString("helpForwardCommand") + example;
    }

    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.DJ;
    }
}
