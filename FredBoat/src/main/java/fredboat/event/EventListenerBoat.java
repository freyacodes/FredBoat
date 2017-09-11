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
package fredboat.event;

import fredboat.Config;
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.PlayerRegistry;
import fredboat.command.fun.TalkCommand;
import fredboat.command.music.control.SkipCommand;
import fredboat.command.util.HelpCommand;
import fredboat.commandmeta.CommandManager;
import fredboat.commandmeta.CommandRegistry;
import fredboat.commandmeta.abs.Command;
import fredboat.db.EntityReader;
import fredboat.feature.I18n;
import fredboat.feature.togglz.FeatureFlags;
import fredboat.util.Tuple2;
import fredboat.util.ratelimit.Ratelimiter;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.events.message.MessageDeleteEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

public class EventListenerBoat extends AbstractEventListener {

    private static final Logger log = LoggerFactory.getLogger(EventListenerBoat.class);

    //first string is the users message ID, second string the id of fredboat's message that should be deleted if the
    // user's message is deleted
    public static Map<String, String> messagesToDeleteIfIdDeleted = new HashMap<>();
    private User lastUserToReceiveHelp;

    public EventListenerBoat() {
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        if (FeatureFlags.RATE_LIMITER.isActive()) {
            if (Ratelimiter.getRatelimiter().isBlacklisted(event.getAuthor().getIdLong())) {
                return;
            }
        }

        if (event.getPrivateChannel() != null) {
            log.info("PRIVATE" + " \t " + event.getAuthor().getName() + " \t " + event.getMessage().getRawContent());
            return;
        }

        if (event.getAuthor().equals(event.getJDA().getSelfUser())) {
            log.info(event.getGuild().getName() + " \t " + event.getAuthor().getName() + " \t " + event.getMessage().getRawContent());
            return;
        }

        if (event.getAuthor().isBot()) {
            return;
        }

        if (event.getMessage().getContent().length() < Config.CONFIG.getPrefix().length()) {
            return;
        }

        if (event.getMessage().getContent().substring(0, Config.CONFIG.getPrefix().length()).equals(Config.CONFIG.getPrefix())) {
            Command invoked = null;
            log.info(event.getGuild().getName() + " \t " + event.getAuthor().getName() + " \t " + event.getMessage().getRawContent());
            Matcher matcher = COMMAND_NAME_PREFIX.matcher(event.getMessage().getContent());

            if (matcher.find()) {
                String cmdName = matcher.group();

                CommandRegistry.CommandEntry entry = CommandRegistry.getCommand(cmdName.toLowerCase());
                if (entry != null) {
                    invoked = entry.command;
                } else {
                    log.info("Unknown command:\t" + cmdName);
                }
            }

            if (invoked == null) {
                return;
            }

            limitOrExecuteCommand(invoked, event);
        } else if (event.getMessage().getMentionedUsers().contains(event.getJDA().getSelfUser())) {
            log.info(event.getGuild().getName() + " \t " + event.getAuthor().getName() + " \t " + event.getMessage().getRawContent());
            CommandManager.commandsExecuted.getAndIncrement();
            //regex101.com/r/9aw6ai/1/
            String message = event.getMessage().getRawContent().replaceAll("<@!?[0-9]*>", "");
            TalkCommand.talk(event.getMember(), event.getTextChannel(), message);
        }
    }

