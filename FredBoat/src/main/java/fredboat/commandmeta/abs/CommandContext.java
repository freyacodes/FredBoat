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

package fredboat.commandmeta.abs;

import fredboat.Config;
import fredboat.commandmeta.CommandManager;
import fredboat.commandmeta.CommandRegistry;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by napster on 08.09.17.
 * <p>
 * Convenience container for values associated with an issued command, also does the parsing
 * <p>
 * Don't save these anywhere as they hold references to JDA objects, just pass them down through (short-lived) command execution
 */
public class CommandContext extends Context {

    private static final Logger log = LoggerFactory.getLogger(CommandContext.class);
    private static final Pattern COMMAND_NAME_PREFIX = Pattern.compile("(\\w+)");

    public final Guild guild;
    public final TextChannel channel;
    public final Member invoker;
    public final Message msg;

    public String prefix = Config.CONFIG.getPrefix();  // useless for now but custom prefixes anyone?
    public String trigger = "";                        // the command trigger, e.g. "play", or "p", or "pLaY", whatever the user typed
    public String[] args = new String[0];              // the arguments including prefix + trigger in args[0]
    public Command command = null;

    /**
     * @param event the event to be parsed
     * @return The full context for the triggered command, or null if it's not a command that we know.
     */
    public static CommandContext parse(MessageReceivedEvent event) {
        Matcher matcher = COMMAND_NAME_PREFIX.matcher(event.getMessage().getContent());
        if (matcher.find()) {
            CommandContext context = new CommandContext(
                    event.getGuild(),
                    event.getTextChannel(),
                    event.getMember(),
                    event.getMessage());

            context.trigger = matcher.group();
            CommandRegistry.CommandEntry entry = CommandRegistry.getCommand(context.trigger.toLowerCase());
            if (entry != null) {
                context.command = entry.command;
                context.args = CommandManager.commandToArguments(context.msg.getRawContent());
                return context;
            } else {
                log.info("Unknown command:\t{}", context.trigger);
                return null;
            }
        } else {
            return null;
        }
    }

    private CommandContext(Guild guild, TextChannel channel, Member invoker, Message message) {
        this.guild = guild;
        this.channel = channel;
        this.invoker = invoker;
        this.msg = message;
    }

    /**
     * Deletes the users message that triggered this command, if we have the permissions to do so
     */
    public void deleteMessage() {
        if (hasPermissions(Permission.MESSAGE_MANAGE)) {
            CentralMessaging.deleteMessage(msg);
        }
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
