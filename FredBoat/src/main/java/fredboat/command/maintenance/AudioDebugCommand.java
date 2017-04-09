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

import fredboat.audio.AudioLossCounter;
import fredboat.audio.GuildPlayer;
import fredboat.audio.PlayerRegistry;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.IMaintenanceCommand;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

public class AudioDebugCommand extends Command implements IMaintenanceCommand {

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        String msg = "";
        GuildPlayer guildPlayer = PlayerRegistry.getExisting(guild);

        if(guildPlayer == null) {
            msg = msg + "No GuildPlayer found.\n";
        } else {
            int deficit = AudioLossCounter.EXPECTED_PACKET_COUNT_PER_MIN - (guildPlayer.getAudioLossCounter().getLastMinuteLoss() + guildPlayer.getAudioLossCounter().getLastMinuteSuccess());

            msg = msg + "Last minute's packet stats:```\n"
                + "Packets sent:   " + guildPlayer.getAudioLossCounter().getLastMinuteSuccess() + "\n"
                + "Null packets:   " + guildPlayer.getAudioLossCounter().getLastMinuteLoss() + "\n"
                + "Packet deficit: " + deficit + "\n```";
        }

        TextUtils.replyWithName(channel, invoker, msg);

    }

    @Override
    public String help(Guild guild) {
        return "{0}{1}\n#Show audio related debug information.";
    }
}
