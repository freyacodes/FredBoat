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
 */

package fredboat.command.util;

import fredboat.Config;
import fredboat.command.fun.RemoteFileCommand;
import fredboat.command.fun.TextCommand;
import fredboat.commandmeta.CommandRegistry;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IFunCommand;
import fredboat.commandmeta.abs.IMaintenanceCommand;
import fredboat.commandmeta.abs.IModerationCommand;
import fredboat.commandmeta.abs.IUtilCommand;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import fredboat.perms.PermsUtil;
import net.dv8tion.jda.core.Permission;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by napster on 22.03.17.
 * <p>
 * YO DAWG I HEARD YOU LIKE COMMANDS SO I PUT
 * THIS COMMAND IN YO BOT SO YOU CAN SHOW MORE
 * COMMANDS WHILE YOU EXECUTE THIS COMMAND
 * <p>
 * Display available commands
 */
public class CommandsCommand extends Command implements IUtilCommand {

    //design inspiration by Weiss Schnee's bot
    //https://cdn.discordapp.com/attachments/230033957998166016/296356070685671425/unknown.png
    @Override
    public void onInvoke(@Nonnull CommandContext context) {

        //is this the music boat? shortcut to showing those commands
        //taking this shortcut we're missing out on showing a few commands to pure music bot users
        // http://i.imgur.com/511Hb8p.png screenshot from 1st April 2017
        //bot owner and debug commands (+ ;;music and ;;help) missing + the currently defunct config command
        //this is currently fine but might change in the future
        new MusicHelpCommand().onInvoke(context);
//        mainBotHelp(context); //TODO: decide how to do handle this after unification of main and music bot
    }

    private void mainBotHelp(CommandContext context) {
        Set<String> commandsAndAliases = CommandRegistry.getRegisteredCommandsAndAliases();
        Set<String> unsortedAliases = new HashSet<>(); //hash set = only unique commands
        for (String commandOrAlias : commandsAndAliases) {
            String mainAlias = CommandRegistry.getCommand(commandOrAlias).name;
            unsortedAliases.add(mainAlias);
        }
        //alphabetical order
        List<String> sortedAliases = new ArrayList<>(unsortedAliases);
        Collections.sort(sortedAliases);

        String fun = "**" + context.i18n("commandsFun") + ":** ";
        String memes = "**" + context.i18n("commandsMemes") + ":**";
        String util = "**" + context.i18n("commandsUtility") + ":** ";
        String mod = "**" + context.i18n("commandsModeration") + ":** ";
        String maint = "**" + context.i18n("commandsMaintenance") + ":** ";
        String owner = "**" + context.i18n("commandsBotOwner") + ":** ";

        for (String alias : sortedAliases) {
            Command c = CommandRegistry.getCommand(alias).command;
            String formattedAlias = "`" + alias + "` ";

            if (c instanceof ICommandRestricted
                    && ((ICommandRestricted) c).getMinimumPerms() == PermissionLevel.BOT_OWNER) {
                owner += formattedAlias;
            } else if (c instanceof TextCommand || c instanceof RemoteFileCommand) {
                memes += formattedAlias;
            } else {
                //overlap is possible in here, that's ok
                if (c instanceof IFunCommand) {
                    fun += formattedAlias;
                }
                if (c instanceof IUtilCommand) {
                    util += formattedAlias;
                }
                if (c instanceof IModerationCommand) {
                    mod += formattedAlias;
                }
                if (c instanceof IMaintenanceCommand) {
                    maint += formattedAlias;
                }
            }
        }

        String out = fun;
        out += "\n" + util;
        out += "\n" + memes;

        if (context.invoker.hasPermission(Permission.MESSAGE_MANAGE)) {
            out += "\n" + mod;
        }

        if (PermsUtil.isUserBotOwner(context.invoker.getUser())) {
            out += "\n" + maint;
            out += "\n" + owner;
        }

        out += "\n\n" + context.i18nFormat("commandsMoreHelp", "`" + Config.CONFIG.getPrefix() + "help <command>`");
        context.reply(out);
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1}\n#" + context.i18n("helpCommandsCommand");
    }
}
