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

import fredboat.feature.I18n;
import fredboat.shared.constant.BotConstants;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.requests.Request;
import net.dv8tion.jda.core.requests.Response;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.core.requests.Route;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Created by napster on 10.09.17.
 * <p>
 * Everything related to sending things out from FredBoat
 */
public class CentralMessaging {

    private static final Logger log = LoggerFactory.getLogger(CentralMessaging.class);

    //this is needed for when we absolutely don't care about a rest action failing (use this only after good consideration!)
    // because if we pass null for a failure handler to JDA it uses a default handler that results in a warning/error level log
    private static final Consumer<Throwable> NOOP_EXCEPTION_HANDLER = __ -> {
    };


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

    //presets fredboat color on a clear embed
    public static EmbedBuilder getColoredEmbedBuilder() {
        return getClearThreadLocalEmbedBuilder()
                .setColor(BotConstants.FREDBOAT_COLOR);
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

    //May not be an empty string, as MessageBuilder#build() will throw an exception
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
     * @return Future that can be waited on in case the code requires completion. Similar to JDA's RestAction#complete,
     * avoid usage where not absolutely needed.
     */
    public static MessageFuture sendMessage(@Nonnull MessageChannel channel, @Nonnull Message message,
                                            @Nullable Consumer<Message> onSuccess, @Nullable Consumer<Throwable> onFail) {
        return sendMessage0(
                channel,
                message,
                onSuccess,
                onFail
        );
    }

    // Message
    public static MessageFuture sendMessage(@Nonnull MessageChannel channel, @Nonnull Message message,
                                            @Nullable Consumer<Message> onSuccess) {
        return sendMessage0(
                channel,
                message,
                onSuccess,
                null
        );
    }

    // Message
    public static MessageFuture sendMessage(@Nonnull MessageChannel channel, @Nonnull Message message) {
        return sendMessage0(
                channel,
                message,
                null,
                null
        );
    }

    // Embed
    public static MessageFuture sendMessage(@Nonnull MessageChannel channel, @Nonnull MessageEmbed embed,
                                            @Nullable Consumer<Message> onSuccess, @Nullable Consumer<Throwable> onFail) {
        return sendMessage0(
                channel,
                from(embed),
                onSuccess,
                onFail
        );
    }

    // Embed
    public static MessageFuture sendMessage(@Nonnull MessageChannel channel, @Nonnull MessageEmbed embed,
                                            @Nullable Consumer<Message> onSuccess) {
        return sendMessage0(
                channel,
                from(embed),
                onSuccess,
                null
        );
    }

    // Embed
    public static MessageFuture sendMessage(@Nonnull MessageChannel channel, @Nonnull MessageEmbed embed) {
        return sendMessage0(
                channel,
                from(embed),
                null,
                null
        );
    }

    // String
    public static MessageFuture sendMessage(@Nonnull MessageChannel channel, @Nonnull String content,
                                            @Nullable Consumer<Message> onSuccess, @Nullable Consumer<Throwable> onFail) {
        return sendMessage0(
                channel,
                from(content),
                onSuccess,
                onFail
        );
    }

    // String
    public static MessageFuture sendMessage(@Nonnull MessageChannel channel, @Nonnull String content,
                                            @Nullable Consumer<Message> onSuccess) {
        return sendMessage0(
                channel,
                from(content),
                onSuccess,
                null
        );
    }

    // String
    public static MessageFuture sendMessage(@Nonnull MessageChannel channel, @Nonnull String content) {
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
     * @return Future that can be waited on in case the code requires completion. Similar to JDA's RestAction#complete,
     * avoid usage where not absolutely needed.
     */
    public static MessageFuture sendFile(@Nonnull MessageChannel channel, @Nonnull File file, @Nullable Message message,
                                         @Nullable Consumer<Message> onSuccess, @Nullable Consumer<Throwable> onFail) {
        return sendFile0(
                channel,
                file,
                message,
                onSuccess,
                onFail
        );
    }

    public static MessageFuture sendFile(@Nonnull MessageChannel channel, @Nonnull File file, @Nullable Message message,
                                         @Nullable Consumer<Message> onSuccess) {
        return sendFile0(
                channel,
                file,
                message,
                onSuccess,
                null
        );
    }

    public static MessageFuture sendFile(@Nonnull MessageChannel channel, @Nonnull File file, @Nullable Message message) {
        return sendFile0(
                channel,
                file,
                message,
                null,
                null
        );
    }

    public static MessageFuture sendFile(@Nonnull MessageChannel channel, @Nonnull File file,
                                         @Nullable Consumer<Message> onSuccess, @Nullable Consumer<Throwable> onFail) {
        return sendFile0(
                channel,
                file,
                null,
                onSuccess,
                onFail
        );
    }

    public static MessageFuture sendFile(@Nonnull MessageChannel channel, @Nonnull File file,
                                         @Nullable Consumer<Message> onSuccess) {
        return sendFile0(
                channel,
                file,
                null,
                onSuccess,
                null
        );
    }

    public static MessageFuture sendFile(@Nonnull MessageChannel channel, @Nonnull File file) {
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
     * @return Future that can be waited on in case the code requires completion. Similar to JDA's RestAction#complete,
     * avoid usage where not absolutely needed.
     */
    public static MessageFuture editMessage(@Nonnull Message oldMessage, @Nonnull Message newMessage,
                                            @Nullable Consumer<Message> onSuccess, @Nullable Consumer<Throwable> onFail) {
        return editMessage0(
                oldMessage.getChannel(),
                oldMessage.getIdLong(),
                newMessage,
                onSuccess,
                onFail
        );
    }

    public static MessageFuture editMessage(@Nonnull Message oldMessage, @Nonnull Message newMessage) {
        return editMessage0(
                oldMessage.getChannel(),
                oldMessage.getIdLong(),
                newMessage,
                null,
                null
        );
    }

    public static MessageFuture editMessage(@Nonnull Message oldMessage, @Nonnull String newContent) {
        return editMessage0(
                oldMessage.getChannel(),
                oldMessage.getIdLong(),
                from(newContent),
                null,
                null
        );
    }


    public static MessageFuture editMessage(@Nonnull MessageChannel channel, long oldMessageId, @Nonnull Message newMessage,
                                            @Nullable Consumer<Message> onSuccess, @Nullable Consumer<Throwable> onFail) {
        return editMessage0(
                channel,
                oldMessageId,
                newMessage,
                onSuccess,
                onFail
        );
    }

    public static MessageFuture editMessage(@Nonnull MessageChannel channel, long oldMessageId, @Nonnull Message newMessage) {
        return editMessage0(
                channel,
                oldMessageId,
                newMessage,
                null,
                null
        );
    }

    public static MessageFuture editMessage(@Nonnull MessageChannel channel, long oldMessageId, @Nonnull String newContent) {
        return editMessage0(
                channel,
                oldMessageId,
                from(newContent),
                null,
                null
        );
    }

    // ********************************************************************************
    //                   Miscellaneous messaging related methods
    // ********************************************************************************

    public static void sendTyping(MessageChannel channel) {
        try {
            channel.sendTyping().queue(
                    null,
                    t -> log.warn("Could not send typing event", t)
            );
        } catch (InsufficientPermissionException e) {
            handleInsufficientPermissionsException(channel, e);
        }
    }

    //make sure that all the messages are from the channel you provide
    public static void deleteMessages(@Nonnull TextChannel channel, @Nonnull Collection<Message> messages) {
        if (!messages.isEmpty()) {
            try {
                channel.deleteMessages(messages).queue(
                        null,
                        t -> log.warn("Could not bulk delete messages", t)
                );
            } catch (InsufficientPermissionException e) {
                handleInsufficientPermissionsException(channel, e);
            }
        }
    }

    public static void deleteMessageById(@Nonnull MessageChannel channel, long messageId) {
        try {
            channel.getMessageById(messageId).queue(
                    CentralMessaging::deleteMessage,
                    NOOP_EXCEPTION_HANDLER //do nothing if that message could not be found in the first place
            );
        } catch (InsufficientPermissionException e) {
            handleInsufficientPermissionsException(channel, e);
        }
    }

    public static void deleteMessage(@Nonnull Message message) {
        try {
            message.delete().queue(
                    null,
                    t -> log.warn("Could not delete message", t)
            );
        } catch (InsufficientPermissionException e) {
            handleInsufficientPermissionsException(message.getChannel(), e);
        }
    }

    @Nonnull
    public static EmbedBuilder addFooter(@Nonnull EmbedBuilder eb, @Nonnull Member author) {
        return eb.setFooter(author.getEffectiveName(), author.getUser().getAvatarUrl());
    }

    @Nonnull
    public static EmbedBuilder addNpFooter(@Nonnull EmbedBuilder eb, @Nonnull Member requester) {
        return eb.setFooter("Requested by " + requester.getEffectiveName(), requester.getUser().getAvatarUrl());
    }


    // ********************************************************************************
    //                           Class internal methods
    // ********************************************************************************

    //class internal message sending method
    private static MessageFuture sendMessage0(@Nonnull MessageChannel channel, @Nonnull Message message,
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

        try {
            channel.sendMessage(message).queue(successWrapper, failureWrapper);
        } catch (InsufficientPermissionException e) {
            failureWrapper.accept(e);
            if (e.getPermission() == Permission.MESSAGE_EMBED_LINKS) {
                handleInsufficientPermissionsException(channel, e);
            } else {
                //do not call CentralMessaging#handleInsufficientPermissionsException() from here as that will result in a loop
                log.warn("Could not send message to channel {} due to missing permission {}", channel.getIdLong(), e.getPermission().getName(), e);
            }
        }
        return result;
    }

    //class internal file sending method
    private static MessageFuture sendFile0(@Nonnull MessageChannel channel, @Nonnull File file, @Nullable Message message,
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

        try {
            // ATTENTION: Do not use JDA's MessageChannel#sendFile(File file, Message message)
            // as it will skip permission checks, since TextChannel does not override that method
            // this is scheduled to be fixed through JDA's message-rw branch
            channel.sendFile(FileUtils.readFileToByteArray(file), file.getName(), message).queue(successWrapper, failureWrapper);
        } catch (InsufficientPermissionException e) {
            failureWrapper.accept(e);
            handleInsufficientPermissionsException(channel, e);
        } catch (IOException e) {
            log.error("Could not send file {}, it appears to be borked", file.getAbsolutePath(), e);
        }
        return result;
    }

    //class internal editing method
    private static MessageFuture editMessage0(@Nonnull MessageChannel channel, long oldMessageId, @Nonnull Message newMessage,
                                              @Nullable Consumer<Message> onSuccess, @Nullable Consumer<Throwable> onFail) {
        if (channel == null) {
            throw new IllegalArgumentException("Channel is null");
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

        try {
            channel.editMessageById(oldMessageId, newMessage).queue(successWrapper, failureWrapper);
        } catch (InsufficientPermissionException e) {
            failureWrapper.accept(e);
            handleInsufficientPermissionsException(channel, e);
        }
        return result;
    }

    private static void handleInsufficientPermissionsException(@Nonnull MessageChannel channel,
                                                               @Nonnull InsufficientPermissionException e) {
        final ResourceBundle i18n;
        if (channel instanceof TextChannel) {
            i18n = I18n.get(((TextChannel) channel).getGuild());
        } else {
            i18n = I18n.DEFAULT.getProps();
        }
        //only ever try sending a simple string from here so we don't end up handling a loop of insufficient permissions
        sendMessage(channel, i18n.getString("permissionMissingBot") + " **" + e.getPermission().getName() + "**");
    }

}
