/*
 *
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
 */

package fredboat.messaging;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.requests.Request;
import net.dv8tion.jda.core.requests.Response;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.core.requests.Route;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.File;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * Created by napster on 10.09.17.
 * <p>
 * Everything related to sending things out from FredBoat
 */
public class CentralMessaging {


    // ********************************************************************************
    //       Thread local handling and providing of Messages and Embeds builders
    // ********************************************************************************

    //instead of creating millions of MessageBuilder and EmbedBuilder objects we're going to reuse the existing ones, on
    // a per-thread scope
    // this makes sense since the vast majority of message processing in FredBoat is happening in the main JDA threads

    private static ThreadLocal<MessageBuilder> threadLocalMessageBuilder = ThreadLocal.withInitial(MessageBuilder::new);
    private static ThreadLocal<EmbedBuilder> threadLocalEmbedBuilder = ThreadLocal.withInitial(EmbedBuilder::new);

    public static MessageBuilder getClearThreadLocalMessageBuilder() {
        return threadLocalMessageBuilder.get().clear();
    }

    public static EmbedBuilder getClearThreadLocalEmbedBuilder() {
        return threadLocalEmbedBuilder.get()
                .clearFields()
                .setTitle(null)
                .setDescription(null)
                .setTimestamp(null)
                .setColor(null)
                .setThumbnail(null)
                .setAuthor(null, null, null)
                .setFooter(null, null)
                .setImage(null);
    }

    public static Message from(String string) {
        return getClearThreadLocalMessageBuilder().append(string).build();
    }

    public static Message from(MessageEmbed embed) {
        return getClearThreadLocalMessageBuilder().setEmbed(embed).build();
    }


    // ********************************************************************************
    //       Convenience methods that convert input to Message objects and send it
    // ********************************************************************************

    /**
     * Combines the benefits of JDAs RestAction#queue and returning a Future that can be get() if necessary
     *
     * @param channel   The channel that should be messaged
     * @param message   Message to be sent
     * @param onSuccess Optional success handler
     * @param onFail    Optional exception handler
     * @return Future that can be waited on in case the code requires completion. Similar to JDA's RestAction#queue, avoid usage where not absolutely needed.
     */
    public static MessageFuture sendMessage(@NotNull MessageChannel channel, @NotNull Message message,
                                            @Nullable Consumer<Message> onSuccess, @Nullable Consumer<Throwable> onFail) {
        return sendMessage0(
                channel,
                message,
                onSuccess,
                onFail
        );
    }

    // Message
    public static MessageFuture sendMessage(@NotNull MessageChannel channel, @NotNull Message message,
                                            @Nullable Consumer<Message> onSuccess) {
        return sendMessage0(
                channel,
                message,
                onSuccess,
                null
        );
    }

    // Message
    public static MessageFuture sendMessage(@NotNull MessageChannel channel, @NotNull Message message) {
        return sendMessage0(
                channel,
                message,
                null,
                null
        );
    }

    // Embed
    public static MessageFuture sendMessage(@NotNull MessageChannel channel, @NotNull MessageEmbed embed,
                                            @Nullable Consumer<Message> onSuccess, @Nullable Consumer<Throwable> onFail) {
        return sendMessage0(
                channel,
                from(embed),
                onSuccess,
                onFail
        );
    }

    // Embed
    public static MessageFuture sendMessage(@NotNull MessageChannel channel, @NotNull MessageEmbed embed,
                                            @Nullable Consumer<Message> onSuccess) {
        return sendMessage0(
                channel,
                from(embed),
                onSuccess,
                null
        );
    }

    // Embed
    public static MessageFuture sendMessage(@NotNull MessageChannel channel, @NotNull MessageEmbed embed) {
        return sendMessage0(
                channel,
                from(embed),
                null,
                null
        );
    }

    // String
    public static MessageFuture sendMessage(@NotNull MessageChannel channel, @NotNull String content,
                                            @Nullable Consumer<Message> onSuccess, @Nullable Consumer<Throwable> onFail) {
        return sendMessage0(
                channel,
                from(content),
                onSuccess,
                onFail
        );
    }

