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

import fredboat.Config;
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.PlayerRegistry;
import fredboat.audio.queue.AudioTrackContext;
import fredboat.command.util.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.feature.I18n;
import fredboat.perms.PermissionLevel;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        GuildPlayer player = PlayerRegistry.get(guild);
        player.setCurrentTC(channel);

        if (player.isQueueEmpty()) {
            channel.sendMessage(I18n.get(guild).getString("skipEmpty")).queue();
            return;
        }

        if (isOnCooldown(guild)) {
            return;
        } else {
            guildIdToLastSkip.put(guild.getId(), System.currentTimeMillis());
        }

        if (args.length == 1) {
            skipNext(guild, channel, invoker, args);
        } else if (args.length == 2 && StringUtils.isNumeric(args[1])) {
            skipGivenIndex(player, channel, invoker, args);
        } else if (args.length == 2 && trackRangePattern.matcher(args[1]).matches()) {
            skipInRange(player, channel, invoker, args);
        } else {
            String command = args[0].substring(Config.CONFIG.getPrefix().length());
            HelpCommand.sendFormattedCommandHelp(guild, channel, invoker, command);
        }
    }

    /**
     * Specifies whether the <B>skip command </B>is on cooldown.
     * @param guild The guild where the <B>skip command</B> was called.
     * @return {@code true} if the elapsed time since the <B>skip command</B> is less than or equal to
     * {@link #SKIP_COOLDOWN}; otherwise, {@code false}.
     */
    private boolean isOnCooldown(Guild guild) {
        long currentTIme = System.currentTimeMillis();
        return currentTIme - guildIdToLastSkip.getOrDefault(guild.getId(), 0L) <= SKIP_COOLDOWN;
    }

    private void skipGivenIndex(GuildPlayer player, TextChannel channel, Member invoker, String[] args) {
        int givenIndex = Integer.parseInt(args[1]);

        if (givenIndex == 1) {
            skipNext(channel.getGuild(), channel, invoker, args);
            return;
        }

        if (player.getRemainingTracks().size() < givenIndex) {
            channel.sendMessage(MessageFormat.format(I18n.get(channel.getGuild()).getString("skipOutOfBounds"), givenIndex, player.getRemainingTracks().size())).queue();
            return;
        } else if (givenIndex < 1) {
            channel.sendMessage(I18n.get(channel.getGuild()).getString("skipNumberTooLow")).queue();
            return;
        }

        AudioTrackContext atc = player.getAudioTrackProvider().getAsListOrdered().get(givenIndex - 2);
        player.skipTracksForMemberPerms(channel, invoker, atc);

        Pair<Boolean, String> result = player.skipTracksForMemberPerms(channel, invoker, atc);
        if (result.getLeft()) {
            channel.sendMessage(MessageFormat.format(I18n.get(channel.getGuild()).getString("skipSuccess"), givenIndex, atc.getEffectiveTitle())).queue();
        }
    }

    private void skipInRange(GuildPlayer player, TextChannel channel, Member invoker, String[] args) {
        Matcher trackMatch = trackRangePattern.matcher(args[1]);
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
            channel.sendMessage(MessageFormat.format(I18n.get(channel.getGuild()).getString("skipOutOfBounds"), tmp, player.getRemainingTracks().size())).queue();
            return;
        }

        if (startTrackIndex < 1) {
            channel.sendMessage(I18n.get(channel.getGuild()).getString("skipNumberTooLow")).queue();
            return;
        } else if (endTrackIndex < startTrackIndex) {
            channel.sendMessage(I18n.get(channel.getGuild()).getString("skipRangeInvalid")).queue();
            return;
        } else if (player.getRemainingTracks().size() < endTrackIndex) {
            channel.sendMessage(MessageFormat.format(I18n.get(channel.getGuild()).getString("skipOutOfBounds"), endTrackIndex, player.getRemainingTracks().size())).queue();
            return;
        }

        List<AudioTrackContext> tracks = new ArrayList<>();
        if (startTrackIndex == 1) {
            //Add the currently playing track
            tracks.add(player.getPlayingTrack());
        }
        tracks.addAll(player.getAudioTrackProvider().getInRange(startTrackIndex - 2, endTrackIndex - 2));

        Pair<Boolean, String> pair = player.skipTracksForMemberPerms(channel, invoker, tracks);

        if (pair.getLeft()) {
            channel.sendMessage(MessageFormat.format(I18n.get(channel.getGuild()).getString("skipRangeSuccess"),
                    TextUtils.forceNDigits(startTrackIndex, 2),
                    TextUtils.forceNDigits(endTrackIndex, 2))).queue();
        }
    }

    private void skipNext(Guild guild, TextChannel channel, Member invoker, String[] args) {
        GuildPlayer player = PlayerRegistry.get(guild);
        AudioTrackContext atc = player.getPlayingTrack();
        if (atc == null) {
            channel.sendMessage(I18n.get(guild).getString("skipTrackNotFound")).queue();
        } else {
            Pair<Boolean, String> result = player.skipTracksForMemberPerms(channel, invoker, atc);
            if (result.getLeft()) {
                channel.sendMessage(MessageFormat.format(I18n.get(guild).getString("skipSuccess"), 1, atc.getEffectiveTitle())).queue();
            }
        }
    }

    @Override
    public String help(Guild guild) {
        String usage = "{0}{1} OR {0}{1} n OR {0}{1} n-m\n#";
        return usage + I18n.get(guild).getString("helpSkipCommand");
    }

    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.USER;
    }
}
