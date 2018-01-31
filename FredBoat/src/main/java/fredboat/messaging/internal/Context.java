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

package fredboat.messaging.internal;

import fredboat.command.config.PrefixCommand;
import fredboat.commandmeta.MessagingException;
import fredboat.feature.I18n;
import fredboat.feature.metrics.Metrics;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.MessageFuture;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Created by napster on 10.09.17.
 * <p>
 * Provides a context to whats going on. Where is it happening, who caused it?
 * Also home to a bunch of convenience methods
 */
public abstract class Context {

    private static final Logger log = LoggerFactory.getLogger(Context.class);

    public abstract TextChannel getTextChannel();

    public abstract Guild getGuild();

    public abstract Member getMember();

    public abstract User getUser();


    // ********************************************************************************
    //                         Convenience reply methods
    // ********************************************************************************

    @SuppressWarnings("UnusedReturnValue")
    public MessageFuture reply(String message) {
        return CentralMessaging.sendMessage(getTextChannel(), message);
    }

    @SuppressWarnings("UnusedReturnValue")
    public MessageFuture reply(String message, Consumer<Message> onSuccess) {
        return CentralMessaging.sendMessage(getTextChannel(), message, onSuccess);
    }

    @SuppressWarnings("UnusedReturnValue")
    public MessageFuture reply(String message, Consumer<Message> onSuccess, Consumer<Throwable> onFail) {
        return CentralMessaging.sendMessage(getTextChannel(), message, onSuccess, onFail);
    }

    @SuppressWarnings("UnusedReturnValue")
    public MessageFuture reply(Message message) {
        return CentralMessaging.sendMessage(getTextChannel(), message);
    }

    @SuppressWarnings("UnusedReturnValue")
    public MessageFuture reply(Message message, Consumer<Message> onSuccess) {
        return CentralMessaging.sendMessage(getTextChannel(), message, onSuccess);
    }

    @SuppressWarnings("UnusedReturnValue")
    public MessageFuture replyWithName(String message) {
        return reply(TextUtils.prefaceWithName(getMember(), message));
    }

    @SuppressWarnings("UnusedReturnValue")
    public MessageFuture replyWithName(String message, Consumer<Message> onSuccess) {
        return reply(TextUtils.prefaceWithName(getMember(), message), onSuccess);
    }

    @SuppressWarnings("UnusedReturnValue")
    public MessageFuture replyWithMention(String message) {
        return reply(TextUtils.prefaceWithMention(getMember(), message));
    }

    @SuppressWarnings("UnusedReturnValue")
    public MessageFuture reply(MessageEmbed embed) {
        return CentralMessaging.sendMessage(getTextChannel(), embed);
    }


    @SuppressWarnings("UnusedReturnValue")
    public MessageFuture replyFile(@Nonnull File file, @Nullable Message message) {
        return CentralMessaging.sendFile(getTextChannel(), file, message);
    }

    @SuppressWarnings("UnusedReturnValue")
    public MessageFuture replyImage(@Nonnull String url, @Nullable String message, @Nullable Consumer<Message> onSuccess) {
        return CentralMessaging.sendMessage(
                getTextChannel(),
                CentralMessaging.getClearThreadLocalMessageBuilder()
                        .setEmbed(embedImage(url))
                        .append(message != null ? message : "")
                        .build(),
                onSuccess
        );
    }


    @SuppressWarnings("UnusedReturnValue")
    public MessageFuture replyImage(@Nonnull String url, @Nullable String message) {
        return replyImage(url, message, null);
    }

    @SuppressWarnings("UnusedReturnValue")
    public MessageFuture replyImage(@Nonnull String url) {
        return replyImage(url, null);
    }

    public void sendTyping() {
        CentralMessaging.sendTyping(getTextChannel());
    }

    public void replyPrivate(@Nonnull String message, @Nullable Consumer<Message> onSuccess, @Nullable Consumer<Throwable> onFail) {
        getMember().getUser().openPrivateChannel().queue(
                privateChannel -> {
                    Metrics.successfulRestActions.labels("openPrivateChannel").inc();
                    CentralMessaging.sendMessage(privateChannel, message, onSuccess, onFail);
                },
                onFail != null ? onFail : CentralMessaging.NOOP_EXCEPTION_HANDLER //dun care logging about ppl that we cant message
        );
    }

    //checks whether we have the provided permissions for the channel of this context
    @CheckReturnValue
    public boolean hasPermissions(Permission... permissions) {
        return hasPermissions(getTextChannel(), permissions);
    }

    //checks whether we have the provided permissions for the provided channel
    @CheckReturnValue
    public boolean hasPermissions(@Nonnull TextChannel tc, Permission... permissions) {
        return getGuild().getSelfMember().hasPermission(tc, permissions);
    }

    /**
     * Return a single translated string.
     *
     * @param key Key of the i18n string.
     * @return Formatted i18n string, or a default language string if i18n is not found.
     */
    @CheckReturnValue
    public String i18n(@Nonnull String key) {
        if (getI18n().containsKey(key)) {
            return getI18n().getString(key);
        } else {
            log.warn("Missing language entry for key {} in language {}", key, I18n.getLocale(getGuild()).getCode());
            return I18n.DEFAULT.getProps().getString(key);
        }
    }

    /**
     * Return a translated string with applied formatting.
     *
     * @param key Key of the i18n string.
     * @param params Parameter(s) to be apply into the i18n string.
     * @return Formatted i18n string.
     */
    @CheckReturnValue
    public String i18nFormat(@Nonnull String key, Object... params) {
        if (params == null || params.length == 0) {
            log.warn("Context#i18nFormat() called with empty or null params, this is likely a bug.",
                    new MessagingException("a stack trace to help find the source"));
        }
        try {
            return MessageFormat.format(this.i18n(key), params);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to format key '{}' for language '{}' with following parameters: {}",
                    key, getI18n().getBaseBundleName(), params, e);
            //fall back to default props
            return MessageFormat.format(I18n.DEFAULT.getProps().getString(key), params);
        }
    }


    /**
     * Convenience method to get the prefix of the guild of this context.
     */
    @Nonnull
    public String getPrefix() {
        return PrefixCommand.giefPrefix(getGuild());
    }

    // ********************************************************************************
    //                         Internal context stuff
    // ********************************************************************************

    private ResourceBundle i18n;

    @Nonnull
    public ResourceBundle getI18n() {
        if (this.i18n == null) {
            this.i18n = I18n.get(getGuild());
        }
        return this.i18n;
    }

    private static MessageEmbed embedImage(String url) {
        return CentralMessaging.getColoredEmbedBuilder()
                .setImage(url)
                .build();
    }
}
