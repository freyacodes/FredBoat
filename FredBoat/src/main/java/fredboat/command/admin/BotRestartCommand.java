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

package fredboat.command.admin;

import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.main.BotController;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import fredboat.shared.constant.ExitCodes;
import fredboat.util.TextUtils;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class BotRestartCommand extends Command implements ICommandRestricted {

    public BotRestartCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Nonnull
    private String code = TextUtils.randomAlphaNumericString(4);

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        if (context.hasArguments()) {
            if (context.rawArgs.equals(code)) {
                try {
                    context.replyWithName("Restarting...").getWithDefaultTimeout();
                } catch (InterruptedException | ExecutionException | TimeoutException ignored) {
                }
                BotController.INS.shutdown(ExitCodes.EXIT_CODE_RESTART);
                return;
            } else {
                context.reply(String.format("Your input `%s` did not fit the required code `%s`. A new code will be issued.",
                        TextUtils.escapeMarkdown(context.rawArgs), code));
            }
        }

        code = TextUtils.randomAlphaNumericString(4);
        context.reply(String.format("This will **restart the whole bot**. "
                + "Please confirm by issuing this command again, with the following confirmation code appended: `%s`", code));
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} [code]\n#Restart the bot.";
    }

    @Nonnull
    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.BOT_ADMIN;
    }
}
