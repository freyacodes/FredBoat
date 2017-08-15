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

import fredboat.audio.GuildPlayer;
import fredboat.audio.PlayerRegistry;
import fredboat.audio.queue.AudioTrackContext;
import fredboat.audio.queue.RepeatMode;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.feature.I18n;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.List;

public class ListCommand extends Command implements IMusicCommand {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ListCommand.class);

    private static final int PAGE_SIZE = 10;

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        GuildPlayer gplayer = PlayerRegistry.get(guild);
        gplayer.setCurrentTC(channel);

        if (gplayer.isQueueEmpty()) {
            channel.sendMessage(I18n.get(guild).getString("npNotPlaying")).queue();
            return;
        }

        MessageBuilder mb = new MessageBuilder();

        int page = 1;
        if(args.length >= 2) {
            try {
                page = Integer.valueOf(args[1]);
            } catch (NumberFormatException e) {
                page = 1;
            }
        }

        int tracksCount = gplayer.getTrackCount();

        int maxPages = (int) Math.ceil(((double) tracksCount - 1d)) / PAGE_SIZE + 1;

        page = Math.max(page, 1);
        page = Math.min(page, maxPages);

        int i = (page - 1) * PAGE_SIZE;
        int listEnd = (page - 1) * PAGE_SIZE + PAGE_SIZE;
        listEnd = Math.min(listEnd, tracksCount);

        int numberLength = Integer.toString(listEnd).length();


        List<AudioTrackContext> sublist = gplayer.getTracksInRange(i, listEnd);

        if (gplayer.isShuffle()) {
            mb.append(I18n.get(guild).getString("listShowShuffled"));
            mb.append("\n");
            if (gplayer.getRepeatMode() == RepeatMode.OFF)
                mb.append("\n");
        }
        if (gplayer.getRepeatMode() == RepeatMode.SINGLE) {
            mb.append(I18n.get(guild).getString("listShowRepeatSingle"));
            mb.append("\n");
        } else if (gplayer.getRepeatMode() == RepeatMode.ALL) {
            mb.append(I18n.get(guild).getString("listShowRepeatAll"));
            mb.append("\n");
        }

        mb.append(MessageFormat.format(I18n.get(guild).getString("listPageNum"), page, maxPages));
        mb.append("\n");
        mb.append("\n");

        for (AudioTrackContext atc : sublist) {
            String status = " ";
            if (i == 0) {
                status = gplayer.isPlaying() ? " \\▶" : " \\\u23F8"; //Escaped play and pause emojis
            }
            Member member = guild.getMemberById(atc.getUserId());
            String username = member != null ? member.getEffectiveName() : guild.getSelfMember().getEffectiveName();
            mb.append("[" +
                    TextUtils.forceNDigits(i + 1, numberLength)
                    + "]", MessageBuilder.Formatting.BLOCK)
                    .append(status)
                    .append(MessageFormat.format(I18n.get(guild).getString("listAddedBy"), atc.getEffectiveTitle(), username))
                    .append("\n");

            if (i == listEnd) {
                break;
            }

            i++;
        }

        //Now add a timestamp for how much is remaining
        String timestamp = TextUtils.formatTime(gplayer.getTotalRemainingMusicTimeMillis());

        long streamsCount = gplayer.getStreamsCount();
        long numTracks = tracksCount - streamsCount;

        String description;

        if (numTracks == 0) {
            //We are only listening to streams
            description = MessageFormat.format(I18n.get(guild).getString(streamsCount == 1 ? "listStreamsOnlySingle" : "listStreamsOnlyMultiple"),
                    streamsCount, streamsCount == 1 ?
                    I18n.get(guild).getString("streamSingular") : I18n.get(guild).getString("streamPlural"));
        } else {

            description = MessageFormat.format(I18n.get(guild).getString(numTracks == 1 ? "listStreamsOrTracksSingle" : "listStreamsOrTracksMultiple"),
                    numTracks, numTracks == 1 ?
                            I18n.get(guild).getString("trackSingular") : I18n.get(guild).getString("trackPlural"), timestamp, streamsCount == 0
                            ? "" : MessageFormat.format(I18n.get(guild).getString("listAsWellAsLiveStreams"), streamsCount, streamsCount == 1
                    ? I18n.get(guild).getString("streamSingular") : I18n.get(guild).getString("streamPlural")));
        }

        mb.append("\n").append(description);

        channel.sendMessage(mb.build()).queue();

    }

    @Override
    public String help(Guild guild) {
        String usage = "{0}{1}\n#";
        return usage + I18n.get(guild).getString("helpListCommand");
    }
}
