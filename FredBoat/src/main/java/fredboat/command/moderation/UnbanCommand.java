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

package fredboat.command.moderation;

import fredboat.commandmeta.abs.CommandContext;
import fredboat.feature.metrics.Metrics;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.util.ArgumentUtil;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.requests.restaction.AuditableRestAction;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by napster on 27.01.18.
 * <p>
 * Unban a user.
 */
public class UnbanCommand extends DiscordModerationCommand<Void> {

    public UnbanCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Nonnull
    @Override
    protected AuditableRestAction<Void> modAction(@Nonnull ModActionInfo modActionInfo) {
        return modActionInfo.getContext().guild.getController().unban(modActionInfo.getTargetUser());
    }

    @Override
    protected boolean requiresMember() {
        return false;
    }

    @Nonnull
    @Override
    protected Consumer<Void> onSuccess(@Nonnull ModActionInfo modActionInfo) {
        String successOutput = modActionInfo.getContext().i18nFormat("unbanSuccess",
                modActionInfo.getTargetUser().getAsMention() + " " + TextUtils.escapeAndDefuse(modActionInfo.targetAsString()));

        return aVoid -> {
            Metrics.successfulRestActions.labels("unban").inc();
            modActionInfo.getContext().replyWithName(successOutput);
        };
    }

    @Nonnull
    @Override
    protected Consumer<Throwable> onFail(@Nonnull ModActionInfo modActionInfo) {
        String escapedTargetName = TextUtils.escapeAndDefuse(modActionInfo.targetAsString());
        return t -> {
            CommandContext context = modActionInfo.getContext();
            if (t instanceof IllegalArgumentException) { //user was not banned in this guild (see GuildController#unban(String) handling of the response)
                context.reply(context.i18nFormat("modUnbanFailNotBanned", modActionInfo.getTargetUser().getAsMention()));
                return;
            }
            CentralMessaging.getJdaRestActionFailureHandler(String.format("Failed to unban user %s in guild %s",
                    modActionInfo.getTargetUser(), context.guild)).accept(t);
            context.replyWithName(context.i18nFormat("modUnbanFail",
                    modActionInfo.getTargetUser().getAsMention() + " " + escapedTargetName));
        };
    }

    @Override
    protected CompletionStage<Optional<User>> fromFuzzySearch(CommandContext context, String searchTerm) {
        return fetchBanlist(context)
                .thenApply(banlist -> {
                    if (!banlist.isPresent()) {
                        return Optional.empty();
                    }
                    return ArgumentUtil.checkSingleFuzzyUserSearchResult(
                            banlist.get().stream().map(Guild.Ban::getUser).collect(Collectors.toList()),
                            context, searchTerm, true);
                });
    }

    @Override
    protected CompletionStage<Boolean> checkPreconditionWithFeedback(User user, CommandContext context) {
        return fetchBanlist(context)
                .thenApply(banlist -> {
                    if (!banlist.isPresent()) {
                        return false;
                    }
                    if (banlist.get().stream().noneMatch(ban -> ban.getUser().equals(user))) {
                        context.reply(context.i18nFormat("modUnbanFailNotBanned", user.getAsMention()));
                        return false;
                    }
                    return true;
                });
    }

    @Override
    protected CompletionStage<Boolean> checkAuthorizationWithFeedback(@Nonnull ModActionInfo modActionInfo) {
        CommandContext context = modActionInfo.getContext();
        return CompletableFuture.completedFuture(
                context.checkInvokerPermissionsWithFeedback(Permission.BAN_MEMBERS)
                        && context.checkSelfPermissionsWithFeedback(Permission.BAN_MEMBERS)
        );
    }

    @Override
    protected Optional<String> dmForTarget(ModActionInfo modActionInfo) {
        return Optional.empty();//don't dm users upon being unbanned
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} userId OR username\n"
                + "#" + context.i18n("helpUnbanCommand");
    }
}
