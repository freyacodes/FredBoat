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

package fredboat.command.moderation;

import fredboat.command.info.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IModerationCommand;
import fredboat.feature.metrics.Metrics;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.util.ArgumentUtil;
import fredboat.util.DiscordUtil;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.requests.RestAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class SoftbanCommand extends Command implements IModerationCommand {

    private static final Logger log = LoggerFactory.getLogger(SoftbanCommand.class);

    public SoftbanCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        Guild guild = context.guild;
        //Ensure we have a search term
        if (!context.hasArguments()) {
            HelpCommand.sendFormattedCommandHelp(context);
            return;
        }

        //was there a target provided?
        Member target = ArgumentUtil.checkSingleFuzzyMemberSearchResult(context, context.args[0]);
        if (target == null) return;

        //are we allowed to do that?
        if (!checkAuthorization(context, target)) return;

        //putting together a reason
        String plainReason = DiscordUtil.getReasonForModAction(context);
        String auditLogReason = DiscordUtil.formatReasonForAuditLog(plainReason, context.invoker);

        //putting together the action
        RestAction<Void> modAction = guild.getController().ban(target, 7, auditLogReason);

        //on success
        String successOutput = context.i18nFormat("softbanSuccess",
                TextUtils.escapeAndDefuse(target.getUser().getName()), target.getUser().getDiscriminator(), target.getUser().getId())
                + "\n" + plainReason;
        Consumer<Void> onSuccess = aVoid -> {
            Metrics.successfulRestActions.labels("ban").inc();
            guild.getController().unban(target.getUser()).queue(
                    __ -> Metrics.successfulRestActions.labels("unban").inc(),
                    CentralMessaging.getJdaRestActionFailureHandler(String.format("Failed to unban user %s in guild %s",
                            target.getUser().getId(), guild.getId()))
            );
            context.replyWithName(successOutput);
        };

        //on fail
        String failOutput = context.i18nFormat("modBanFail", target.getUser());
        Consumer<Throwable> onFail = t -> {
            CentralMessaging.getJdaRestActionFailureHandler(String.format("Failed to ban user %s in guild %s",
                    target.getUser().getId(), guild.getId())).accept(t);
            context.replyWithName(failOutput);
        };

        //issue the mod action
        modAction.queue(onSuccess, onFail);
    }

    private boolean checkAuthorization(CommandContext context, Member target) {
        Member mod = context.invoker;
        if(mod == target) {
            context.replyWithName(context.i18n("softbanFailSelf"));
            return false;
        }

        if(target.isOwner()) {
            context.replyWithName(context.i18n("softbanFailOwner"));
            return false;
        }

        if(target == target.getGuild().getSelfMember()) {
            context.replyWithName(context.i18n("softbanFailMyself"));
            return false;
        }

        if (!mod.hasPermission(Permission.BAN_MEMBERS, Permission.KICK_MEMBERS) && !mod.isOwner()) {
            context.replyWithName(context.i18n("modKickBanFailUserPerms"));
            return false;
        }

        if (DiscordUtil.getHighestRolePosition(mod) <= DiscordUtil.getHighestRolePosition(target) && !mod.isOwner()) {
            context.replyWithName(context.i18nFormat("modFailUserHierarchy", TextUtils.escapeAndDefuse(target.getEffectiveName())));
            return false;
        }

        if (!mod.getGuild().getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
            context.replyWithName(context.i18n("modBanBotPerms"));
            return false;
        }

        if (DiscordUtil.getHighestRolePosition(mod.getGuild().getSelfMember()) <= DiscordUtil.getHighestRolePosition(target)) {
            context.replyWithName(context.i18nFormat("modFailBotHierarchy", TextUtils.escapeAndDefuse(target.getEffectiveName())));
            return false;
        }

        return true;
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} <user> <reason>\n#" + context.i18n("helpSoftbanCommand");
    }
}
