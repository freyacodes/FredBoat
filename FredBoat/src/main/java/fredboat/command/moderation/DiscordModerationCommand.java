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

import fredboat.command.info.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IModerationCommand;
import fredboat.feature.I18n;
import fredboat.feature.metrics.Metrics;
import fredboat.main.Launcher;
import fredboat.messaging.CentralMessaging;
import fredboat.util.ArgumentUtil;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.requests.ErrorResponse;
import net.dv8tion.jda.core.requests.restaction.AuditableRestAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by napster on 27.01.18.
 * <p>
 * Base class for Kick, Ban, Softban commands, and possibly others
 * <p>
 * Conventions for these are as follows:
 * ;;command @memberMention audit reason with whitespace allowed
 * or
 * ;;command memberNameThatWillBeFuzzySearched audit reason with whitespace allowed
 * or
 * ;;command id snowflakeUserId audit reason with whitespace allowed
 *
 * @param <T> Class of the return value of the AuditableRestAction of this command
 */
public abstract class DiscordModerationCommand<T> extends Command implements IModerationCommand {

    private static final Logger log = LoggerFactory.getLogger(DiscordModerationCommand.class);

    public static final int AUDIT_LOG_MAX_LENGTH = 512; // 512 is a hard limit by discord
    public static final int DEFAULT_DELETE_DAYS = 1;    // same as the 24hours default in the discord client

    // https://regex101.com/r/7ss0ho/1
    // group 1: userid from a mention at the left side of the input string
    // group 2: rest of the command (right side of input string), can be empty
    private static final Pattern USER_MENTION = Pattern.compile("^<@!?([0-9]+)>(.*)$");

    // https://regex101.com/r/f6uIGA/1
    // group 1: userid a raw number with atleast 15 digits at the left side of the input string
    // group 2: rest of the command (right side of input string), can be empty
    private static final Pattern USER_ID = Pattern.compile("^([0-9]{15,})(.*)$");


    protected DiscordModerationCommand(@Nonnull String name, String... aliases) {
        super(name, aliases);
    }

    /**
     * Returns the rest action to be issued by this command
     */
    @Nonnull
    protected abstract AuditableRestAction<T> modAction(@Nonnull ModActionInfo modActionInfo);

    /**
     * @return true if this mod action requires the target to be a member of the guild (example: kick). The invoker will
     * be informed about this requirement if their input does not yield a member of the guild
     */
    protected abstract boolean requiresMember();

    /**
     * Returns the success handler for the mod action
     */
    @Nonnull
    protected abstract Consumer<T> onSuccess(@Nonnull ModActionInfo modActionInfo);

    /**
     * Returns the failure handler for the mod action
     */
    @Nonnull
    protected abstract Consumer<Throwable> onFail(@Nonnull ModActionInfo modActionInfo);

    /**
     * Checks ourself, the context.invoker, and the target member for proper authorization, and replies with appropriate
     * feedback if any check fails. Some guild specific checks are only run if the target member is not null, assuming
     * that in that case a mod action is being performed on a user that is not a member of the guild (for example,
     * banning a user that left the guild)
     *
     * @return true if all checks are successful, false otherwise. In case the return value is false, the invoker has
     * received feedback as to why.
     */
    protected abstract CompletionStage<Boolean> checkAuthorizationWithFeedback(@Nonnull ModActionInfo modActionInfo);

