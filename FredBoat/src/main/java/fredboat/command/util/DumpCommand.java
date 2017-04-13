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

import com.mashape.unirest.http.exceptions.UnirestException;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.IUtilCommand;
import fredboat.util.DiscordUtil;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.entities.Message.Attachment;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DumpCommand extends Command implements IUtilCommand {

    private static final int MAX_DUMP_SIZE = 2000;

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        //Interpret arguments
        boolean isQuiet = args[1].equals("-q");
        int dumpSize = Integer.valueOf(args[args.length - 1]);
        int realDumpSize = Math.min(dumpSize, MAX_DUMP_SIZE);

        if(!invoker.getUser().hasPrivateChannel()){
            try {
                invoker.getUser().openPrivateChannel().complete(true);
            } catch (RateLimitedException e) {
                throw new RuntimeException(e);
            }
        }

        MessageChannel outputChannel = isQuiet ? invoker.getUser().getPrivateChannel() : channel;
        outputChannel.sendTyping().queue();
        
        //Quick hack to allow infinite messages if invoked by owner:
        if(invoker.getUser().getId().equals(DiscordUtil.getOwnerId(invoker.getJDA()))){
            realDumpSize = dumpSize;
        }
        
        try {

            MessageHistory mh = new MessageHistory(channel);
            int availableMessages = mh.getCachedHistory().size();

            while (availableMessages < realDumpSize) {
                int nextMessages = Math.min(100, realDumpSize - availableMessages);
                availableMessages = nextMessages + availableMessages;
                mh.retrievePast(nextMessages).complete(true);
            }

            String dump = "**------BEGIN DUMP------**\n";
            List<Message> messages = new ArrayList<>(mh.getCachedHistory());
            Collections.reverse(messages);
            messages = messages.subList(0, Math.min(realDumpSize, messages.size()));
            dump = dump + "Size = " + messages.size() + "\nTimes are in UTC\n\n";

            int i = 1;
            for (Message msg : messages) {
                String authr = "[UNKNOWN USER]";
                String time = "[UNKNOWN TIME]";
                String content = "[COULD NOT DISPLAY CONTENT!]";

                try {
                    authr = msg.getAuthor().getName() + "#" + msg.getAuthor().getDiscriminator();
                } catch (NullPointerException ignored) {
                }
                
                try {
                    time = formatTimestamp(msg.getCreationTime());
                } catch (NullPointerException ignored) {
                }
                
                try {
                    content = msg.getContent();
                } catch (NullPointerException ignored) {
                }

                dump = dump + "--Msg #" + i + " by " + authr
                        + " at " + time + "--\n" + content + "\n";
                if (msg.getAttachments().size() > 0) {
                    dump = dump + "Attachments:\n";
                    int j = 1;
                    for (Attachment attach : msg.getAttachments()) {
                        dump = dump + "[" + j + "] " + attach.getUrl();
                    }
                }
                dump = dump + "\n\n";
                i++;
            }
            dump = dump + "**------END DUMP------**\n";

            MessageBuilder mb = new MessageBuilder();
            mb.append("Successfully found and dumped `" + messages.size() + "` messages.\n");
            mb.append(TextUtils.postToHastebin(dump, true) + ".txt\n");
            if (!isQuiet) {
                mb.append("Hint: You can call this with `-q` to instead get the dump in a DM\n");
            }
            outputChannel.sendMessage(mb.build()).queue();
        } catch (UnirestException ex) {
            outputChannel.sendMessage("Failed to connect to Hastebin: " + ex.getMessage()).queue();
        } catch (RateLimitedException e) {
            throw new RuntimeException(e);
        }
    }

    private String formatTimestamp(OffsetDateTime t) {
        String str;
        if (LocalDateTime.now(Clock.systemUTC()).getDayOfYear() != t.getDayOfYear()) {
            str = "[" + t.getMonth().name().substring(0, 3).toLowerCase() + " " + t.getDayOfMonth() + " " + forceTwoDigits(t.getHour()) + ":" + forceTwoDigits(t.getMinute()) + "]";
        } else {
            str = "[" + forceTwoDigits(t.getHour()) + ":" + forceTwoDigits(t.getMinute()) + "]";
        }
        return str;
    }

    private String forceTwoDigits(int i) {
        String str = String.valueOf(i);

        if (str.length() == 1) {
            str = "0" + str;
        }

        return str;
    }

    @Override
    public String help(Guild guild) {
        return "{0}{1} <1-2000>\n#Dumps between 1 and 2000 messages to Hastebin.";
    }
}
