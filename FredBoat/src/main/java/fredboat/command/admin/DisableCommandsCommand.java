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

package fredboat.command.admin;

import fredboat.command.info.HelpCommand;
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
            Command command = CommandRegistry.findCommand(context.args[0]);
            if (command == null) {
                context.reply("This command doesn't exist!");
                return;
            }

            if (command.name.equals("enable")
                    || command.name.equals("disable")) {
                context.reply("Let's not disable this :wink:");
                return;
            }

            if (CommandManager.disabledCommands.contains(command)) {
                context.reply("This command is already disabled!");
                return;
            }

            CommandManager.disabledCommands.add(command);
            context.reply(":ok_hand: Command `" + command.name + "` disabled!");
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
