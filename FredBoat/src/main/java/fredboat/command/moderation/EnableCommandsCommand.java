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

public class EnableCommandsCommand extends Command implements ICommandRestricted {
    @Override
    public void onInvoke(@Nonnull CommandContext context) {

        if (context.args.length  == 2) {
            try {
                CommandRegistry.CommandEntry commandEntry = CommandRegistry.getCommand(context.args[1]);

                if (CommandManager.disabledCommands.contains(commandEntry.command)) {
                    CommandManager.disabledCommands.remove(commandEntry.command);
                    context.reply(":ok_hand: Command `" + commandEntry.name + "` enabled!");
                    return;
                }
                context.reply("This command is not disabled!");

            } catch (NullPointerException ex) {
                context.reply("This command doesn't exist!");
            }

        } else {
            HelpCommand.sendFormattedCommandHelp(context);
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} <command>\n#Re-enable a globally disabled command";
    }

    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.BOT_ADMIN;
    }
}
