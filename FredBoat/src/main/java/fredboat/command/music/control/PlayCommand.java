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

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fredboat.Config;
import fredboat.audio.player.*;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.feature.I18n;
import fredboat.perms.PermissionLevel;
import fredboat.util.TextUtils;
import fredboat.util.rest.SearchUtil;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Message.Attachment;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayCommand extends Command implements IMusicCommand, ICommandRestricted {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(PlayCommand.class);
    private final List<SearchUtil.SearchProvider> searchProviders;
    private static final JoinCommand JOIN_COMMAND = new JoinCommand();

    public PlayCommand(SearchUtil.SearchProvider... searchProviders) {
        this.searchProviders = Arrays.asList(searchProviders);
    }

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        if (!invoker.getVoiceState().inVoiceChannel()) {
            channel.sendMessage(I18n.get(guild).getString("playerUserNotInChannel")).queue();
            return;
        }

        if (!PlayerLimitManager.checkLimitResponsive(channel)) return;

        if (!message.getAttachments().isEmpty()) {
            GuildPlayer player = PlayerRegistry.get(guild);
            player.setCurrentTC(channel);
            
            for (Attachment atc : message.getAttachments()) {
                player.queue(atc.getUrl(), channel, invoker);
            }
            
            player.setPause(false);
            
            return;
        }

        if (args.length < 2) {
            handleNoArguments(guild, channel, invoker, message);
            return;
        }

        //What if we want to select a selection instead?
        if (args.length == 2 && StringUtils.isNumeric(args[1])){
            SelectCommand.select(guild, channel, invoker, message, args);
            return;
        }

        //Search youtube for videos and let the user select a video
        if (!args[1].startsWith("http")) {
            searchForVideos(guild, channel, invoker, message, args);
            return;
        }

        GuildPlayer player = PlayerRegistry.get(guild);
        player.setCurrentTC(channel);

        player.queue(args[1], channel, invoker);
        player.setPause(false);

        try {
            message.delete().queue();
        } catch (Exception ignored) {

        }
    }

    private void handleNoArguments(Guild guild, TextChannel channel, Member invoker, Message message) {
        GuildPlayer player = PlayerRegistry.get(guild);
        if (player.isQueueEmpty()) {
            channel.sendMessage(I18n.get(guild).getString("playQueueEmpty")).queue();
        } else if (player.isPlaying()) {
            channel.sendMessage(I18n.get(guild).getString("playAlreadyPlaying")).queue();
        } else if (player.getHumanUsersInVC().isEmpty() && LavalinkManager.ins.getConnectedChannel(guild) != null) {
            channel.sendMessage(I18n.get(guild).getString("playVCEmpty")).queue();
        } else if(LavalinkManager.ins.getConnectedChannel(guild) == null) {
            // When we just want to continue playing, but the user is not in a VC
            JOIN_COMMAND.onInvoke(guild, channel, invoker, message, new String[0]);
            if(LavalinkManager.ins.getConnectedChannel(guild) != null || guild.getAudioManager().isAttemptingToConnect()) {
                player.play();
                channel.sendMessage(I18n.get(guild).getString("playWillNowPlay")).queue();
            }
        } else {
            player.play();
            channel.sendMessage(I18n.get(guild).getString("playWillNowPlay")).queue();
        }
    }

    private void searchForVideos(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        Matcher m = Pattern.compile("\\S+\\s+(.*)").matcher(message.getRawContent());
        m.find();
        String query = m.group(1);
        
        //Now remove all punctuation
        query = query.replaceAll(SearchUtil.PUNCTUATION_REGEX, "");

        String finalQuery = query;
        channel.sendMessage(I18n.get(guild).getString("playSearching").replace("{q}", query)).queue(outMsg -> {
            AudioPlaylist list;
            try {
                list = SearchUtil.searchForTracks(finalQuery, searchProviders);
            } catch (SearchUtil.SearchingException e) {
                channel.sendMessage(I18n.get(guild).getString("playYoutubeSearchError")).queue();
                log.error("YouTube search exception", e);
                return;
            }

            if (list == null || list.getTracks().isEmpty()) {
                outMsg.editMessage(I18n.get(guild).getString("playSearchNoResults").replace("{q}", finalQuery)).queue();
            } else {
                //Clean up any last search by this user
                GuildPlayer player = PlayerRegistry.get(guild);

                //Get at most 5 tracks
                List<AudioTrack> selectable = list.getTracks().subList(0, Math.min(SearchUtil.MAX_RESULTS, list.getTracks().size()));

                VideoSelection oldSelection = player.selections.get(invoker.getUser().getId());
                if(oldSelection != null) {
                    channel.deleteMessageById(oldSelection.getOutMsgId()).queue();
                }

                MessageBuilder builder = new MessageBuilder();
                builder.append(MessageFormat.format(I18n.get(guild).getString("playSelectVideo"), Config.CONFIG.getPrefix()));

                int i = 1;
                for (AudioTrack track : selectable) {
                    builder.append("\n**")
                            .append(String.valueOf(i))
                            .append(":** ")
                            .append(track.getInfo().title)
                            .append(" (")
                            .append(TextUtils.formatTime(track.getInfo().length))
                            .append(")");

                    i++;
                }

                outMsg.editMessage(builder.build().getRawContent()).queue();

                player.setCurrentTC(channel);

                player.selections.put(invoker.getUser().getId(), new VideoSelection(selectable, outMsg));
            }
        });
    }

    @Override
    public String help(Guild guild) {
        String usage = "{0}{1} <url> OR {0}{1} <search-term>\n#";
        return usage + I18n.get(guild).getString("helpPlayCommand");
    }

    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.USER;
    }
}