    /**
     * Check the rate limit of the user and execute the command if everything is fine.
     * @param invoked Command to be invoked.
     * @param event Message received from the chat.
     */
    private void limitOrExecuteCommand(Command invoked, MessageReceivedEvent event) {
        Tuple2<Boolean, Class> ratelimiterResult = new Tuple2<>(true, null);
        if (FeatureFlags.RATE_LIMITER.isActive()) {
            ratelimiterResult = Ratelimiter.getRatelimiter().isAllowed(event.getMember(), invoked, 1, event.getTextChannel());

        }
        if (ratelimiterResult.a)
            CommandManager.prefixCalled(invoked, event.getGuild(), event.getTextChannel(), event.getMember(), event.getMessage());
        else {
            String out = event.getMember().getAsMention() + ": " + I18n.get(event.getGuild()).getString("ratelimitedGeneralInfo");
            if (ratelimiterResult.b == SkipCommand.class) { //we can compare classes with == as long as we are using the same classloader (which we are)
                //add a nice reminder on how to skip more than 1 song
                out += "\n" + MessageFormat.format(I18n.get(event.getGuild()).getString("ratelimitedSkipCommand"), "`" + Config.CONFIG.getPrefix() + "skip n-m`");
            }
            event.getTextChannel().sendMessage(out).queue();
        }

    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        if (messagesToDeleteIfIdDeleted.containsKey(event.getMessageId())) {
            String msgId = messagesToDeleteIfIdDeleted.remove(event.getMessageId());
            event.getChannel().deleteMessageById(msgId).queue();
        }
    }

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {

        if (FeatureFlags.RATE_LIMITER.isActive()) {
            if (Ratelimiter.getRatelimiter().isBlacklisted(event.getAuthor().getIdLong())) {
                return;
            }
        }

        if (event.getAuthor() == lastUserToReceiveHelp) {
            //Ignore, they just got help! Stops any bot chain reactions
            return;
        }

        if (event.getAuthor().equals(event.getJDA().getSelfUser())) {
            //Don't reply to ourselves
            return;
        }

        event.getChannel().sendMessage(HelpCommand.getHelpDmMsg(null)).queue();
        lastUserToReceiveHelp = event.getAuthor();
    }

    /* music related */
    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        checkForAutoPause(event.getChannelLeft());
    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        checkForAutoPause(event.getChannelLeft());
        checkForAutoResume(event.getChannelJoined(), event.getMember());

        //were we moved?
        if (event.getMember().getUser().getIdLong() == event.getJDA().getSelfUser().getIdLong()) {
            checkForAutoPause(event.getChannelJoined());
        }
    }

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        checkForAutoResume(event.getChannelJoined(), event.getMember());
    }

    private void checkForAutoResume(VoiceChannel joinedChannel, Member joined) {
        Guild guild = joinedChannel.getGuild();
        //ignore bot users taht arent us joining / moving
        if (joined.getUser().isBot()
                && guild.getSelfMember().getUser().getIdLong() != joined.getUser().getIdLong()) return;

        GuildPlayer player = PlayerRegistry.getExisting(guild);

        if (player != null
                && player.isPaused()
                && player.getPlayingTrack() != null
                && joinedChannel.getMembers().contains(guild.getSelfMember())
                && player.getHumanUsersInCurrentVC().size() > 0
                && EntityReader.getGuildConfig(guild.getId()).isAutoResume()
                ) {
            player.getActiveTextChannel().sendMessage(I18n.get(guild).getString("eventAutoResumed")).queue();
            player.setPause(false);
        }
    }

    private void checkForAutoPause(VoiceChannel channelLeft) {
        Guild guild = channelLeft.getGuild();
        GuildPlayer player = PlayerRegistry.getExisting(guild);

        if (player == null) {
            return;
        }

        //we got kicked from the server while in a voice channel, do nothing and return, because onGuildLeave()
        // should take care of destroying stuff
        if (!guild.isMember(guild.getJDA().getSelfUser())) {
            log.warn("onGuildVoiceLeave called for a guild where we aren't a member. This line should only ever be " +
                    "reached if we are getting kicked from that guild while in a voice channel. Investigate if not.");
            return;
        }

        //are we in the channel that someone left from?
        if (guild.getSelfMember().getVoiceState().inVoiceChannel() &&
                guild.getSelfMember().getVoiceState().getChannel().getIdLong() != channelLeft.getIdLong()) {
            return;
        }

        if (player.getHumanUsersInCurrentVC().isEmpty() && !player.isPaused()) {
            player.pause();
            player.getActiveTextChannel().sendMessage(I18n.get(guild).getString("eventUsersLeftVC")).queue();
        }
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        PlayerRegistry.destroyPlayer(event.getGuild());
    }

}
