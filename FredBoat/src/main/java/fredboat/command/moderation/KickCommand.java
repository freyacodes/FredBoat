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

package fredboat.command.moderation;

import fredboat.commandmeta.abs.CommandContext;
import fredboat.feature.metrics.Metrics;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.util.DiscordUtil;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.requests.restaction.AuditableRestAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * Created by napster on 19.04.17.
 * <p>
 * Kick a user.
 */
public class KickCommand extends DiscordModerationCommand<Void> {

    private static final Logger log = LoggerFactory.getLogger(KickCommand.class);

    public KickCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Nonnull
    @Override
    protected AuditableRestAction<Void> modAction(@Nonnull ModActionInfo modActionInfo) {
        String auditLogReason = modActionInfo.getFormattedReason();
        Guild guild = modActionInfo.getContext().guild;
        if (modActionInfo.getTargetMember() != null) {
            return guild.getController().kick(modActionInfo.getTargetMember(), auditLogReason);
        }

        //we should not reach this place due to requiresMember() = true implementation
        log.warn("Requested kick action for null member. This looks like a bug. Returning EmptyRestAction. User input:\n{}",
                modActionInfo.getContext().msg.getContentRaw());
        return new AuditableRestAction.EmptyRestAction<>(modActionInfo.getContext().guild.getJDA());
    }

    @Override
    protected boolean requiresMember() {
        return true; //can't kick non-members
    }

    @Nonnull
    @Override
    protected Consumer<Void> onSuccess(@Nonnull ModActionInfo modActionInfo) {
        String successOutput = modActionInfo.getContext().i18nFormat("kickSuccess",
                modActionInfo.getTargetUser().getAsMention() + " " + TextUtils.escapeAndDefuse(modActionInfo.targetAsString()))
                + "\n" + TextUtils.escapeAndDefuse(modActionInfo.getPlainReason());
        return aVoid -> {
            Metrics.successfulRestActions.labels("kick").inc();
            modActionInfo.getContext().replyWithName(successOutput);
        };
    }

    @Nonnull
    @Override
    protected Consumer<Throwable> onFail(@Nonnull ModActionInfo modActionInfo) {
        String escapedTargetName = TextUtils.escapeAndDefuse(modActionInfo.targetAsString());
        return t -> {
            CommandContext context = modActionInfo.getContext();
            CentralMessaging.getJdaRestActionFailureHandler(String.format("Failed to kick user %s in guild %s",
                    modActionInfo.getTargetUser(), context.guild)).accept(t);
            context.replyWithName(context.i18nFormat("kickFail",
                    modActionInfo.getTargetUser().getAsMention() + " " + escapedTargetName));
        };
    }

    @Override
    protected CompletionStage<Boolean> checkAuthorizationWithFeedback(@Nonnull ModActionInfo modActionInfo) {
        CommandContext context = modActionInfo.getContext();
        Member target = modActionInfo.getTargetMember();
        Member mod = context.invoker;

        if (!context.checkInvokerPermissionsWithFeedback(Permission.KICK_MEMBERS)) {
            return CompletableFuture.completedFuture(false);
        }
        if (!context.checkSelfPermissionsWithFeedback(Permission.KICK_MEMBERS)) {
            return CompletableFuture.completedFuture(false);
        }
        if (target == null) {
            return CompletableFuture.completedFuture(true);
        }


        if (mod == target) {
            context.replyWithName(context.i18n("kickFailSelf"));
            return CompletableFuture.completedFuture(false);
        }

        if (target.isOwner()) {
            context.replyWithName(context.i18n("kickFailOwner"));
            return CompletableFuture.completedFuture(false);
        }

        if (target == target.getGuild().getSelfMember()) {
            context.replyWithName(context.i18n("kickFailMyself"));
            return CompletableFuture.completedFuture(false);
        }

        if (DiscordUtil.getHighestRolePosition(mod) <= DiscordUtil.getHighestRolePosition(target) && !mod.isOwner()) {
            context.replyWithName(context.i18nFormat("modFailUserHierarchy", TextUtils.escapeAndDefuse(target.getEffectiveName())));
            return CompletableFuture.completedFuture(false);
        }

        if (DiscordUtil.getHighestRolePosition(mod.getGuild().getSelfMember()) <= DiscordUtil.getHighestRolePosition(target)) {
            context.replyWithName(context.i18nFormat("modFailBotHierarchy", TextUtils.escapeAndDefuse(target.getEffectiveName())));
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.completedFuture(true);
    }

    @Override
    protected Optional<String> dmForTarget(ModActionInfo modActionInfo) {
        return Optional.of(modActionInfo.getContext().i18nFormat("modActionTargetDmKicked",
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
                + "#" + context.i18n("helpKickCommand");
    }
}
