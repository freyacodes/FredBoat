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
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.LavalinkManager;
import fredboat.audio.player.PlayerLimitManager;
import fredboat.audio.player.PlayerRegistry;
import fredboat.audio.player.VideoSelection;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.feature.I18n;
import fredboat.messaging.CentralMessaging;
import fredboat.perms.PermissionLevel;
import fredboat.util.TextUtils;
import fredboat.util.rest.SearchUtil;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message.Attachment;
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
    public void onInvoke(CommandContext context) {
        String[] args = context.args;
        if (!context.invoker.getVoiceState().inVoiceChannel()) {
            context.reply(I18n.get(context, "playerUserNotInChannel"));
            return;
        }

        if (!PlayerLimitManager.checkLimitResponsive(context)) return;

        if (!context.msg.getAttachments().isEmpty()) {
            GuildPlayer player = PlayerRegistry.get(context.guild);
            player.setCurrentTC(context.channel);

            for (Attachment atc : context.msg.getAttachments()) {
                player.queue(atc.getUrl(), context);
            }
            
            player.setPause(false);
            
            return;
        }

        if (args.length < 2) {
            handleNoArguments(context);
            return;
        }

        //What if we want to select a selection instead?
        if (args.length == 2 && StringUtils.isNumeric(args[1])){
            SelectCommand.select(context);
            return;
        }

        //Search youtube for videos and let the user select a video
        if (!args[1].startsWith("http")) {
            searchForVideos(context);
            return;
        }

        GuildPlayer player = PlayerRegistry.get(context.guild);
        player.setCurrentTC(context.channel);

        player.queue(args[1], context);
        player.setPause(false);

        context.deleteMessage();
    }

    private void handleNoArguments(CommandContext context) {
        Guild guild = context.guild;
        GuildPlayer player = PlayerRegistry.get(guild);
        player.setCurrentTC(context.channel);
        if (player.isQueueEmpty()) {
            context.reply(I18n.get(context, "playQueueEmpty"));
        } else if (player.isPlaying()) {
            context.reply(I18n.get(context, "playAlreadyPlaying"));
        } else if (player.getHumanUsersInCurrentVC().isEmpty() && LavalinkManager.ins.getConnectedChannel(guild) != null) {
            context.reply(I18n.get(context, "playVCEmpty"));
        } else if(LavalinkManager.ins.getConnectedChannel(guild) == null) {
            // When we just want to continue playing, but the user is not in a VC
            JOIN_COMMAND.onInvoke(context);
            if(LavalinkManager.ins.getConnectedChannel(guild) != null || guild.getAudioManager().isAttemptingToConnect()) {
                player.play();
                context.reply(I18n.get(context, "playWillNowPlay"));
            }
        } else {
            player.play();
            context.reply(I18n.get(context, "playWillNowPlay"));
        }
    }

    private void searchForVideos(CommandContext context) {
        Matcher m = Pattern.compile("\\S+\\s+(.*)").matcher(context.msg.getRawContent());
        m.find();
        String query = m.group(1);
        
        //Now remove all punctuation
        query = query.replaceAll(SearchUtil.PUNCTUATION_REGEX, "");

        String finalQuery = query;
        context.reply(I18n.get(context, "playSearching").replace("{q}", query), outMsg -> {
            AudioPlaylist list;
            try {
                list = SearchUtil.searchForTracks(finalQuery, searchProviders);
            } catch (SearchUtil.SearchingException e) {
                context.reply(I18n.get(context, "playYoutubeSearchError"));
                log.error("YouTube search exception", e);
                return;
            }

            if (list == null || list.getTracks().isEmpty()) {
                CentralMessaging.editMessage(outMsg,
                        I18n.get(context, "playSearchNoResults").replace("{q}", finalQuery)
                );

            } else {
                //Clean up any last search by this user
                GuildPlayer player = PlayerRegistry.get(context.guild);

                //Get at most 5 tracks
                List<AudioTrack> selectable = list.getTracks().subList(0, Math.min(SearchUtil.MAX_RESULTS, list.getTracks().size()));

                VideoSelection oldSelection = player.selections.get(context.invoker.getUser().getId());
                if(oldSelection != null) {
                    CentralMessaging.deleteMessageById(context.channel, oldSelection.getOutMsgId());
                }

                MessageBuilder builder = CentralMessaging.getClearThreadLocalMessageBuilder();
                builder.append(MessageFormat.format(I18n.get(context, "playSelectVideo"), Config.CONFIG.getPrefix()));

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

                CentralMessaging.editMessage(outMsg, builder.build());
                player.setCurrentTC(context.channel);
                player.selections.put(context.invoker.getUser().getId(), new VideoSelection(selectable, outMsg));
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
