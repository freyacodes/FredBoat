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

import fredboat.audio.player.AudioLossCounter;
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.PlayerRegistry;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IMaintenanceCommand;
import fredboat.messaging.internal.Context;

import javax.annotation.Nonnull;

public class AudioDebugCommand extends Command implements IMaintenanceCommand {

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        String msg = "";
        GuildPlayer guildPlayer = PlayerRegistry.getExisting(context.guild);

        if(guildPlayer == null) {
            msg = msg + "No GuildPlayer found.\n";
        } else {
            int deficit = AudioLossCounter.EXPECTED_PACKET_COUNT_PER_MIN - (guildPlayer.getAudioLossCounter().getLastMinuteLoss() + guildPlayer.getAudioLossCounter().getLastMinuteSuccess());

            msg = msg + "Last minute's packet stats:```\n"
                + "Packets sent:   " + guildPlayer.getAudioLossCounter().getLastMinuteSuccess() + "\n"
                + "Null packets:   " + guildPlayer.getAudioLossCounter().getLastMinuteLoss() + "\n"
                + "Packet deficit: " + deficit + "\n```";
        }

        context.replyWithName(msg);

    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1}\n#Show audio related debug information.";
    }
}
