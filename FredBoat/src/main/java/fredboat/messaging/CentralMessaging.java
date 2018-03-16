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
import fredboat.feature.metrics.Metrics;
import fredboat.shared.constant.BotConstants;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.requests.ErrorResponse;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

/**
 * Created by napster on 10.09.17.
 * <p>
 * Everything related to sending RestActions from FredBoat
 */
@SuppressWarnings("UnusedReturnValue")
public class CentralMessaging {

    @Nonnull
    private static final Logger log = LoggerFactory.getLogger(CentralMessaging.class);

    //this is needed for when we absolutely don't care about a rest action failing (use this only after good consideration!)
    // because if we pass null for a failure handler to JDA it uses a default handler that results in a warning/error level log
    @Nonnull
    public static final Consumer<Throwable> NOOP_EXCEPTION_HANDLER = __ -> {
    };

    //use this to schedule rest actions whenever queueAfter() or similar JDA methods would be used
    // this makes it way easier to track stats + handle failures of such delayed RestActions
    // instead of implementing a ton of overloaded methods in this class
    @Nonnull
    public static final ScheduledExecutorService restService = Executors.newScheduledThreadPool(10,
            runnable -> new Thread(runnable, "central-messaging-scheduler"));


    // ********************************************************************************
    //       Thread local handling and providing of Messages and Embeds builders
    // ********************************************************************************

    //instead of creating millions of MessageBuilder and EmbedBuilder objects we're going to reuse the existing ones, on
    // a per-thread scope
    // this makes sense since the vast majority of message processing in FredBoat is happening in the main JDA threads

    @Nonnull
    private static ThreadLocal<MessageBuilder> threadLocalMessageBuilder = ThreadLocal.withInitial(MessageBuilder::new);
    @Nonnull
    private static ThreadLocal<EmbedBuilder> threadLocalEmbedBuilder = ThreadLocal.withInitial(EmbedBuilder::new);

    @Nonnull
    public static MessageBuilder getClearThreadLocalMessageBuilder() {
        return threadLocalMessageBuilder.get().clear();
    }

    //presets fredboat color on a clear embed
    @Nonnull
    public static EmbedBuilder getColoredEmbedBuilder() {
        return getClearThreadLocalEmbedBuilder()
                .setColor(BotConstants.FREDBOAT_COLOR);
    }

    @Nonnull
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
    @Nonnull
    public static Message from(@Nonnull String string) {
        return getClearThreadLocalMessageBuilder().append(string).build();
    }

