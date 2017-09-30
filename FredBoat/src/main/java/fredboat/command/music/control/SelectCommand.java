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

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fredboat.Config;
import fredboat.FredBoat;
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.PlayerRegistry;
import fredboat.audio.player.VideoSelection;
import fredboat.audio.queue.AudioTrackContext;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

public class SelectCommand extends Command implements IMusicCommand, ICommandRestricted {

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        select(context);
    }

    static void select(CommandContext context) {
        String[] args = context.args;
        Member invoker = context.invoker;
        GuildPlayer player = PlayerRegistry.get(context.guild);
        player.setCurrentTC(context.channel);
        VideoSelection selection = VideoSelection.get(invoker);
        if (selection != null) {
            try {
                int i = 1;

                if (args.length >= 1) {
                    String contentWithoutPrefix = args[0].substring(Config.CONFIG.getPrefix().length());
                    if (StringUtils.isNumeric(contentWithoutPrefix)) {
                        i = Integer.valueOf(contentWithoutPrefix);
                    } else {
                        i = Integer.valueOf(args[1]);
                    }
                }

                if (selection.choices.size() < i || i < 1) {
                    throw new NumberFormatException();
                } else {
                    AudioTrack selected = selection.choices.get(i - 1);
                    VideoSelection.remove(invoker);
                    TextChannel tc = FredBoat.getTextChannelById(Long.toString(selection.channelId));
                    if (tc != null) {
                        String msg = context.i18nFormat("selectSuccess", i, selected.getInfo().title,
                                TextUtils.formatTime(selected.getInfo().length));
                        CentralMessaging.editMessage(tc, selection.outMsgId, CentralMessaging.from(msg));
                    }
                    player.queue(new AudioTrackContext(selected, invoker));
                    player.setPause(false);
                    context.deleteMessage();
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                context.reply(context.i18nFormat("selectInterval", selection.choices.size()));
            }
        } else {
            context.reply(context.i18n("selectSelectionNotGiven"));
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} n OR {0}{2} n\n#" + context.i18n("helpSelectCommand");
    }

    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.USER;
    }
}