    // String
    public static MessageFuture sendMessage(@NotNull MessageChannel channel, @NotNull String content,
                                            @Nullable Consumer<Message> onSuccess) {
        return sendMessage0(
                channel,
                from(content),
                onSuccess,
                null
        );
    }

    // String
    public static MessageFuture sendMessage(@NotNull MessageChannel channel, @NotNull String content) {
        return sendMessage0(
                channel,
                from(content),
                null,
                null
        );
    }

    // for the adventurers among us
    public static void sendShardlessMessage(long channelId, Message msg) {
        sendShardlessMessage(msg.getJDA(), channelId, msg.getRawContent());
    }

    // for the adventurers among us
    public static void sendShardlessMessage(JDA jda, long channelId, String content) {
        JSONObject body = new JSONObject();
        body.put("content", content);
        new RestAction<Void>(jda, Route.Messages.SEND_MESSAGE.compile(Long.toString(channelId)), body) {
            @Override
            protected void handleResponse(Response response, Request<Void> request) {
                if (response.isOk())
                    request.onSuccess(null);
                else
                    request.onFailure(response);
            }
        }.queue();
    }

    // ********************************************************************************
    //                            File sending methods
    // ********************************************************************************

    /**
     * Combines the benefits of JDAs RestAction#queue and returning a Future that can be get() if necessary
     *
     * @param channel   The channel that should be messaged
     * @param file      File to be sent
     * @param message   Optional message
     * @param onSuccess Optional success handler
     * @param onFail    Optional exception handler
     * @return Future that can be waited on in case the code requires completion. Similar to JDA's RestAction#queue, avoid usage where not absolutely needed.
     */
    public static MessageFuture sendFile(@NotNull MessageChannel channel, @NotNull File file, @Nullable Message message,
                                         @Nullable Consumer<Message> onSuccess, @Nullable Consumer<Throwable> onFail) {
        return sendFile0(
                channel,
                file,
                message,
                onSuccess,
                onFail
        );
    }

    public static MessageFuture sendFile(@NotNull MessageChannel channel, @NotNull File file, @Nullable Message message,
                                         @Nullable Consumer<Message> onSuccess) {
        return sendFile0(
                channel,
                file,
                message,
                onSuccess,
                null
        );
    }

    public static MessageFuture sendFile(@NotNull MessageChannel channel, @NotNull File file, @Nullable Message message) {
        return sendFile0(
                channel,
                file,
                message,
                null,
                null
        );
    }

    public static MessageFuture sendFile(@NotNull MessageChannel channel, @NotNull File file,
                                         @Nullable Consumer<Message> onSuccess, @Nullable Consumer<Throwable> onFail) {
        return sendFile0(
                channel,
                file,
                null,
                onSuccess,
                onFail
        );
    }

    public static MessageFuture sendFile(@NotNull MessageChannel channel, @NotNull File file,
                                         @Nullable Consumer<Message> onSuccess) {
        return sendFile0(
                channel,
                file,
                null,
                onSuccess,
                null
        );
    }

    public static MessageFuture sendFile(@NotNull MessageChannel channel, @NotNull File file) {
        return sendFile0(
                channel,
                file,
                null,
                null,
                null
        );
    }


    // ********************************************************************************
    //                            Message editing methods
    // ********************************************************************************

    /**
     * Combines the benefits of JDAs RestAction#queue and returning a Future that can be get() if necessary
     *
     * @param oldMessage The message to be edited
     * @param newMessage The message to be set
     * @param onSuccess  Optional success handler
     * @param onFail     Optional exception handler
     * @return Future that can be waited on in case the code requires completion. Similar to JDA's RestAction#queue, avoid usage where not absolutely needed.
     */
    public static MessageFuture editMessage(@NotNull Message oldMessage, @NotNull Message newMessage,
                                            @Nullable Consumer<Message> onSuccess, @Nullable Consumer<Throwable> onFail) {
        return editMessage0(
                oldMessage,
                newMessage,
                onSuccess,
                onFail
        );
    }

    public static MessageFuture editMessage(@NotNull Message oldMessage, @NotNull Message newMessage) {
        return editMessage0(
                oldMessage,
                newMessage,
                null,
                null
        );
    }

