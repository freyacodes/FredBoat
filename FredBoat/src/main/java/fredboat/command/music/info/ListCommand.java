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

package fredboat.command.music.info;

import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.PlayerRegistry;
import fredboat.audio.queue.AudioTrackContext;
import fredboat.audio.queue.RepeatMode;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.feature.I18n;
import fredboat.messaging.CentralMessaging;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;

public class ListCommand extends Command implements IMusicCommand {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ListCommand.class);

    private static final int PAGE_SIZE = 10;

    @Override
    public void onInvoke(CommandContext context) {
        GuildPlayer player = PlayerRegistry.get(context.guild);
        player.setCurrentTC(context.channel);
        ResourceBundle i18n = I18n.get(context.guild);

        if(player.isQueueEmpty()) {
            context.reply(i18n.getString("npNotPlaying"));
            return;
        }

        MessageBuilder mb = CentralMessaging.getClearThreadLocalMessageBuilder();

        int page = 1;
        if (context.args.length >= 2) {
            try {
                page = Integer.valueOf(context.args[1]);
            } catch (NumberFormatException e) {
                page = 1;
            }
        }

        int tracksCount = player.getTrackCount();
        int maxPages = (int) Math.ceil(((double) tracksCount - 1d)) / PAGE_SIZE + 1;

        page = Math.max(page, 1);
        page = Math.min(page, maxPages);

        int i = (page - 1) * PAGE_SIZE;
        int listEnd = (page - 1) * PAGE_SIZE + PAGE_SIZE;
        listEnd = Math.min(listEnd, tracksCount);

        int numberLength = Integer.toString(listEnd).length();

        List<AudioTrackContext> sublist = player.getTracksInRange(i, listEnd);

        if (player.isShuffle()) {
            mb.append(I18n.get(context, "listShowShuffled"));
            mb.append("\n");
            if (player.getRepeatMode() == RepeatMode.OFF)
                mb.append("\n");
        }
        if (player.getRepeatMode() == RepeatMode.SINGLE) {
            mb.append(i18n.getString("listShowRepeatSingle"));
            mb.append("\n");
        } else if (player.getRepeatMode() == RepeatMode.ALL) {
            mb.append(i18n.getString("listShowRepeatAll"));
            mb.append("\n");
        }

        mb.append(MessageFormat.format(i18n.getString("listPageNum"), page, maxPages));
        mb.append("\n");
        mb.append("\n");

        for (AudioTrackContext atc : sublist) {
            String status = " ";
            if (i == 0) {
                status = player.isPlaying() ? " \\▶" : " \\\u23F8"; //Escaped play and pause emojis
            }
            Member member = context.guild.getMemberById(atc.getUserId());
            String username = member != null ? member.getEffectiveName() : context.guild.getSelfMember().getEffectiveName();
            mb.append("[" +
                    TextUtils.forceNDigits(i + 1, numberLength)
                    + "]", MessageBuilder.Formatting.BLOCK)
                    .append(status)
                    .append(MessageFormat.format(i18n.getString("listAddedBy"), atc.getEffectiveTitle(), username, TextUtils.formatTime(atc.getEffectiveDuration())))
                    .append("\n");

            if (i == listEnd) {
                break;
            }

            i++;
        }

        //Now add a timestamp for how much is remaining
        String timestamp = TextUtils.formatTime(player.getTotalRemainingMusicTimeMillis());

        long streams = player.getStreamsCount();
        long numTracks = tracksCount - streams;

        String desc;

        if (numTracks == 0) {
            //We are only listening to streams
            desc = MessageFormat.format(i18n.getString(streams == 1 ? "listStreamsOnlySingle" : "listStreamsOnlyMultiple"),
                    streams, streams == 1 ?
                            i18n.getString("streamSingular") : i18n.getString("streamPlural"));
        } else {

            desc = MessageFormat.format(i18n.getString(numTracks == 1 ? "listStreamsOrTracksSingle" : "listStreamsOrTracksMultiple"),
                    numTracks, numTracks == 1 ?
                            i18n.getString("trackSingular") : i18n.getString("trackPlural"), timestamp, streams == 0
                            ? "" : MessageFormat.format(i18n.getString("listAsWellAsLiveStreams"), streams, streams == 1
                            ? i18n.getString("streamSingular") : i18n.getString("streamPlural")));
        }

        mb.append("\n").append(desc);

        context.reply(mb.build());

    }

    @Override
    public String help(Guild guild) {
        String usage = "{0}{1}\n#";
        return usage + I18n.get(guild).getString("helpListCommand");
    }
}