    @Nonnull
    public static Message from(@Nonnull MessageEmbed embed) {
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
    @Nonnull
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
    @Nonnull
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
    @Nonnull
    public static MessageFuture sendMessage(@Nonnull MessageChannel channel, @Nonnull Message message) {
        return sendMessage0(
                channel,
                message,
                null,
                null
        );
    }

    // Embed
    @Nonnull
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
    @Nonnull
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
    @Nonnull
    public static MessageFuture sendMessage(@Nonnull MessageChannel channel, @Nonnull MessageEmbed embed) {
        return sendMessage0(
                channel,
                from(embed),
                null,
                null
        );
    }

    // String
    @Nonnull
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
    @Nonnull
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
    @Nonnull
    public static MessageFuture sendMessage(@Nonnull MessageChannel channel, @Nonnull String content) {
        return sendMessage0(
                channel,
                from(content),
                null,
                null
        );
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
    @Nonnull
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

    @Nonnull
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

    @Nonnull
    public static MessageFuture sendFile(@Nonnull MessageChannel channel, @Nonnull File file, @Nullable Message message) {
        return sendFile0(
                channel,
                file,
                message,
                null,
                null
        );
    }

    @Nonnull
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

    @Nonnull
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

    @Nonnull
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
    @Nonnull
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

    @Nonnull
    public static MessageFuture editMessage(@Nonnull Message oldMessage, @Nonnull Message newMessage) {
        return editMessage0(
                oldMessage.getChannel(),
                oldMessage.getIdLong(),
                newMessage,
                null,
                null
        );
    }

    @Nonnull
    public static MessageFuture editMessage(@Nonnull Message oldMessage, @Nonnull String newContent) {
        return editMessage0(
                oldMessage.getChannel(),
                oldMessage.getIdLong(),
                from(newContent),
                null,
                null
        );
    }

    @Nonnull
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

    @Nonnull
    public static MessageFuture editMessage(@Nonnull MessageChannel channel, long oldMessageId, @Nonnull Message newMessage) {
        return editMessage0(
                channel,
                oldMessageId,
                newMessage,
                null,
                null
        );
    }

    @Nonnull
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

    public static void sendTyping(@Nonnull MessageChannel channel) {
        try {
            channel.sendTyping().queue(
                    __ -> Metrics.successfulRestActions.labels("sendTyping").inc(),
                    getJdaRestActionFailureHandler("Could not send typing event in channel " + channel.getId())
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
                        __ -> Metrics.successfulRestActions.labels("bulkDeleteMessages").inc(),
                        getJdaRestActionFailureHandler(String.format("Could not bulk delete %s messages in channel %s",
                                messages.size(), channel.getId()))
                );
            } catch (InsufficientPermissionException e) {
                handleInsufficientPermissionsException(channel, e);
            }
        }
    }

    public static void deleteMessageById(@Nonnull MessageChannel channel, long messageId) {
        try {
            channel.getMessageById(messageId).queue(
                    message -> {
                        Metrics.successfulRestActions.labels("getMessageById").inc();
                        CentralMessaging.deleteMessage(message);
                    },
                    NOOP_EXCEPTION_HANDLER //prevent logging an error if that message could not be found in the first place
            );
        } catch (InsufficientPermissionException e) {
            handleInsufficientPermissionsException(channel, e);
        }
    }

    //make sure that the message passed in here is actually existing in Discord
    // e.g. dont pass messages in here that were created with a MessageBuilder in our code
    public static void deleteMessage(@Nonnull Message message) {
        try {
            message.delete().queue(
                    __ -> Metrics.successfulRestActions.labels("deleteMessage").inc(),
                    getJdaRestActionFailureHandler(String.format("Could not delete message %s in channel %s with content\n%s",
                            message.getId(), message.getChannel().getId(), message.getContentRaw()))
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
    @Nonnull
    private static MessageFuture sendMessage0(@Nonnull MessageChannel channel, @Nonnull Message message,
                                              @Nullable Consumer<Message> onSuccess, @Nullable Consumer<Throwable> onFail) {
        MessageFuture result = new MessageFuture();
        Consumer<Message> successWrapper = m -> {
            result.complete(m);
            Metrics.successfulRestActions.labels("sendMessage").inc();
            if (onSuccess != null) {
                onSuccess.accept(m);
            }
        };
        Consumer<Throwable> failureWrapper = t -> {
            result.completeExceptionally(t);
            if (onFail != null) {
                onFail.accept(t);
            } else {
                String info = String.format("Could not sent message\n%s\nto channel %s in guild %s",
                        message.getContentRaw(), channel.getId(),
                        (channel instanceof TextChannel) ? ((TextChannel) channel).getGuild().getIdLong() : "null");
                getJdaRestActionFailureHandler(info).accept(t);
            }
        };

        try {
            channel.sendMessage(message).queue(successWrapper, failureWrapper);
        } catch (InsufficientPermissionException e) {
            if (onFail != null) {
                onFail.accept(e);
            }
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
    @Nonnull
    private static MessageFuture sendFile0(@Nonnull MessageChannel channel, @Nonnull File file, @Nullable Message message,
                                           @Nullable Consumer<Message> onSuccess, @Nullable Consumer<Throwable> onFail) {

        MessageFuture result = new MessageFuture();
        Consumer<Message> successWrapper = m -> {
            result.complete(m);
            Metrics.successfulRestActions.labels("sendFile").inc();
            if (onSuccess != null) {
                onSuccess.accept(m);
            }
        };
        Consumer<Throwable> failureWrapper = t -> {
            result.completeExceptionally(t);
            if (onFail != null) {
                onFail.accept(t);
            } else {
                String info = String.format("Could not send file %s to channel %s in guild %s",
                        file.getAbsolutePath(), channel.getId(),
                        (channel instanceof TextChannel) ? ((TextChannel) channel).getGuild().getIdLong() : "null");
                getJdaRestActionFailureHandler(info).accept(t);
            }
        };

        try {
            // ATTENTION: Do not use JDA's MessageChannel#sendFile(File file, Message message)
            // as it will skip permission checks, since TextChannel does not override that method
            // this is scheduled to be fixed through JDA's message-rw branch
            channel.sendFile(FileUtils.readFileToByteArray(file), file.getName(), message).queue(successWrapper, failureWrapper);
        } catch (InsufficientPermissionException e) {
            if (onFail != null) {
                onFail.accept(e);
            }
            handleInsufficientPermissionsException(channel, e);
        } catch (IOException e) {
            log.error("Could not send file {}, it appears to be borked", file.getAbsolutePath(), e);
        }
        return result;
    }

    //class internal editing method
    @Nonnull
    private static MessageFuture editMessage0(@Nonnull MessageChannel channel, long oldMessageId, @Nonnull Message newMessage,
                                              @Nullable Consumer<Message> onSuccess, @Nullable Consumer<Throwable> onFail) {

        MessageFuture result = new MessageFuture();
        Consumer<Message> successWrapper = m -> {
            result.complete(m);
            Metrics.successfulRestActions.labels("editMessage").inc();
            if (onSuccess != null) {
                onSuccess.accept(m);
            }
        };
        Consumer<Throwable> failureWrapper = t -> {
            result.completeExceptionally(t);
            if (onFail != null) {
                onFail.accept(t);
            } else {
                String info = String.format("Could not edit message %s in channel %s in guild %s with new content %s",
                        oldMessageId, channel.getId(),
                        (channel instanceof TextChannel) ? ((TextChannel) channel).getGuild().getIdLong() : "null",
                        newMessage.getContentRaw());
                getJdaRestActionFailureHandler(info).accept(t);
            }
        };

        try {
            channel.editMessageById(oldMessageId, newMessage).queue(successWrapper, failureWrapper);
        } catch (InsufficientPermissionException e) {
            if (onFail != null) {
                onFail.accept(e);
            }
            handleInsufficientPermissionsException(channel, e);
        }
        return result;
    }

    public static void handleInsufficientPermissionsException(@Nonnull MessageChannel channel,
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


    //handles failed JDA rest actions by logging them with an informational string and optionally ignoring some error response codes
    @Nonnull
    public static Consumer<Throwable> getJdaRestActionFailureHandler(@Nonnull String info, ErrorResponse... ignored) {
        return t -> {
            if (t instanceof ErrorResponseException) {
                ErrorResponseException e = (ErrorResponseException) t;
                Metrics.failedRestActions.labels(Integer.toString(e.getErrorCode())).inc();
                if (Arrays.asList(ignored).contains(e.getErrorResponse())
                        || e.getErrorCode() == -1) { //socket timeout, fuck those
                    return;
                }
            }
            log.warn(info, t);
        };
    }

}
