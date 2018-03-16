/*
 *
 * MIT License
 *
 * Copyright (c) 2017-2018 Frederik Ar. Mikkelsen
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

package fredboat.commandmeta.abs;

import fredboat.definitions.Module;
import fredboat.main.Launcher;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import space.npstr.annotations.FieldsAreNonNullByDefault;
import space.npstr.annotations.ParametersAreNonnullByDefault;
import space.npstr.annotations.ReturnTypesAreNonNullByDefault;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by napster on 08.09.17.
 * <p>
 * Convenience container for values associated with an issued command, also does the parsing
 * <p>
 * Don't save these anywhere as they hold references to JDA objects, just pass them down through (short-lived) command execution
 */
@FieldsAreNonNullByDefault
@ParametersAreNonnullByDefault
@ReturnTypesAreNonNullByDefault
public class CommandContext extends Context {

    public final Guild guild;
    public final TextChannel channel;
    public final Member invoker;
    public final Message msg;

    public final boolean isMention;          // whether a mention was used to trigger this command
    public final String trigger;             // the command trigger, e.g. "play", or "p", or "pLaY", whatever the user typed
    public final String[] args;              // the arguments split by whitespace, excluding prefix and trigger
    public final String rawArgs;             // raw arguments excluding prefix and trigger, trimmed
    public final Command command;


    //built by the CommandContextParser
    public CommandContext(Guild guild, TextChannel channel, Member invoker, Message message,
                          boolean isMention, String trigger, String[] args, String rawArgs, Command command) {
        this.guild = guild;
        this.channel = channel;
        this.invoker = invoker;
        this.msg = message;
        this.isMention = isMention;
        this.trigger = trigger;
        this.args = args;
        this.rawArgs = rawArgs;
        this.command = command;
    }

    /**
     * Deletes the users message that triggered this command, if we have the permissions to do so
     */
    public void deleteMessage() {
        TextChannel tc = msg.getTextChannel();

        if (tc != null && hasPermissions(tc, Permission.MESSAGE_MANAGE)) {
            CentralMessaging.deleteMessage(msg);
        }
    }

    /**
     * @return an adjusted list of mentions in case the prefix mention is used to exclude it. This method should always
     * be used over Message#getMentions()
     */
    public List<User> getMentionedUsers() {
        if (isMention) {
            //remove the first mention
            List<User> mentions = new ArrayList<>(msg.getMentionedUsers());
            if (!mentions.isEmpty()) {
                mentions.remove(0);
                //FIXME: this will mess with the mentions if the bot was mentioned at a later place in the messagea second time,
                // for example @bot hug @bot will not trigger a self hug message
                // low priority, this is mostly a cosmetic issue
            }
            return mentions;
        } else {
            return msg.getMentionedUsers();
        }
    }

    public boolean hasArguments() {
        return args.length > 0 && !rawArgs.isEmpty();
    }

    public Collection<Module> getEnabledModules() {
        return Launcher.getBotController().getGuildModulesService().fetchGuildModules(this.guild).getEnabledModules();
    }

    @Override
    public TextChannel getTextChannel() {
        return channel;
    }

    @Override
    public Guild getGuild() {
        return guild;
    }

    @Override
    public Member getMember() {
        return invoker;
    }

    @Override
    public User getUser() {
        return invoker.getUser();
    }
}
