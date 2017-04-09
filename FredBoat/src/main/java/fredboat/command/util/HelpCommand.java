/*
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
 *
 */

package fredboat.command.util;

import fredboat.Config;
import fredboat.command.fun.TalkCommand;
import fredboat.command.music.control.SelectCommand;
import fredboat.commandmeta.CommandRegistry;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.ICommandOwnerRestricted;
import fredboat.commandmeta.abs.IMusicBackupCommand;
import fredboat.commandmeta.abs.IUtilCommand;
import fredboat.feature.I18n;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import java.text.MessageFormat;

public class HelpCommand extends Command implements IMusicBackupCommand, IUtilCommand {

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {

        if (args.length > 1) {
            String commandOrAlias = args[1];
            sendFormattedCommandHelp(guild, channel, invoker, commandOrAlias);
        } else {
            sendGeneralHelp(guild, channel, invoker);
        }
    }

    @Override
    public String help(Guild guild) {
        String usage = "{0}{1} OR {0}{1} <command>\n#";
        return usage + I18n.get(guild).getString("helpHelpCommand");
    }

    private static void sendGeneralHelp(Guild guild, TextChannel channel, Member invoker) {
        if (!invoker.getUser().hasPrivateChannel()) {
            try {
                invoker.getUser().openPrivateChannel().complete(true);
            } catch (RateLimitedException e) {
                throw new RuntimeException(e);
            }
        }
        invoker.getUser().getPrivateChannel().sendMessage(I18n.get(guild).getString("helpDM")).queue();
        String out = I18n.get(guild).getString("helpSent");
        out += "\n" + MessageFormat.format(I18n.get(guild).getString("helpCommandsPromotion"), "`" + Config.CONFIG.getPrefix() + "commands`");
        TextUtils.replyWithName(channel, invoker, out);
    }

    public static String getFormattedCommandHelp(Guild guild, Command command, String commandOrAlias) {


        String helpStr = command.help(guild);
        //some special needs
        //to display helpful information on some commands: thirdParam = {2} in the language resources
        String thirdParam = "";
        if (command instanceof TalkCommand)
            thirdParam = guild.getSelfMember().getEffectiveName();
        else if (command instanceof SelectCommand)
            thirdParam = "play";

        return MessageFormat.format(helpStr, Config.CONFIG.getPrefix(), commandOrAlias, thirdParam);
    }

    public static void sendFormattedCommandHelp(Guild guild, TextChannel channel, Member invoker, String commandOrAlias) {

        CommandRegistry.CommandEntry commandEntry = CommandRegistry.getCommand(commandOrAlias);
        if (commandEntry == null) {
            String out = Config.CONFIG.getPrefix() + commandOrAlias + ": " + I18n.get(guild).getString("helpUnknownCommand");
            out += "\n" + MessageFormat.format(I18n.get(guild).getString("helpCommandsPromotion"), "`" + Config.CONFIG.getPrefix() + "commands`");
            TextUtils.replyWithName(channel, invoker, out);
            return;
        }

        Command command = commandEntry.command;

        String out = getFormattedCommandHelp(guild, command, commandOrAlias);

        if (command instanceof ICommandOwnerRestricted)
            out += "\n#" + I18n.get(guild).getString("helpCommandOwnerRestricted");
        out = TextUtils.asMarkdown(out);
        out = I18n.get(guild).getString("helpProperUsage") + out;
        TextUtils.replyWithName(channel, invoker, out);
    }
}