    /**
     * Parse the context of the command into a ModActionInfo.
     * If that is not possible, reply a reason to the user, and return null.
     * <p>
     * Can be overridden for custom behaviour.
     * <p>
     * NOTE: Since it may be necessary to poll ddoscord to identify a User, this method should not be called blocking.
     *
     * @return an optional containing a ModActionInfo. Optional may be empty if something could not be parsed.  In that
     * the user will have been notified of the issue, no further action by the caller is necessary
     */
    @Nonnull
    protected CompletionStage<Optional<ModActionInfo>> parseModActionInfoWithFeedback(@Nonnull CommandContext context) {
        //Ensure we have a search term
        if (!context.hasArguments()) {
            HelpCommand.sendFormattedCommandHelp(context);
            return CompletableFuture.completedFuture(Optional.empty());
        }

        //this variable is updated throughout the steps of the parsing with the remaining parts of the string
        String processed = context.rawArgs.trim();

        //parsing keep flag
        final boolean keep;

        if (processed.endsWith("--keep")) {
            keep = true;
            processed = processed.substring(0, processed.length() - "--keep".length());
        } else if (processed.endsWith("-k")) {
            keep = true;
            processed = processed.substring(0, processed.length() - "-k".length());
        } else {
            keep = false;
        }

        //parsing target and reason
        long targetId = -1;
        String reason = "";
        Matcher mentionMatcher = USER_MENTION.matcher(processed);
        String userIdGroup = "";
        if (mentionMatcher.matches()) {
            userIdGroup = mentionMatcher.group(1);
            reason = mentionMatcher.group(2);
        }
        if (userIdGroup.isEmpty()) {
            Matcher idMatcher = USER_ID.matcher(processed);
            if (idMatcher.matches()) {
                userIdGroup = idMatcher.group(1);
                reason = idMatcher.group(2);
            }
        }

        if (!userIdGroup.isEmpty()) {
            try {
                targetId = Long.parseUnsignedLong(userIdGroup);
            } catch (NumberFormatException e) {
                log.error("Failed to parse unsigned long from {}. Fix the regexes?", userIdGroup);
            }
        }

        CompletionStage<Optional<User>> userFuture;
        final long finalTargetId = targetId;
        if (targetId == -1) {
            final String search = context.args[0];
            reason = processed.replaceFirst(Pattern.quote(search), "").trim();
            userFuture = fromFuzzySearch(context, search);
        } else {
            userFuture = fetchUser(targetId).thenApply(optionalUser -> {
                if (!optionalUser.isPresent()) {
                    String reply = context.i18nFormat("parseNotAUser", "`" + finalTargetId + "`");
                    reply += "\n" + context.i18nFormat("parseSnowflakeIdHelp", HelpCommand.LINK_DISCORD_DOCS_IDS);
                    context.reply(reply);
                }
                return optionalUser;
            });
        }

        final String finalReason = reason;
        return userFuture
                .thenCompose(optionalUser -> {
                    if (!optionalUser.isPresent()) {
                        //feedback has been given at this point
                        return CompletableFuture.completedFuture(Optional.empty());
                    }

                    User existingUser = optionalUser.get();
                    return checkPreconditionWithFeedback(existingUser, context)
                            .thenApply(preconditionMet -> {
                                if (!preconditionMet) {
                                    return Optional.empty();
                                }

                                Member guildMember = context.guild.getMember(existingUser);
                                return Optional.of(new ModActionInfo(context, guildMember, existingUser, keep, finalReason));
                            });
                });
    }

    /**
     * @param userId The user to fetch from discord.
     * @return A future that will complete with an optional user. The optional will be empty if Discord doesn't know
     * any user by the userId that was passed to this method, otherwise if will contain a (possibly fake from JDAs point
     * of view) user.
     * <p>
     * This method will handle ( = log) any unexpected exceptions, and return an empty Optional instead. Callers of this
     * don't need to handle throwables in the return completion stage.
     */
    @CheckReturnValue
    private CompletionStage<Optional<User>> fetchUser(long userId) {
        User user = Launcher.getBotController().getJdaEntityProvider().getUserById(userId);
        if (user != null) {
            return CompletableFuture.completedFuture(Optional.of(user));
        }
        CompletionStage<User> userFuture = Launcher.getBotController().getJdaEntityProvider().anyShard()
                .retrieveUserById(userId).submit();

        return userFuture.handle((u, t) -> {
            if (t != null) {
                if (t instanceof ErrorResponseException
                        && ((ErrorResponseException) t).getErrorResponse() == ErrorResponse.UNKNOWN_USER) {
                    Metrics.successfulRestActions.labels("retrieveUserById").inc();
                    return Optional.empty(); //no need to log this one
                }
                log.error("Unexpected exception retrieving user from discord", t);
                Metrics.failedRestActions.labels("retrieveUserById").inc();
            } else {
                Metrics.successfulRestActions.labels("retrieveUserById").inc();
            }
            return Optional.ofNullable(u);
        });
    }

