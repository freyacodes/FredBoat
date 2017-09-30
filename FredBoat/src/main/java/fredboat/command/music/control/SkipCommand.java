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

import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.PlayerRegistry;
import fredboat.audio.queue.AudioTrackContext;
import fredboat.command.util.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import fredboat.perms.PermsUtil;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import javax.annotation.Nonnull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkipCommand extends Command implements IMusicCommand, ICommandRestricted {
    private static final String TRACK_RANGE_REGEX = "^(0?\\d+)-(0?\\d+)$";
    private static final Pattern trackRangePattern = Pattern.compile(TRACK_RANGE_REGEX);

    /**
     * Represents the relationship between a <b>guild's id</b> and <b>skip cooldown</b>.
     */
    private static Map<String, Long> guildIdToLastSkip = new HashMap<String, Long>();

    /**
     * The default cooldown for calling the {@link #onInvoke} method in milliseconds.
     */
    private static final int SKIP_COOLDOWN = 500;

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        GuildPlayer player = PlayerRegistry.get(context.guild);
        player.setCurrentTC(context.channel);

        if (player.isQueueEmpty()) {
            context.reply(context.i18n("skipEmpty"));
            return;
        }

        if (isOnCooldown(context.guild)) {
            return;
        } else {
            guildIdToLastSkip.put(context.guild.getId(), System.currentTimeMillis());
        }

        String[] args = context.args;
        if (args.length == 1) {
            skipNext(context);
        } else if (args.length == 2 && StringUtils.isNumeric(args[1])) {
            skipGivenIndex(player, context);
        } else if (args.length == 2 && trackRangePattern.matcher(args[1]).matches()) {
            skipInRange(player, context);
        } else if (args.length >= 2 && context.msg.getMentionedUsers().size() > 0) {
            skipUser(player, context, context.msg.getMentionedUsers());
        } else {
            HelpCommand.sendFormattedCommandHelp(context);
        }
    }

    /**
     * Specifies whether the <B>skip command </B>is on cooldown.
     *
     * @param guild The guild where the <B>skip command</B> was called.
     * @return {@code true} if the elapsed time since the <B>skip command</B> is less than or equal to
     * {@link #SKIP_COOLDOWN}; otherwise, {@code false}.
     */
    private boolean isOnCooldown(Guild guild) {
        long currentTIme = System.currentTimeMillis();
        return currentTIme - guildIdToLastSkip.getOrDefault(guild.getId(), 0L) <= SKIP_COOLDOWN;
    }

    private void skipGivenIndex(GuildPlayer player, CommandContext context) {
        int givenIndex;
        try {
            givenIndex = Integer.parseInt(context.args[1]);
        } catch (NumberFormatException e) {
            context.reply(context.i18nFormat("skipOutOfBounds", context.args[1], player.getTrackCount()));
            return;
        }

        if (givenIndex == 1) {
            skipNext(context);
            return;
        }

        if (player.getRemainingTracks().size() < givenIndex) {
            context.reply(context.i18nFormat("skipOutOfBounds", givenIndex, player.getTrackCount()));
            return;
        } else if (givenIndex < 1) {
            context.reply(context.i18n("skipNumberTooLow"));
            return;
        }

        AudioTrackContext atc = player.getTracksInRange(givenIndex - 1, givenIndex).get(0);

        String successMessage = context.i18nFormat("skipSuccess", givenIndex, atc.getEffectiveTitle());
        player.skipTracksForMemberPerms(context, Collections.singletonList(atc.getTrackId()), successMessage);
    }

    private void skipInRange(GuildPlayer player, CommandContext context) {
        Matcher trackMatch = trackRangePattern.matcher(context.args[1]);
        if (!trackMatch.find()) return;

        int startTrackIndex;
        int endTrackIndex;
        String tmp = "";
        try {
            tmp = trackMatch.group(1);
            startTrackIndex = Integer.parseInt(tmp);
            tmp = trackMatch.group(2);
            endTrackIndex = Integer.parseInt(tmp);
        } catch (NumberFormatException e) {
            context.reply(context.i18nFormat("skipOutOfBounds", tmp, player.getTrackCount()));
            return;
        }

        if (startTrackIndex < 1) {
            context.reply(context.i18n("skipNumberTooLow"));
            return;
        } else if (endTrackIndex < startTrackIndex) {
            context.reply(context.i18n("skipRangeInvalid"));
            return;
        } else if (player.getTrackCount() < endTrackIndex) {
            context.reply(context.i18nFormat("skipOutOfBounds", endTrackIndex, player.getTrackCount()));
            return;
        }

        List<Long> trackIds = player.getTrackIdsInRange(startTrackIndex - 1, endTrackIndex);

        String successMessage = context.i18nFormat("skipRangeSuccess",
                TextUtils.forceNDigits(startTrackIndex, 2),
                TextUtils.forceNDigits(endTrackIndex, 2));
        player.skipTracksForMemberPerms(context, trackIds, successMessage);
    }

    private void skipUser(GuildPlayer player, CommandContext context, List<User> users) {

        if (!PermsUtil.checkPerms(PermissionLevel.DJ, context.invoker)) {

            if (users.size() == 1) {
                User user = users.get(0);

                if (context.invoker.getUser().getIdLong() != user.getIdLong()) {
                    context.reply(context.i18n("skipDeniedTooManyTracks"));
                    return;
                }

            } else {
                context.reply(context.i18n("skipDeniedTooManyTracks"));
                return;
            }
        }

        List<AudioTrackContext> listAtc = player.getTracksInRange(0, player.getTrackCount());
        List<Long> userAtcIds = new ArrayList<>();
        List<User> affectedUsers = new ArrayList<>();

        for (User user : users) {
            for (AudioTrackContext atc : listAtc) {
                if (atc.getUserId() == user.getIdLong()) {
                    userAtcIds.add(atc.getTrackId());

                    if (!affectedUsers.contains(user)) {
                        affectedUsers.add(user);
                    }
                }
            }
        }


        if (userAtcIds.size() > 0) {

            String title = player.getPlayingTrack().getEffectiveTitle();
            player.skipTracks(userAtcIds);

            if (affectedUsers.size() > 1) {
                context.reply(context.i18nFormat("skipUsersMultiple", ("`" + userAtcIds.size() + "`"), ("**" + affectedUsers.size() + "**")));
            } else {
                User user = affectedUsers.get(0);
                String userName = "**" + user.getName() + "#" + user.getDiscriminator() + "**";
                if (userAtcIds.size() == 1) {
                    context.reply(context.i18nFormat("skipUserSingle", "**" + title + "**", userName));
                } else {
                    context.reply(context.i18nFormat("skipUserMultiple", "`" + userAtcIds.size() + "`", userName));
                }
            }
        } else {
            context.reply(context.i18n("skipUserNoTracks"));
        }
    }

    private void skipNext(CommandContext context) {
        GuildPlayer player = PlayerRegistry.get(context.guild);
        AudioTrackContext atc = player.getPlayingTrack();
        if (atc == null) {
            context.reply(context.i18n("skipTrackNotFound"));
        } else {
            String successMessage = context.i18nFormat("skipSuccess", 1, atc.getEffectiveTitle());
            player.skipTracksForMemberPerms(context, Collections.singletonList(atc.getTrackId()), successMessage);
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        String usage = "{0}{1} OR {0}{1} n OR {0}{1} n-m OR {0}{1} @Users...\n#";
        return usage + context.i18n("helpSkipCommand");
    }

    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.USER;
    }
}
