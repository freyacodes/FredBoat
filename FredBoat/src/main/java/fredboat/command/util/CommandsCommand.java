package fredboat.command.util;

import fredboat.Config;
import fredboat.command.fun.RemoteFileCommand;
import fredboat.command.fun.TextCommand;
import fredboat.commandmeta.CommandRegistry;
import fredboat.commandmeta.abs.*;
import fredboat.feature.I18n;
import fredboat.util.DiscordUtil;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.utils.PermissionUtil;

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
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {

        //is this the music boat? shortcut to showing those commands
        //taking this shortcut we're missing out on showing a few commands to pure music bot users
        // http://i.imgur.com/511Hb8p.png screenshot from 1st April 2017
        //bot owner and debug commands (+ ;;music and ;;help) missing + the currently defunct config command
        //this is currently fine but might change in the future
        if (DiscordUtil.isMusicBot()) {
            new MusicHelpCommand().onInvoke(guild, channel, invoker, message, args);
        }

        if (DiscordUtil.isMainBot()) {
            mainBotHelp(guild, channel, invoker);
        }
    }

    private void mainBotHelp(Guild guild, TextChannel channel, Member invoker) {
        Set<String> commandsAndAliases = CommandRegistry.getRegisteredCommandsAndAliases();
        Set<String> unsortedAliases = new HashSet<>(); //hash set = only unique commands
        for (String commandOrAlias : commandsAndAliases) {
            String mainAlias = CommandRegistry.getCommand(commandOrAlias).name;
            unsortedAliases.add(mainAlias);
        }
        //alphabetical order
        List<String> sortedAliases = new ArrayList<>(unsortedAliases);
        Collections.sort(sortedAliases);

        String fun = "**" + I18n.get(guild).getString("commandsFun") + ":** ";
        String memes = "**" + I18n.get(guild).getString("commandsMemes") + ":**";
        String util = "**" + I18n.get(guild).getString("commandsUtility") + ":** ";
        String mod = "**" + I18n.get(guild).getString("commandsModeration") + ":** ";
        String maint = "**" + I18n.get(guild).getString("commandsMaintenance") + ":** ";
        String owner = "**" + I18n.get(guild).getString("commandsBotOwner") + ":** ";

        for (String alias : sortedAliases) {
            Command c = CommandRegistry.getCommand(alias).command;
            String formattedAlias = "`" + alias + "` ";

            if (c instanceof ICommandOwnerRestricted) {
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

        if (PermissionUtil.checkPermission(guild, invoker, Permission.MESSAGE_MANAGE)) {
            out += "\n" + mod;
        }

        if (DiscordUtil.isUserBotOwner(invoker.getUser())) {
            out += "\n" + maint;
            out += "\n" + owner;
        }

        out += "\n\n" + MessageFormat.format(I18n.get(guild).getString("commandsMoreHelp"), "`" + Config.CONFIG.getPrefix() + "help <command>`");
        channel.sendMessage(out).queue();
    }

    @Override
    public String help(Guild guild) {
        String usage = "{0}{1}\n#";
        return usage + I18n.get(guild).getString("helpCommandsCommand");
    }
}