    /**
     * @return Does a fuzzy search for a user. Default implementation fuzzy searches the guild of the context.
     */
    @CheckReturnValue
    protected CompletionStage<Optional<User>> fromFuzzySearch(CommandContext context, String searchTerm) {
        Member member = ArgumentUtil.checkSingleFuzzyMemberSearchResult(context, searchTerm, true);
        if (member == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return CompletableFuture.completedFuture(Optional.of(member.getUser()));
    }

    /**
     * Check preconditions of a command. Examples:
     * - To kick a user, they need to be a member of a guild.
     * - To unban a user, they need to be on the banlist.
     * If the precondition is not met, this method should give feedback to the invoker about the unmet precondition.
     */
    protected CompletionStage<Boolean> checkPreconditionWithFeedback(User user, CommandContext context) {
        if (requiresMember() && !context.guild.isMember(user)) {
            context.reply(context.i18nFormat("parseNotAMember", user.getAsMention()));
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public final void onInvoke(@Nonnull CommandContext context) {
        ExecutorService executor = Launcher.getBotController().getExecutor();
        parseModActionInfoWithFeedback(context)
                .exceptionally(t -> {
                    TextUtils.handleException("Failed to parse a moderation action", t, context);
                    return Optional.empty();
                })
                .thenComposeAsync(optionalModActionInfo -> {
                    if (optionalModActionInfo == null || !optionalModActionInfo.isPresent()) {
                        return CompletableFuture.completedFuture(null);
                    }
                    final ModActionInfo modActionInfo = optionalModActionInfo.get();
                    return checkAuthorizationWithFeedback(modActionInfo)
                            .exceptionally(t -> {
                                TextUtils.handleException("Failed to check authorization for a moderation command",
                                        t, context);
                                return false;
                            })
                            .thenAccept(hasAuthorization -> {
                                if (hasAuthorization) {
                                    Runnable modAction = () -> modAction(modActionInfo)
                                            .queue(onSuccess(modActionInfo), onFail(modActionInfo));

                                    // dm the target about the action. do this before executing the action.
                                    Optional<String> dm = dmForTarget(modActionInfo);
                                    if (dm.isPresent() && !modActionInfo.getTargetUser().isFake()) {
                                        context.sendPrivate(modActionInfo.getTargetUser(), dm.get(),
                                                //run the mod action no matter if the dm fails or succeeds
                                                __ -> modAction.run(),
                                                __ -> modAction.run()
                                        );
                                    } else { //no dm -> just do the mod action
                                        modAction.run();
                                    }
                                }
                            });
                }, executor)
                .whenComplete((ignored, t) -> {
                    if (t != null) {
                        TextUtils.handleException("Failed to execute a moderation command", t, context);
                    }
                });
    }

    /**
     * @return An optional message that will be attempted to be sent to the target user before executing the moderation
     * action against them, so that they know what happened to them.
     */
    protected abstract Optional<String> dmForTarget(ModActionInfo modActionInfo);

    @Nonnull
    protected static String formatReasonForAuditLog(@Nonnull String plainReason, @Nonnull Member invoker,
                                                    @Nonnull User target) {
        String i18nAuditLogMessage = MessageFormat.format(I18n.get(invoker.getGuild()).getString("modAuditLogMessage"),
                TextUtils.asString(invoker), TextUtils.asString(target)) + ", ";
        int leftOverAudioLogLength = AUDIT_LOG_MAX_LENGTH - i18nAuditLogMessage.length();
        return i18nAuditLogMessage + (plainReason.length() > leftOverAudioLogLength ?
                plainReason.substring(0, leftOverAudioLogLength) : plainReason);
    }

    /**
     * Fetch the banlist of the guild of the context. If that's not possible of fails, the context is informed about the
     * issue, and an empty optional returned.
     */
    protected static CompletionStage<Optional<List<Guild.Ban>>> fetchBanlist(@Nonnull CommandContext context) {
        //need ban perms to read the ban list
        if (!context.checkSelfPermissionsWithFeedback(Permission.BAN_MEMBERS)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        final Guild guild = context.guild;
        return guild.getBanList().submit()
                .handle((result, t) -> {
                    if (t == null) {
                        Metrics.successfulRestActions.labels("getBanList").inc();
                        return Optional.of(result);
                    }
                    Metrics.failedRestActions.labels("getBanList").inc();

                    // dont need to catch InsufficientPermissionExceptions here, because we checked that we have
                    // banning permissions at the start of this method. any exception here is an unexpected one
                    // that we don't have any detailed info for the user to show for.
                    CentralMessaging.getJdaRestActionFailureHandler(String.format("Failed to fetch banlist of guild %s",
                            guild)).accept(t);
                    context.replyWithName(context.i18n("modBanlistFail"));
                    return Optional.empty();
                });
    }


    //pass information between methods called inside this class and its subclasses
    protected static class ModActionInfo {

        @Nonnull
        private final CommandContext context;
        @Nullable
        private final Member targetMember;
        @Nonnull
        private final User targetUser;
        private final boolean keepMessages;
        @Nonnull
        private final String plainReason;

        public ModActionInfo(@Nonnull CommandContext context, @Nullable Member targetMember, @Nonnull User targetUser,
                             boolean keepMessages, @Nullable String rawReason) {
            this.context = context;
            this.targetMember = targetMember;
            this.targetUser = targetUser;
            this.keepMessages = keepMessages;
            this.plainReason = context.i18n("modReason") + ": " +
                    ((rawReason == null || rawReason.trim().isEmpty())
                            ? context.i18n("modAudioLogNoReason")
                            : rawReason);
        }

        @Nonnull
        public CommandContext getContext() {
            return context;
        }

        @Nullable
        public Member getTargetMember() {
            return targetMember;
        }

        /**
         * May be a fake user, so do not DM them.
         */
        @Nonnull
        public User getTargetUser() {
            return targetUser;
        }

        public boolean isKeepMessages() {
            return keepMessages;
        }

        /**
         * Full reason, including a prefaced "Reason: ".
         * Do not use for audit log, use {@link ModActionInfo#getFormattedReason()} instead.
         */
        @Nonnull
        public String getPlainReason() {
            return plainReason;
        }

        /**
         * Suitable for display discord audit logs. Will add information about who issued the ban, and respect Discords
         * lenght limit for audit log reasons.
         */
        public String getFormattedReason() {
            return formatReasonForAuditLog(this.plainReason, this.context.invoker, this.targetUser);
        }

        public String targetAsString() {
            if (this.targetMember != null) {
                return TextUtils.asString(this.targetMember);
            } else {
                return TextUtils.asString(this.targetUser);
            }
        }
    }
}
