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

import fredboat.messaging.CentralMessaging;
import fredboat.messaging.MessageFuture;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.function.Consumer;

/**
 * Created by napster on 10.09.17.
 * <p>
 * Provides a context to whats going on. Where is it happening, who caused it?
 * Also home to a bunch of convenience methods
 */
public abstract class Context {

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

    public void sendTyping() {
        CentralMessaging.sendTyping(getTextChannel());
    }

    public void replyPrivate(@Nonnull String message, @Nullable Consumer<Message> onSuccess, @Nullable Consumer<Throwable> onFail) {
        getMember().getUser().openPrivateChannel().queue(
                privateChannel -> CentralMessaging.sendMessage(privateChannel, message, onSuccess, onFail)
        );
    }
}
