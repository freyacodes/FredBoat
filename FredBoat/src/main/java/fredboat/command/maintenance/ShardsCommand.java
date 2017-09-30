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

import fredboat.Config;
import fredboat.FredBoat;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IMaintenanceCommand;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.impl.JDAImpl;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ShardsCommand extends Command implements IMaintenanceCommand {

    private static final int SHARDS_PER_MESSAGE = 30;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        MessageBuilder mb = null;
        List<Message> messages = new ArrayList<>();

        //do a full report? or just a summary
        boolean full = false;
        String[] args = context.args;
        if (args.length > 1 && ("full".equals(args[1]) || "all".equals(args[1]))) {
            full = true;
        }

        //make a copy to avoid concurrent modification errors
        List<FredBoat> shards = new ArrayList<>(FredBoat.getShards());
        int borkenShards = 0;
        int healthyGuilds = 0;
        int healthyUsers = 0;
        for (FredBoat fb : shards) {
            if (fb.getJda().getStatus() == JDA.Status.CONNECTED && !full) {
                healthyGuilds += fb.getGuildCount();
                // casting to get the underlying map, this is safe because we only need the .size()
                healthyUsers += ((JDAImpl) fb.getJda()).getUserMap().size();
            } else {
                if (borkenShards % SHARDS_PER_MESSAGE == 0) {
                    if (mb != null) {
                        mb.append("```");
                        messages.add(mb.build());
                    }
                    mb = CentralMessaging.getClearThreadLocalMessageBuilder().append("```diff\n");
                }
                mb.append(fb.getJda().getStatus() == JDA.Status.CONNECTED ? "+" : "-")
                        .append(" ")
                        .append(fb.getShardInfo().getShardString())
                        .append(" ")
                        .append(fb.getJda().getStatus())
                        .append(" -- Guilds: ")
                        .append(String.format("%04d", fb.getGuildCount()))
                        .append(" -- Users: ")
                        .append(fb.getUserCount())
                        .append("\n");
                borkenShards++;
            }
        }
        if (mb != null && borkenShards % SHARDS_PER_MESSAGE != 0) {
            mb.append("```");
            messages.add(mb.build());
        }

        //healthy shards summary, contains sensible data only if we aren't doing a full report
        if (!full) {
            context.reply("```diff\n+ "
                    + (shards.size() - borkenShards) + "/" + Config.CONFIG.getNumShards() + " shards are " + JDA.Status.CONNECTED
                    + " -- Guilds: " + healthyGuilds + " -- Users: " + healthyUsers + "\n```");
        }

        //detailed shards
        for (Message message : messages) {
            context.reply(message);
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} [full]\n#Show information about the shards of the bot as a summary or in a detailed report.";
    }
}
