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

import fredboat.FredBoat;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.IMaintenanceCommand;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.ArrayList;
import java.util.List;

public class ShardsCommand extends Command implements IMaintenanceCommand {

    private static final int SHARDS_PER_MESSAGE = 30;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        MessageBuilder mb = null;
        List<MessageBuilder> builders = new ArrayList<>();

        int i = 0;
        for(FredBoat fb : FredBoat.getShards()) {
            if(i % SHARDS_PER_MESSAGE == 0) {
                mb = new MessageBuilder()
                        .append("```diff\n");
                builders.add(mb);
            }

            mb.append(fb.getJda().getStatus() == JDA.Status.CONNECTED ? "+" : "-")
                    .append(" ")
                    .append(fb.getShardInfo().getShardString())
                    .append(" ")
                    .append(fb.getJda().getStatus())
                    .append(" -- Guilds: ")
                    .append(String.format("%04d",fb.getJda().getGuilds().size()))
                    .append(" -- Users: ")
                    .append(fb.getJda().getUsers().size())
                    .append("\n");
            i++;
        }

        for(MessageBuilder builder : builders){
            builder.append("```");
            channel.sendMessage(builder.build()).queue();
        }
    }

    @Override
    public String help(Guild guild) {
        return "{0}{1}\n#Show information about the shards of the bot.";
    }
}
