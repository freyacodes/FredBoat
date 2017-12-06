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
import fredboat.command.moderation.PrefixCommand;
import fredboat.commandmeta.CommandRegistry;
import fredboat.feature.metrics.Metrics;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    // https://regex101.com/r/ceFMeF/6
    //group 1 is the mention, group 2 is the id of the mention, group 3 is the rest of the input including new lines
    public static final Pattern MENTION_PREFIX = Pattern.compile("^(<@!?([0-9]+)>)(.*)$", Pattern.DOTALL);

    //@formatter:off
    @Nonnull public final Guild guild;
    @Nonnull public final TextChannel channel;
    @Nonnull public final Member invoker;
    @Nonnull public final Message msg;

             public boolean isMention = false;                  // whether a mention was used to trigger this command
    @Nonnull public String trigger = "";                        // the command trigger, e.g. "play", or "p", or "pLaY", whatever the user typed
    @Nonnull public String[] args = new String[0];              // the arguments split by whitespace, excluding prefix and trigger
    @Nonnull public String rawArgs = "";                        // raw arguments excluding prefix and trigger, trimmed
    @SuppressWarnings("ConstantConditions")//the parsing code handles setting this to a nonnull value
    @Nonnull public Command command = null;
    //@formatter:on

    /**
     * @param event the event to be parsed
     * @return The full context for the triggered command, or null if it's not a command that we know.
     */
    public static CommandContext parse(MessageReceivedEvent event) {
        String raw = event.getMessage().getRawContent();

        String input;
        boolean isMention = false;
        Matcher mentionMatcher = MENTION_PREFIX.matcher(raw);
        // either starts with a mention of us
        if (mentionMatcher.find() && mentionMatcher.group(2).equals(event.getJDA().getSelfUser().getId())) {
            input = mentionMatcher.group(3).trim();
            isMention = true;
        }
        // or starts with a custom/default prefix
        else {
            String prefix = PrefixCommand.giefPrefix(event.getGuild());
            if (raw.startsWith(prefix)) {
                input = raw.substring(prefix.length());
                if (prefix.equals(Config.CONFIG.getPrefix())) {
                    Metrics.prefixParsed.labels("default").inc();
                } else {
                    Metrics.prefixParsed.labels("custom").inc();
                }
            } else {
                //hardcoded check for the help command that is always displayed as FredBoat status
                if (raw.startsWith(Config.CONFIG.getPrefix() + "help")) {
                    Metrics.prefixParsed.labels("default").inc();
                    input = raw.substring(Config.CONFIG.getPrefix().length());
                } else {
                    //no match neither mention nor custom/default prefix
                    return null;
                }
            }
        }
        input = input.trim();// eliminate possible whitespace between the mention/prefix and the rest of the input
        if (input.isEmpty()) {
            if (isMention) { //just a mention and nothing else? trigger the prefix command
                input = "prefix";
            } else {
                return null; //no command will be detectable from an empty input
            }
        }

        String[] args = input.split("\\s+"); //split by any length of white space characters (including new lines)
        if (args.length < 1) {
            return null; //while this shouldn't technically be possible due to the preprocessing of the input, better be safe than throw exceptions
        }

        String commandTrigger = args[0];

        CommandRegistry.CommandEntry entry = CommandRegistry.getCommand(commandTrigger.toLowerCase());
        if (entry == null) {
            log.info("Unknown command:\t{}", commandTrigger);
            return null;
        } else {
            CommandContext context = new CommandContext(
                    event.getGuild(),
                    event.getTextChannel(),
                    event.getMember(),
                    event.getMessage());

            context.isMention = isMention;
            context.trigger = commandTrigger;
            context.command = entry.command;
            context.args = Arrays.copyOfRange(args, 1, args.length);//exclude args[0] that contains the command trigger
            context.rawArgs = input.replaceFirst(commandTrigger, "").trim();
            return context;
        }
    }

    private CommandContext(@Nonnull Guild guild, @Nonnull TextChannel channel, @Nonnull Member invoker, @Nonnull Message message) {
        this.guild = guild;
        this.channel = channel;
        this.invoker = invoker;
        this.msg = message;
    }

    /**
     * Deletes the users message that triggered this command, if we have the permissions to do so
     */
    public void deleteMessage() {
        TextChannel tc = msg.getTextChannel();
        if (tc != null && hasPermissions(tc, Permission.MESSAGE_MANAGE, //While Manage Message _should_ be enough as it _should_
                Permission.MESSAGE_READ,                                // implicitly give us all the other Text permissions,
                Permission.MESSAGE_HISTORY)) {                          // it is bugged, so we do some additional checks here.
            CentralMessaging.deleteMessage(msg);                        // See https://github.com/DV8FromTheWorld/JDA/issues/414 for more info.
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

    @Nonnull
    @Override
    public TextChannel getTextChannel() {
        return channel;
    }

    @Nonnull
    @Override
    public Guild getGuild() {
        return guild;
    }

    @Nonnull
    @Override
    public Member getMember() {
        return invoker;
    }

    @Nonnull
    @Override
    public User getUser() {
        return invoker.getUser();
    }
}
