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
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import fredboat.util.Emojis;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.MessageFormat;

public class HelpCommand extends Command implements IUtilCommand {

    //This can be set using eval in case we need to change it in the future ~Fre_d
    public static String inviteLink = "https://discord.gg/cgPFW4q";

    private static final Logger log = LoggerFactory.getLogger(HelpCommand.class);

    @Override
    public void onInvoke(@Nonnull CommandContext context) {

        if (context.args.length > 1) {
            sendFormattedCommandHelp(context, context.args[1]);
        } else {
            sendGeneralHelp(context);
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} OR {0}{1} <command>\n#" + context.i18n("helpHelpCommand");
    }

    public static void sendGeneralHelp(CommandContext context) {
        context.replyPrivate(getHelpDmMsg(context.guild),
                success -> {
                    String out = context.i18n("helpSent");
                    out += "\n" + context.i18nFormat("helpCommandsPromotion",
                            "`" + Config.CONFIG.getPrefix() + "commands`");
                    if (context.hasPermissions(Permission.MESSAGE_WRITE)) {
                        context.replyWithName(out);
                    }
                },
                failure -> {
                    if (context.hasPermissions(Permission.MESSAGE_WRITE)) {
                        context.replyWithName(Emojis.EXCLAMATION + context.i18n("helpDmFailed"));
                    }
                }
        );
    }

    public static String getFormattedCommandHelp(Context context, Command command, String commandOrAlias) {
        String helpStr = command.help(context);
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
            String out = "`" + Config.CONFIG.getPrefix() + trigger + "`: " + context.i18n("helpUnknownCommand");
            out += "\n" + context.i18nFormat("helpCommandsPromotion",
                    "`" + Config.CONFIG.getPrefix() + "commands`");
            context.replyWithName(out);
            return;
        }

        Command command = commandEntry.command;

        String out = getFormattedCommandHelp(context, command, trigger);

        if (command instanceof ICommandRestricted
                && ((ICommandRestricted) command).getMinimumPerms() == PermissionLevel.BOT_OWNER)
            out += "\n#" + context.i18n("helpCommandOwnerRestricted");
        out = TextUtils.asCodeBlock(out, "md");
        out = context.i18n("helpProperUsage") + out;
        context.replyWithName(out);
    }

    public static String getHelpDmMsg(@Nullable Guild guild) {
        return MessageFormat.format(I18n.get(guild).getString("helpDM"), inviteLink);
    }
}
