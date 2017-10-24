package fredboat.command.moderation;

import fredboat.command.util.HelpCommand;
import fredboat.commandmeta.CommandManager;
import fredboat.commandmeta.CommandRegistry;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;

import javax.annotation.Nonnull;

public class DisableCommandsCommand extends Command implements ICommandRestricted {

    public DisableCommandsCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {

        if (context.hasArguments()) {
            CommandRegistry.CommandEntry commandEntry = CommandRegistry.getCommand(context.args[0]);
            if (commandEntry == null) {
                context.reply("This command doesn't exist!");
                return;
            }

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
        } else {
            HelpCommand.sendFormattedCommandHelp(context);
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} <command>\n#Disable a command GLOBALLY use with caution";
    }

    @Nonnull
    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.BOT_ADMIN;
    }
}
