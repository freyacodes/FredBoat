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

import fredboat.Config;
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.PlayerRegistry;
import fredboat.commandmeta.MessagingException;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

public class VolumeCommand extends Command implements IMusicCommand, ICommandRestricted {

    @Override
    public void onInvoke(@Nonnull CommandContext context) {

        if(Config.CONFIG.getDistribution().volumeSupported()) {

            GuildPlayer player = PlayerRegistry.getOrCreate(context.guild);
            try {
                float volume = Float.parseFloat(context.args[1]) / 100;
                volume = Math.max(0, Math.min(1.5f, volume));

                context.reply(context.i18nFormat("volumeSuccess",
                        Math.floor(player.getVolume() * 100), Math.floor(volume * 100)));

                player.setVolume(volume);
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
                throw new MessagingException(context.i18nFormat("volumeSyntax",
                        100 * PlayerRegistry.DEFAULT_VOLUME, Math.floor(player.getVolume() * 100)));
            }
        } else {
            context.reply(context.i18n("volumeApology"),
                    msg -> msg.delete().queueAfter(2, TimeUnit.MINUTES));
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} <0-150>\n#" + context.i18n("helpVolumeCommand");
    }

    @Nonnull
    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.DJ;
    }
}
