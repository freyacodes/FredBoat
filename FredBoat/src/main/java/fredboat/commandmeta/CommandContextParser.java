/*
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

package fredboat.commandmeta;

import fredboat.command.config.PrefixCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.config.property.AppConfig;
import fredboat.feature.metrics.Metrics;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by napster on 23.02.18.
 */
@Component
public class CommandContextParser {

    private static final Logger log = LoggerFactory.getLogger(CommandContext.class);

    // https://regex101.com/r/ceFMeF/6
    //group 1 is the mention, group 2 is the id of the mention, group 3 is the rest of the input including new lines
    public static final Pattern MENTION_PREFIX = Pattern.compile("^(<@!?([0-9]+)>)(.*)$", Pattern.DOTALL);
    private final AppConfig appConfig;

    public CommandContextParser(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    /**
     * @param event the event to be parsed
     * @return The full context for the triggered command, or null if it's not a command that we know.
     */
    @Nullable
    public CommandContext parse(MessageReceivedEvent event) {
        String raw = event.getMessage().getContentRaw();

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
            String defaultPrefix = appConfig.getPrefix();
            if (raw.startsWith(prefix)) {
                input = raw.substring(prefix.length());
                if (prefix.equals(defaultPrefix)) {
                    Metrics.prefixParsed.labels("default").inc();
                } else {
                    Metrics.prefixParsed.labels("custom").inc();
                }
            } else {
                //hardcoded check for the help or prefix command that is always displayed as FredBoat status
                if (raw.startsWith(defaultPrefix + CommandInitializer.HELP_COMM_NAME)
                        || raw.startsWith(defaultPrefix + CommandInitializer.PREFIX_COMM_NAME)) {
                    Metrics.prefixParsed.labels("default").inc();
                    input = raw.substring(defaultPrefix.length());
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

        // the \p{javaSpaceChar} instead of the better known \s is used because it actually includes unicode whitespaces
        String[] args = input.split("\\p{javaSpaceChar}+");
        if (args.length < 1) {
            return null; //while this shouldn't technically be possible due to the preprocessing of the input, better be safe than throw exceptions
        }

        String commandTrigger = args[0];

        Command command = CommandRegistry.findCommand(commandTrigger.toLowerCase());
        if (command == null) {
            log.info("Unknown command:\t{}", commandTrigger);
            return null;
        } else {
            return new CommandContext(
                    event.getGuild(),
                    event.getTextChannel(),
                    event.getMember(),
                    event.getMessage(),
                    isMention,
                    commandTrigger,
                    Arrays.copyOfRange(args, 1, args.length),//exclude args[0] that contains the command trigger
                    input.replaceFirst(commandTrigger, "").trim(),
                    command);
        }
    }

}
