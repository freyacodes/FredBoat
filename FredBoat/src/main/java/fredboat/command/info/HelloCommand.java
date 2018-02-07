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

package fredboat.command.info;

import fredboat.command.config.PrefixCommand;
import fredboat.commandmeta.CommandInitializer;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IInfoCommand;
import fredboat.main.Config;
import fredboat.messaging.internal.Context;
import fredboat.shared.constant.BotConstants;
import net.dv8tion.jda.core.entities.Guild;

import javax.annotation.Nonnull;

/**
 * Created by napster on 23.01.18.
 */
public class HelloCommand extends Command implements IInfoCommand {

    public HelloCommand(@Nonnull String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        context.reply(getHello(context.guild));
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1}\n#Send the FredBoat hello message."; //todo i18n
    }

    @Nonnull
    private static final String HELLO
            = "Thanks for using Fredboat Music! To get started, join a voice channel and type `%s <song name>`."
            + " I'll give you a list of 5 songs where you can choose one by typing `%s <number>`."
            + "\n\n"
            + "Type `%s` for a list of commands you can use! Or go to <%s> for a full review of what I can do."
            + " Type `%s` for documentation, bot invite links, and an invite to the Fredboat Hangout Discord Server."
            + "\n\n"
            + "You can set up FredBoat to announce music as it plays and automatically"
            + " resume when a user enters the channel with the `%s` command."
            + "\n\n"
            + "I can speak your language, too! Type `%s` to see a list of languages I can speak."
            + " It should look like this: `%s en_US` but with your language code."
            + "\n\n"
            + "I have a built in permissions system you can use! Follow this link to learn more: <%s>";

    @Nonnull
    public static String getHello(@Nonnull Guild guild) {
        String prefix = Config.CONFIG.getPrefix();
        try {
            prefix = PrefixCommand.giefPrefix(guild);
        } catch (Exception ignored) {
        }

        return String.format(HELLO,
                prefix + CommandInitializer.PLAY_COMM_NAME,
                prefix + CommandInitializer.PLAY_COMM_NAME,
                prefix + CommandInitializer.COMMANDS_COMM_NAME,
                BotConstants.DOCS_URL,
                prefix + CommandInitializer.HELP_COMM_NAME,
                prefix + CommandInitializer.CONFIG_COMM_NAME,
                prefix + CommandInitializer.LANGUAGE_COMM_NAME,
                prefix + CommandInitializer.LANGUAGE_COMM_NAME,
                BotConstants.DOCS_PERMISSIONS_URL
        );
    }
}
