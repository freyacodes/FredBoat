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

import fredboat.commandmeta.abs.CommandContext;
import fredboat.feature.metrics.Metrics;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.util.DiscordUtil;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.requests.restaction.AuditableRestAction;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public class SoftbanCommand extends DiscordModerationCommand<Void> {


    public SoftbanCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Nonnull
    @Override
    protected AuditableRestAction<Void> modAction(@Nonnull ModActionInfo modActionInfo) {
        return modActionInfo.getContext().guild.getController()
                .ban(modActionInfo.getTargetUser(),
                        modActionInfo.isKeepMessages() ? 0 : DEFAULT_DELETE_DAYS,
                        modActionInfo.getFormattedReason());
    }

    @Override
    protected boolean requiresMember() {
        return false;
    }

    @Nonnull
    @Override
    protected Consumer<Void> onSuccess(@Nonnull ModActionInfo modActionInfo) {
        String successOutput = modActionInfo.getContext().i18nFormat("softbanSuccess",
                modActionInfo.getTargetUser().getAsMention() + " " + TextUtils.escapeAndDefuse(modActionInfo.targetAsString()))
                + "\n" + TextUtils.escapeAndDefuse(modActionInfo.getPlainReason());

        return aVoid -> {
            Metrics.successfulRestActions.labels("ban").inc();
            modActionInfo.getContext().guild.getController().unban(modActionInfo.getTargetUser()).queue(
                    __ -> Metrics.successfulRestActions.labels("unban").inc(),
                    CentralMessaging.getJdaRestActionFailureHandler(String.format("Failed to unban user %s in guild %s",
                            modActionInfo.getTargetUser(), modActionInfo.getContext().guild))
            );
            modActionInfo.getContext().replyWithName(successOutput);
        };
    }

    @Nonnull
    @Override
    protected Consumer<Throwable> onFail(@Nonnull ModActionInfo modActionInfo) {
        String escapedTargetName = TextUtils.escapeAndDefuse(modActionInfo.targetAsString());
        return t -> {
            CommandContext context = modActionInfo.getContext();
            CentralMessaging.getJdaRestActionFailureHandler(String.format("Failed to ban user %s in guild %s",
                    modActionInfo.getTargetUser(), context.guild)).accept(t);
            context.replyWithName(context.i18nFormat("modBanFail",
                    modActionInfo.getTargetUser().getAsMention() + " " + escapedTargetName));
        };
    }

    @Override
    protected CompletionStage<Boolean> checkAuthorizationWithFeedback(@Nonnull ModActionInfo modActionInfo) {
        CommandContext context = modActionInfo.getContext();
        Member targetMember = modActionInfo.getTargetMember();
        Member mod = context.invoker;

        //a softban is like a kick + clear messages, so check for those on the invoker
        if (!context.checkInvokerPermissionsWithFeedback(Permission.KICK_MEMBERS, Permission.MESSAGE_MANAGE)) {
            return CompletableFuture.completedFuture(false);
        }
        //we however need ban perms to do this
        if (!context.checkSelfPermissionsWithFeedback(Permission.BAN_MEMBERS)) {
            return CompletableFuture.completedFuture(false);
        }

        // if the target user is NOT member of the guild AND banned, the invoker needs to have BAN perms, because it is
        // essentially an UNBAN, and we dont want to allow users to unban anyone with just kick perms
        if (targetMember == null) {
            return fetchBanlist(context)
                    .thenApply(bans -> bans.orElse(Collections.emptyList()))
                    .thenApply(bans ->
                            bans.stream().noneMatch(ban -> ban.getUser().equals(modActionInfo.getTargetUser())) // either target is not on the banlist
                                    || context.checkInvokerPermissionsWithFeedback(Permission.BAN_MEMBERS)      // or the invoker has ban perms
                    );
        }


        if (mod == targetMember) {
            context.replyWithName(context.i18n("softbanFailSelf"));
            return CompletableFuture.completedFuture(false);
        }

        if (targetMember.isOwner()) {
            context.replyWithName(context.i18n("softbanFailOwner"));
            return CompletableFuture.completedFuture(false);
        }

        if (targetMember == targetMember.getGuild().getSelfMember()) {
            context.replyWithName(context.i18n("softbanFailMyself"));
            return CompletableFuture.completedFuture(false);
        }

        if (DiscordUtil.getHighestRolePosition(mod) <= DiscordUtil.getHighestRolePosition(targetMember) && !mod.isOwner()) {
            context.replyWithName(context.i18nFormat("modFailUserHierarchy", TextUtils.escapeAndDefuse(targetMember.getEffectiveName())));
            return CompletableFuture.completedFuture(false);
        }

        if (DiscordUtil.getHighestRolePosition(mod.getGuild().getSelfMember()) <= DiscordUtil.getHighestRolePosition(targetMember)) {
            context.replyWithName(context.i18nFormat("modFailBotHierarchy", TextUtils.escapeAndDefuse(targetMember.getEffectiveName())));
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.completedFuture(true);
    }

    @Override
    protected Optional<String> dmForTarget(ModActionInfo modActionInfo) {
        return Optional.of(modActionInfo.getContext().i18nFormat("modActionTargetDmKicked", // a softban is like a kick
                "**" + TextUtils.escapeMarkdown(modActionInfo.getContext().getGuild().getName()) + "**",
                TextUtils.asString(modActionInfo.getContext().getUser()))
                + "\n" + modActionInfo.getPlainReason()
        );
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} <user>\n"
                + "{0}{1} <user> <reason>\n"
                + "{0}{1} <user> <reason> --keep\n"
                + "#" + context.i18n("helpSoftbanCommand") + "\n" + context.i18nFormat("modKeepMessages", "--keep or -k");

    }
}