    public static MessageFuture editMessage(@NotNull Message oldMessage, @NotNull String newContent) {
        return editMessage0(
                oldMessage,
                from(newContent),
                null,
                null
        );
    }

    public static void editMessageById(MessageChannel channel, long oldMessageId, Message newMessage) {
        channel.editMessageById(oldMessageId, newMessage).queue();
    }

    // ********************************************************************************
    //                   Miscellaneous messaging related methods
    // ********************************************************************************

    public static void sendTyping(MessageChannel channel) {
        channel.sendTyping().queue();
    }

    //messages must all be from the same channel
    public static void deleteMessages(Collection<Message> messages) {
        if (!messages.isEmpty()) {
            MessageChannel channel = messages.iterator().next().getChannel();
            if (channel instanceof TextChannel) {
                ((TextChannel) channel).deleteMessages(messages).queue();
            } else {
                messages.forEach(m -> channel.deleteMessageById(m.getIdLong()).queue());
            }
        }
    }

    public static void deleteMessage(Message message) {
        message.delete().queue();
    }

    public static void deleteMessageById(MessageChannel channel, long messageId) {
        channel.deleteMessageById(messageId).queue();
    }

    public static EmbedBuilder addFooter(EmbedBuilder eb, Member author) {
        return eb.setFooter(author.getEffectiveName(), author.getUser().getAvatarUrl());
    }


    // ********************************************************************************
    //                           Class internal methods
    // ********************************************************************************

    //class internal message sending method
    private static MessageFuture sendMessage0(@NotNull MessageChannel channel, @NotNull Message message,
                                              @Nullable Consumer<Message> onSuccess, @Nullable Consumer<Throwable> onFail) {
        if (channel == null) {
            throw new IllegalArgumentException("Channel is null");
        }
        if (message == null) {
            throw new IllegalArgumentException("Message is null");
        }

        MessageFuture result = new MessageFuture();
        Consumer<Message> successWrapper = m -> {
            result.complete(m);
            if (onSuccess != null) {
                onSuccess.accept(m);
            }
        };
        Consumer<Throwable> failureWrapper = t -> {
            result.completeExceptionally(t);
            if (onFail != null) {
                onFail.accept(t);
            }
        };

        channel.sendMessage(message).queue(successWrapper, failureWrapper);
        return result;
    }

    //class internal file sending method
    private static MessageFuture sendFile0(@NotNull MessageChannel channel, @NotNull File file, @Nullable Message message,
                                           @Nullable Consumer<Message> onSuccess, @Nullable Consumer<Throwable> onFail) {
        if (channel == null) {
            throw new IllegalArgumentException("Channel is null");
        }
        if (file == null) {
            throw new IllegalArgumentException("File is null");
        }

        MessageFuture result = new MessageFuture();
        Consumer<Message> successWrapper = m -> {
            result.complete(m);
            if (onSuccess != null) {
                onSuccess.accept(m);
            }
        };
        Consumer<Throwable> failureWrapper = t -> {
            result.completeExceptionally(t);
            if (onFail != null) {
                onFail.accept(t);
            }
        };

        channel.sendFile(file, message).queue(successWrapper, failureWrapper);
        return result;
    }

    //class internal editing method
    private static MessageFuture editMessage0(@NotNull Message oldMessage, @NotNull Message newMessage,
                                              @Nullable Consumer<Message> onSuccess, @Nullable Consumer<Throwable> onFail) {
        if (oldMessage == null) {
            throw new IllegalArgumentException("Old message is null");
        }
        if (newMessage == null) {
            throw new IllegalArgumentException("New message is null");
        }

        MessageFuture result = new MessageFuture();
        Consumer<Message> successWrapper = m -> {
            result.complete(m);
            if (onSuccess != null) {
                onSuccess.accept(m);
            }
        };
        Consumer<Throwable> failureWrapper = t -> {
            result.completeExceptionally(t);
            if (onFail != null) {
                onFail.accept(t);
            }
        };

        oldMessage.editMessage(newMessage).queue(successWrapper, failureWrapper);
        return result;
    }

}
