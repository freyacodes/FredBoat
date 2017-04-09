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
import fredboat.audio.GuildPlayer;
import fredboat.audio.PlayerRegistry;
import fredboat.audio.queue.RepeatMode;
import fredboat.command.util.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.feature.I18n;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

public class RepeatCommand extends Command implements IMusicCommand {

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        GuildPlayer player = PlayerRegistry.get(guild);

        if (args.length < 2) {
            String command = args[0].substring(Config.CONFIG.getPrefix().length());
            HelpCommand.sendFormattedCommandHelp(guild, channel, invoker, command);
            return;
        }

        RepeatMode desiredRepeatMode;
        String userInput = args[1];
        switch (userInput) {
            case "off":
            case "out":
                desiredRepeatMode = RepeatMode.OFF;
                break;
            case "single":
            case "one":
            case "track":
                desiredRepeatMode = RepeatMode.SINGLE;
                break;
            case "all":
            case "list":
            case "queue":
                desiredRepeatMode = RepeatMode.ALL;
                break;
            case "help":
            default:
                String command = args[0].substring(Config.CONFIG.getPrefix().length());
                HelpCommand.sendFormattedCommandHelp(guild, channel, invoker, command);
                return;
        }

        player.setRepeatMode(desiredRepeatMode);

        switch (desiredRepeatMode) {
            case OFF:
                channel.sendMessage(I18n.get(guild).getString("repeatOff")).queue();
                break;
            case SINGLE:
                channel.sendMessage(I18n.get(guild).getString("repeatOnSingle")).queue();
                break;
            case ALL:
                channel.sendMessage(I18n.get(guild).getString("repeatOnAll")).queue();
                break;
        }
    }

    @Override
    public String help(Guild guild) {
        String usage = "{0}{1} single|all|off\n#";
        return usage + I18n.get(guild).getString("helpRepeatCommand");
    }
}
