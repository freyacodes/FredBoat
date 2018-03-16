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

package fredboat.command.info;

import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IInfoCommand;
import fredboat.main.Launcher;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Message;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ShardsCommand extends Command implements IInfoCommand {

    private static final int SHARDS_PER_MESSAGE = 30;

    public ShardsCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        for (Message message : getShardStatus(context.msg)) {
            context.reply(message);
        }
    }

    public static List<Message> getShardStatus(@Nonnull Message input) {
        List<String> lines = new ArrayList<>();

        //do a full report? or just a summary
        String raw = input.getContentRaw().toLowerCase();
        boolean full = raw.contains("full") || raw.contains("all");

        AtomicInteger shardCounter = new AtomicInteger(0);
        AtomicInteger borkenShards = new AtomicInteger(0);
        AtomicLong healthyGuilds = new AtomicLong(0);
        AtomicLong healthyUsers = new AtomicLong(0);
        Launcher.getBotController().getJdaEntityProvider().streamShards().forEach(shard -> {
            shardCounter.incrementAndGet();
            if (shard.getStatus() == JDA.Status.CONNECTED && !full) {
                healthyGuilds.addAndGet(shard.getGuildCache().size());
                healthyUsers.addAndGet(shard.getUserCache().size());
            } else {
                //noinspection ConstantConditions
                lines.add((shard.getStatus() == JDA.Status.CONNECTED ? "+" : "-")
                        + " "
                        + shard.getShardInfo().getShardString()
                        + " "
                        + shard.getStatus()
                        + " -- Guilds: "
                        + String.format("%04d", shard.getGuildCache().size())
                        + " -- Users: "
                        + shard.getUserCache().size()
                        + "\n");
                borkenShards.incrementAndGet();
            }
        });

        List<Message> messages = new ArrayList<>();

        StringBuilder stringBuilder = new StringBuilder();
        int lineCounter = 0;
        for (String line : lines) {
            stringBuilder.append(line);
            lineCounter++;
            if (lineCounter % SHARDS_PER_MESSAGE == 0 || lineCounter == lines.size()) {
                messages.add(CentralMessaging.getClearThreadLocalMessageBuilder()
                        .appendCodeBlock(stringBuilder.toString(), "diff").build());
                stringBuilder = new StringBuilder();
            }
        }

        //healthy shards summary, contains sensible data only if we aren't doing a full report
        if (!full) {
            String content = String.format("+ %s of %s shards are %s -- Guilds: %s -- Users: %s", (shardCounter.get() - borkenShards.get()),
                    Launcher.getBotController().getCredentials().getRecommendedShardCount(), JDA.Status.CONNECTED, healthyGuilds, healthyUsers);
            messages.add(0, CentralMessaging.getClearThreadLocalMessageBuilder().append(TextUtils.asCodeBlock(content, "diff")).build());
        }

        return messages;
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} [full]\n#Show information about the shards of the bot as a summary or in a detailed report.";
    }
}
