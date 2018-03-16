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

package fredboat.command.music.control;

import fredboat.audio.queue.RepeatMode;
import fredboat.command.info.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.definitions.PermissionLevel;
import fredboat.main.Launcher;
import fredboat.messaging.internal.Context;

import javax.annotation.Nonnull;

public class RepeatCommand extends Command implements IMusicCommand, ICommandRestricted {

    public RepeatCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        if (!context.hasArguments()) {
            HelpCommand.sendFormattedCommandHelp(context);
            return;
        }

        RepeatMode desiredRepeatMode;
        String userInput = context.args[0];
        switch (userInput) {
            case "off":
            case "out":
                desiredRepeatMode = RepeatMode.OFF;
                break;
            case "single":
            case "one":
            case "track":
                desiredRepeatMode = RepeatMode.SINGLE;
                break;
            case "all":
            case "list":
            case "queue":
                desiredRepeatMode = RepeatMode.ALL;
                break;
            case "help":
            default:
                HelpCommand.sendFormattedCommandHelp(context);
                return;
        }

        Launcher.getBotController().getPlayerRegistry().getOrCreate(context.guild).setRepeatMode(desiredRepeatMode);

        switch (desiredRepeatMode) {
            case OFF:
                context.reply(context.i18n("repeatOff"));
                break;
            case SINGLE:
                context.reply(context.i18n("repeatOnSingle"));
                break;
            case ALL:
                context.reply(context.i18n("repeatOnAll"));
                break;
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        String usage = "{0}{1} single|all|off\n#";
        return usage + context.i18n("helpRepeatCommand");
    }

    @Nonnull
    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.DJ;
    }
}
