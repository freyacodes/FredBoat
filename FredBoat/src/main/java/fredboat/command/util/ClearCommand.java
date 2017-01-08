/*
 * MIT License
 *
 * Copyright (c) 2016 Frederik Ar. Mikkelsen
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

package fredboat.command.util;

import fredboat.commandmeta.MessagingException;
import fredboat.commandmeta.abs.Command;
import fredboat.util.BotConstants;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.MessageHistory;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.utils.PermissionUtil;

import java.util.ArrayList;
import java.util.List;

public class ClearCommand extends Command {

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        JDA jda = guild.getJDA();
        
        if(!PermissionUtil.checkPermission(channel, invoker, Permission.MESSAGE_MANAGE) && !invoker.getUser().getId().equals(BotConstants.OWNER_ID)){
            TextUtils.replyWithName(channel, invoker, " You must have Manage Messages to do that!");
            return;
        }
        
        MessageHistory history = new MessageHistory(channel);
        List<Message> msgs;
        try {
            msgs = history.retrievePast(50).complete(true);

            ArrayList<Message> myMessages = new ArrayList<>();

            for (Message msg : msgs) {
                if(msg.getAuthor().equals(jda.getSelfUser())){
                    myMessages.add(msg);
                }
            }

            if(myMessages.isEmpty()){
                throw new MessagingException("No messages found.");
            } else if(myMessages.size() == 1) {
                myMessages.get(0).deleteMessage().complete(true);
                channel.sendMessage("Deleted one message.").queue();
            } else {

                if(PermissionUtil.checkPermission(channel, guild.getSelfMember(), Permission.MESSAGE_MANAGE)){
                    throw new MessagingException("I must have the `Manage Messages` permission to delete my own messages in bulk.");
                }

                channel.deleteMessages(myMessages).complete(true);
                channel.sendMessage("Deleted **" + myMessages.size() + "** messages.").queue();
            }
        } catch (RateLimitedException e) {
            throw new RuntimeException(e);
        }
    }
}
