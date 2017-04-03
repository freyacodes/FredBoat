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

public class ListCommand extends Command implements IMusicCommand {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ListCommand.class);

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        GuildPlayer player = PlayerRegistry.get(guild);
        player.setCurrentTC(channel);
        if (!player.isQueueEmpty()) {
            MessageBuilder mb = new MessageBuilder();

            int numberLength = 2;
            /*if(player.isShuffle()) {
                numberLength = Integer.toString(player.getSongCount()).length();
                numberLength = Math.max(2, numberLength);
            } else {
                numberLength = 2;
            }*/

            int i = 0;

            if (player.isShuffle()) {
                mb.append(I18n.get(guild).getString("listShowShuffled"));
                mb.append("\n");
                if (player.getRepeatMode() == RepeatMode.OFF)
                    mb.append("\n");
            }
            if (player.getRepeatMode() == RepeatMode.SINGLE) {
                mb.append(I18n.get(guild).getString("listShowRepeatSingle"));
                mb.append("\n\n");
            } else if (player.getRepeatMode() == RepeatMode.ALL) {
                mb.append(I18n.get(guild).getString("listShowRepeatAll"));
                mb.append("\n\n");
            }


            for (AudioTrackContext atc : player.getRemainingTracksOrdered()) {
                String status = " ";
                if (i == 0) {
                    status = player.isPlaying() ? " \\▶" : " \\\u23F8"; //Escaped play and pause emojis
                }
                mb.append("[" +
                        TextUtils.forceNDigits(i + 1, numberLength)
                        + "]", MessageBuilder.Formatting.BLOCK)
                        .append(status)
                        .append(MessageFormat.format(I18n.get(guild).getString("listAddedBy"), atc.getEffectiveTitle(), atc.getMember().getEffectiveName()))
                        .append("\n");

                if (i == 10) {
                    break;
                }

                i++;
            }

            //Now add a timestamp for how much is remaining
            long t = player.getTotalRemainingMusicTimeSeconds();
            String timestamp = TextUtils.formatTime(t * 1000L);

            int tracks = player.getRemainingTracks().size() - player.getLiveTracks().size();
            int streams = player.getLiveTracks().size();

            String desc;

            if (tracks == 0) {
                //We are only listening to streams
                desc = MessageFormat.format(I18n.get(guild).getString(streams == 1 ? "listStreamsOnlySingle" : "listStreamsOnlyMultiple"),
                        streams, streams == 1 ?
                        I18n.get(guild).getString("streamSingular") : I18n.get(guild).getString("streamPlural"));
            } else {

                desc = MessageFormat.format(I18n.get(guild).getString(tracks == 1 ? "listStreamsOrTracksSingle" : "listStreamsOrTracksMultiple"),
                        tracks, tracks == 1 ?
                        I18n.get(guild).getString("trackSingular") : I18n.get(guild).getString("trackPlural"), timestamp, streams == 0
                        ? "" : MessageFormat.format(I18n.get(guild).getString("listAsWellAsLiveStreams"), streams, streams == 1
                        ? I18n.get(guild).getString("streamSingular") : I18n.get(guild).getString("streamPlural")));
            }
            
            mb.append("\n" + desc);

            channel.sendMessage(mb.build()).queue();
        } else {
            channel.sendMessage(I18n.get(guild).getString("npNotPlaying")).queue();
        }
    }

    @Override
    public String help(Guild guild) {
        String usage = "{0}{1}\n#";
        return usage + I18n.get(guild).getString("helpListCommand");
    }
}
