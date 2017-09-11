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
import fredboat.commandmeta.abs.*;
import fredboat.feature.I18n;
import fredboat.perms.PermissionLevel;
import fredboat.perms.PermsUtil;
import fredboat.util.DiscordUtil;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;

import java.text.MessageFormat;
import java.util.*;

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
    public void onInvoke(CommandContext context) {

        //is this the music boat? shortcut to showing those commands
        //taking this shortcut we're missing out on showing a few commands to pure music bot users
        // http://i.imgur.com/511Hb8p.png screenshot from 1st April 2017
        //bot owner and debug commands (+ ;;music and ;;help) missing + the currently defunct config command
        //this is currently fine but might change in the future
        if (DiscordUtil.isMusicBot()) {
            new MusicHelpCommand().onInvoke(context);
        }

        if (DiscordUtil.isMainBot()) {
            mainBotHelp(context);
        }
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

        ResourceBundle i18n = I18n.get(context.guild);
        String fun = "**" + i18n.getString("commandsFun") + ":** ";
        String memes = "**" + i18n.getString("commandsMemes") + ":**";
        String util = "**" + i18n.getString("commandsUtility") + ":** ";
        String mod = "**" + i18n.getString("commandsModeration") + ":** ";
        String maint = "**" + i18n.getString("commandsMaintenance") + ":** ";
        String owner = "**" + i18n.getString("commandsBotOwner") + ":** ";

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

        out += "\n\n" + MessageFormat.format(i18n.getString("commandsMoreHelp"), "`" + Config.CONFIG.getPrefix() + "help <command>`");
        context.reply(out);
    }

    @Override
    public String help(Guild guild) {
        String usage = "{0}{1}\n#";
        return usage + I18n.get(guild).getString("helpCommandsCommand");
    }
}
