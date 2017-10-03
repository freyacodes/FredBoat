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

package fredboat.command.admin;

import fredboat.command.util.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.feature.togglz.FeatureFlags;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import fredboat.util.ratelimit.Ratelimiter;
import net.dv8tion.jda.core.entities.User;

import javax.annotation.Nonnull;

/**
 * Created by napster on 17.04.17.
 * <p>
 * Lift ratelimit and remove a user from the blacklist
 */
public class UnblacklistCommand extends Command implements ICommandRestricted {
    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        if (!FeatureFlags.RATE_LIMITER.isActive()) {
            context.replyWithName("The rate limiter feature has not been turned on.");
            return;
        }

        if (context.msg.getMentionedUsers().isEmpty()) {
            HelpCommand.sendFormattedCommandHelp(context);
            return;
        }

        User user = context.msg.getMentionedUsers().get(0);
        String userId = user.getId();

        if (userId == null || "".equals(userId)) {
            HelpCommand.sendFormattedCommandHelp(context);
            return;
        }

        Ratelimiter.getRatelimiter().liftLimitAndBlacklist(user.getIdLong());
        context.replyWithName(context.i18nFormat("unblacklisted", user.getAsMention()));
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} @<user>\n#Remove a user from the blacklist.";
    }

    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.BOT_OWNER;
    }
}
