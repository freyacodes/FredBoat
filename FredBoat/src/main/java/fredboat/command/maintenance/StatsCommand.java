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

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import fredboat.Config;
import fredboat.FredBoat;
import fredboat.audio.player.PlayerRegistry;
import fredboat.commandmeta.CommandManager;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IMaintenanceCommand;
import fredboat.feature.I18n;
import fredboat.util.AppInfo;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.entities.Guild;

import java.text.MessageFormat;

public class StatsCommand extends Command implements IMaintenanceCommand {

    @Override
    public void onInvoke(CommandContext context) {
        long totalSecs = (System.currentTimeMillis() - FredBoat.START_TIME) / 1000;
        int days = (int) (totalSecs / (60 * 60 * 24));
        int hours = (int) ((totalSecs / (60 * 60)) % 24);
        int mins = (int) ((totalSecs / 60) % 60);
        int secs = (int) (totalSecs % 60);
        
        String str = MessageFormat.format(
                I18n.get(context, "statsParagraph"),
                days, hours, mins, secs, CommandManager.commandsExecuted.get() - 1)
                + "\n";

        str = MessageFormat.format(I18n.get(context, "statsRate"), str,
                (float) (CommandManager.commandsExecuted.get() - 1) / ((float) totalSecs / (float) (60 * 60)));

        str = str + "\n\n```";

        str = str + "Reserved memory:                " + Runtime.getRuntime().totalMemory() / 1000000 + "MB\n";
        str = str + "-> Of which is used:            " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000 + "MB\n";
        str = str + "-> Of which is free:            " + Runtime.getRuntime().freeMemory() / 1000000 + "MB\n";
        str = str + "Max reservable:                 " + Runtime.getRuntime().maxMemory() / 1000000 + "MB\n";

        str = str + "\n----------\n\n";

        str = str + "Sharding:                       " + FredBoat.getInstance(context.guild.getJDA()).getShardInfo().getShardString() + "\n";
        str = str + "Players playing:                " + PlayerRegistry.getPlayingPlayers().size() + "\n";
        str = str + "Known servers:                  " + FredBoat.countAllGuilds() + "\n";
        str = str + "Known users in servers:         " + FredBoat.countAllUniqueUsers() + "\n";
        str = str + "Distribution:                   " + Config.CONFIG.getDistribution() + "\n";
        str = str + "JDA responses total:            " + context.guild.getJDA().getResponseTotal() + "\n";
        str = str + "JDA version:                    " + JDAInfo.VERSION + "\n";
        str = str + "FredBoat version:               " + AppInfo.getAppInfo().getVersionBuild() + "\n";
        str = str + "Lavaplayer version:             " + PlayerLibrary.VERSION + "\n";

        str = str + "```";

        context.replyWithName(str);
    }

    @Override
    public String help(Guild guild) {
        return "{0}{1}\n#Show some statistics about this bot.";
    }
}
