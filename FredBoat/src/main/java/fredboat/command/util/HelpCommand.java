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
import fredboat.command.music.control.SelectCommand;
import fredboat.commandmeta.CommandRegistry;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IUtilCommand;
import fredboat.feature.I18n;
import fredboat.perms.PermissionLevel;
import fredboat.util.Emojis;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;

public class HelpCommand extends Command implements IUtilCommand {

    //This can be set using eval in case we need to change it in the future ~Fre_d
    public static String inviteLink = "https://discord.gg/cgPFW4q";

    private static final Logger log = LoggerFactory.getLogger(HelpCommand.class);

    @Override
    public void onInvoke(CommandContext context) {

        if (context.args.length > 1) {
            sendFormattedCommandHelp(context, context.args[1]);
        } else {
            sendGeneralHelp(context);
        }
    }

    @Override
    public String help(Guild guild) {
        String usage = "{0}{1} OR {0}{1} <command>\n#";
        return usage + I18n.get(guild).getString("helpHelpCommand");
    }

    public static void sendGeneralHelp(CommandContext context) {
        context.replyPrivate(getHelpDmMsg(context.guild),
                success -> {
                    String out = I18n.get(context, "helpSent");
                    out += "\n" + MessageFormat.format(I18n.get(context, "helpCommandsPromotion"),
                            "`" + Config.CONFIG.getPrefix() + "commands`");
                    if (context.hasPermissions(Permission.MESSAGE_WRITE)) {
                        context.replyWithName(out);
                    }
                },
                failure -> {
                    if (context.hasPermissions(Permission.MESSAGE_WRITE)) {
                        context.replyWithName(Emojis.EXCLAMATION + I18n.get(context, "helpDmFailed"));
                    }
                }
        );
    }

    public static String getFormattedCommandHelp(Guild guild, Command command, String commandOrAlias) {
        String helpStr = command.help(guild);
        //some special needs
        //to display helpful information on some commands: thirdParam = {2} in the language resources
        String thirdParam = "";
        if (command instanceof SelectCommand)
            thirdParam = "play";

        return MessageFormat.format(helpStr, Config.CONFIG.getPrefix(), commandOrAlias, thirdParam);
    }

    public static void sendFormattedCommandHelp(CommandContext context) {
        sendFormattedCommandHelp(context, context.trigger);
    }

    private static void sendFormattedCommandHelp(CommandContext context, String trigger) {
        CommandRegistry.CommandEntry commandEntry = CommandRegistry.getCommand(trigger);
        if (commandEntry == null) {
            String out = "`" + Config.CONFIG.getPrefix() + trigger + "`: " + I18n.get(context, "helpUnknownCommand");
            out += "\n" + MessageFormat.format(I18n.get(context, "helpCommandsPromotion"),
                    "`" + Config.CONFIG.getPrefix() + "commands`");
            context.replyWithName(out);
            return;
        }

        Command command = commandEntry.command;

        String out = getFormattedCommandHelp(context.guild, command, trigger);

        if (command instanceof ICommandRestricted
                && ((ICommandRestricted) command).getMinimumPerms() == PermissionLevel.BOT_OWNER)
            out += "\n#" + I18n.get(context, "helpCommandOwnerRestricted");
        out = TextUtils.asMarkdown(out);
        out = I18n.get(context, "helpProperUsage") + out;
        context.replyWithName(out);
    }

    public static String getHelpDmMsg(Guild guild) {
        return MessageFormat.format(I18n.get(guild).getString("helpDM"), inviteLink);
    }
}
