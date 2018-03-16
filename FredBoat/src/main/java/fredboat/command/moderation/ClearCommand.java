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

package fredboat.command.moderation;

import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IModerationCommand;
import fredboat.feature.metrics.Metrics;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.definitions.PermissionLevel;
import fredboat.perms.PermsUtil;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.entities.TextChannel;

import javax.annotation.Nonnull;
import java.time.OffsetDateTime;
import java.util.ArrayList;

public class ClearCommand extends Command implements IModerationCommand {

    public ClearCommand(String name, String... aliases) {
        super(name, aliases);
    }

    //TODO: Redo this
    //TODO: i18n this class
    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        JDA jda = context.guild.getJDA();
        TextChannel channel = context.channel;
        Member invoker = context.invoker;

        if (!invoker.hasPermission(channel, Permission.MESSAGE_MANAGE)
                && !PermsUtil.checkPerms(PermissionLevel.BOT_ADMIN, invoker)) {
            context.replyWithName("You must have Manage Messages to do that!");
            return;
        }

        if (!context.guild.getSelfMember().hasPermission(channel, Permission.MESSAGE_HISTORY)) {
            context.reply(context.i18n("permissionMissingBot") + " **" + Permission.MESSAGE_HISTORY.getName() + "**");
            return;
        }

        MessageHistory history = new MessageHistory(channel);
        history.retrievePast(50).queue(msgs -> {
                    Metrics.successfulRestActions.labels("retrieveMessageHistory").inc();
                    ArrayList<Message> toDelete = new ArrayList<>();

                    for (Message msg : msgs) {
                        if (msg.getAuthor().equals(jda.getSelfUser())
                                && youngerThanTwoWeeks(msg)) {
                            toDelete.add(msg);
                        }
                    }

                    if (toDelete.isEmpty()) {
                        context.reply("No messages found.");
                    } else if (toDelete.size() == 1) {
                        context.reply("Found one message, deleting.");
                        CentralMessaging.deleteMessage(toDelete.get(0));
                    } else {

                        if (!context.hasPermissions(Permission.MESSAGE_MANAGE)) {
                            context.reply("I must have the `Manage Messages` permission to delete my own messages in bulk.");
                            return;
                        }

                        context.reply("Deleting **" + toDelete.size() + "** messages.");
                        CentralMessaging.deleteMessages(channel, toDelete);
                    }
                },
                CentralMessaging.getJdaRestActionFailureHandler(
                        String.format("Failed to retrieve message history in channel %s in guild %s",
                                channel.getId(), context.guild.getId())
                )
        );
    }

    private boolean youngerThanTwoWeeks(@Nonnull Message msg) {
        return msg.getCreationTime().isAfter(OffsetDateTime.now().minusWeeks(2)
                .plusMinutes(2));//some tolerance
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1}\n#" + context.i18n("helpClearCommand");
    }
}
