package fredboat.command.moderation;

import fredboat.command.util.HelpCommand;
import fredboat.commandmeta.CommandManager;
import fredboat.commandmeta.CommandRegistry;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.perms.PermissionLevel;
import net.dv8tion.jda.core.entities.Guild;

public class DisableCommandsCommand extends Command implements ICommandRestricted {
    @Override
    public void onInvoke(CommandContext context) {

        if (context.args.length  == 2) {
            try {
                CommandRegistry.CommandEntry commandEntry = CommandRegistry.getCommand(context.args[1]);

                if (commandEntry.name.equals("enable")
                        || commandEntry.name.equals("disable")) {
                    context.reply("Let's not disable this :wink:");
                    return;
                }

                if (CommandManager.disabledCommands.contains(commandEntry.command)) {
                    context.reply("This command is already disabled!");
                    return;
                }

                CommandManager.disabledCommands.add(commandEntry.command);
                context.reply(":ok_hand: Command `" + commandEntry.name + "` disabled!");

            } catch (NullPointerException ex) {
                context.reply("This command doesn't exist!");
            }

        } else {
            HelpCommand.sendFormattedCommandHelp(context);
        }
    }

    @Override
    public String help(Guild guild) {
        return "{0}{1} <command>\n#Disable a command GLOBALLY use with caution";
    }

    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.BOT_ADMIN;
    }
}
