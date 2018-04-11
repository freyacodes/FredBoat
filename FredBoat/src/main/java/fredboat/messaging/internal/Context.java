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
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
        return CentralMessaging.message(getTextChannel(), message).send(this);
    }

    @SuppressWarnings("UnusedReturnValue")
    public MessageFuture reply(String message, Consumer<Message> onSuccess) {
        return CentralMessaging.message(getTextChannel(), message)
                .success(onSuccess)
                .send(this);
    }

    @SuppressWarnings("UnusedReturnValue")
    public MessageFuture reply(String message, Consumer<Message> onSuccess, Consumer<Throwable> onFail) {
        return CentralMessaging.message(getTextChannel(), message)
                .success(onSuccess)
                .failure(onFail)
                .send(this);
    }

    @SuppressWarnings("UnusedReturnValue")
    public MessageFuture reply(Message message) {
        return CentralMessaging.message(getTextChannel(), message).send(this);
    }

    @SuppressWarnings("UnusedReturnValue")
    public MessageFuture reply(Message message, Consumer<Message> onSuccess) {
        return CentralMessaging.message(getTextChannel(), message)
                .success(onSuccess)
                .send(this);
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
        return CentralMessaging.message(getTextChannel(), embed).send(this);
    }

    @SuppressWarnings("UnusedReturnValue")
    public MessageFuture replyImage(@Nonnull String url, @Nullable String message, @Nullable Consumer<Message> onSuccess) {
        return CentralMessaging.message(
                getTextChannel(),
                CentralMessaging.getClearThreadLocalMessageBuilder()
                        .setEmbed(embedImage(url))
                        .append(message != null ? message : "")
                        .build())
                .success(onSuccess)
                .send(this);
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

    /**
     * Privately DM the invoker
     */
    public void replyPrivate(@Nonnull String message, @Nullable Consumer<Message> onSuccess, @Nullable Consumer<Throwable> onFail) {
        sendPrivate(getUser(), message, onSuccess, onFail);
    }

    /**
     * Privately DM any user
     */
    public void sendPrivate(@Nonnull User user, @Nonnull String message, @Nullable Consumer<Message> onSuccess, @Nullable Consumer<Throwable> onFail) {
        user.openPrivateChannel().queue(
                privateChannel -> {
                    Metrics.successfulRestActions.labels("openPrivateChannel").inc();
                    CentralMessaging.message(privateChannel, message)
                            .success(onSuccess)
                            .failure(onFail)
                            .send(this);
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
     * @return true if we the bot have all the provided permissions, false if not. Also informs the invoker about the
     * missing permissions for the bot, given there is a channel to reply in.
     */
    public boolean checkSelfPermissionsWithFeedback(@Nonnull Permission... permissions) {
        TextChannel channel = getTextChannel();
        if (channel == null) {
            return false;  //no textchannel? can't have any permissions at all in a non-guild environment
        }
        Member self = channel.getGuild().getSelfMember();
        if (self == null) {
            return false;  //an overly defensive null check
        }

        Set<Permission> missingPerms = new HashSet<>();
        for (Permission permission : permissions) {
            if (!self.hasPermission(channel, permission)) {
                missingPerms.add(permission);
            }
        }
        if (missingPerms.isEmpty()) {
            return true;
        } else {
            List<String> permissionNames = missingPerms.stream().map(Permission::getName).collect(Collectors.toList());
            reply(i18n("permissionMissingBot") + " **" + String.join("**, **", permissionNames) + "**");
            return false;
        }
    }


    /**
     * @return true if the invoker has all the provided permissions, false if not. Also informs the invoker about the
     * missing permissions, given there is a channel to reply in.
     */
    public boolean checkInvokerPermissionsWithFeedback(@Nonnull Permission... permissions) {
        Member invoker = getMember();
        TextChannel channel = getTextChannel();
        if (invoker == null || channel == null) {
            return false; //no invoker member or textchannel? can't have any permissions at all in a non-guild environment
        }

        Set<Permission> missingPerms = new HashSet<>();
        for (Permission permission : permissions) {
            if (!invoker.hasPermission(channel, permission)) {
                missingPerms.add(permission);
            }
        }
        if (missingPerms.isEmpty()) {
            return true;
        } else {
            List<String> permissionNames = missingPerms.stream().map(Permission::getName).collect(Collectors.toList());
            reply(i18n("permissionMissingInvoker") + " **" + String.join("**, **", permissionNames) + "**");
            return false;
        }
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
