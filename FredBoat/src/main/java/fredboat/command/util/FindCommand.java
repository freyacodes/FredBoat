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
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.MessageHistory;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.impl.MemberImpl;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;

public class FindCommand extends Command {

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        JDA jda = guild.getJDA();

        String searchTerm = null;
        try {
            searchTerm = args[1].toLowerCase();
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new MessagingException("Correct usage: ;;find <text> [#channel]");
        }
        TextChannel selected = channel;
        if (!message.getMentionedChannels().isEmpty()) {
            selected = message.getMentionedChannels().get(0);
        }
        int toSearch = 1500;
        channel.sendTyping().queue();

        Message startMsg = new MessageBuilder().append(invoker)
                .append(" searching in ")
                .append(String.valueOf(toSearch), MessageBuilder.Formatting.BLOCK)
                .append(" messages from ")
                .append("#" + selected.getName(), MessageBuilder.Formatting.BLOCK)
                .append(" containing ")
                .append(searchTerm, MessageBuilder.Formatting.BLOCK)
                .append(".")
                .build();

        MessageHistory history = new MessageHistory(selected);
        ArrayList<Message> msgs = new ArrayList<>();

        try {
            for (int i = 0; i < Math.ceil(toSearch / 100); i++) {
                try {
                    msgs.addAll(history.retrieveFuture(Math.min(100, toSearch - (i * 100))).block());
                } catch (RateLimitedException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (NullPointerException ex) {//End of chat - ignore
        }

        channel.sendMessage(startMsg).queue();
        ArrayList<Message> matches = new ArrayList<>();

        for (Message msg : msgs) {
            if (msg.getContent().toLowerCase().contains(searchTerm) && !msg.equals(message)) {
                matches.add(msg);
            }
        }

        MessageBuilder endMsg = new MessageBuilder();
        endMsg.append("Found a total of ")
                .append(String.valueOf(matches.size()), MessageBuilder.Formatting.BOLD)
                .append(" matches:");

        int i = 0;
        int truncated = 0;
        for (Message msg : matches) {
            i++;
            if (endMsg.length() > 1600 || msg.getContent().length() > 400) {
                truncated++;
            } else {
                try {
                    endMsg.append("\n")
                            .append("[" + forceTwoDigits(i) + "] " + formatTimestamp(msg.getCreationTime()), MessageBuilder.Formatting.BLOCK)
                            .append(" ")
                            .append(new MemberImpl(msg.getGuild(), msg.getAuthor()).getEffectiveName(), MessageBuilder.Formatting.BOLD)
                            .append(" ")
                            .append(msg.getContent());
                } catch (NullPointerException e) {
                    endMsg.append("\n")
                            .append("[" + forceTwoDigits(i) + "] " + formatTimestamp(msg.getCreationTime()), MessageBuilder.Formatting.BLOCK)
                            .append(" ")
                            .append("[Got NullPointerException]", MessageBuilder.Formatting.BOLD)
                            .append(" ")
                            .append("[Got NullPointerException]", MessageBuilder.Formatting.BLOCK);
                }
            }
        }

        if (truncated > 0) {
            endMsg.append("\n[Truncated " + truncated + "]");
        }
        channel.sendMessage(endMsg.build()).queue();

    }
    
    private String forceTwoDigits(int i){
        String str = String.valueOf(i);
        
        if (str.length() == 1){
            str = "0" + str;
        }
        
        return str;
    }

    private String formatTimestamp(OffsetDateTime t) {
        String str;
        if(LocalDateTime.now(Clock.systemUTC()).getDayOfYear() != t.getDayOfYear()){
            str = "[" + t.getMonth().name().substring(0, 3).toLowerCase() + " " + t.getDayOfMonth() + " " + forceTwoDigits(t.getHour()) + ":" + forceTwoDigits(t.getMinute()) + "]";
        } else {
            str = "[" + forceTwoDigits(t.getHour()) + ":" + forceTwoDigits(t.getMinute()) + "]";
        }
        return str;
    }
}
