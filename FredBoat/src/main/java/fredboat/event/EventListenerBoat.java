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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fredboat.Config;
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.PlayerRegistry;
import fredboat.command.maintenance.ShardsCommand;
import fredboat.command.maintenance.StatsCommand;
import fredboat.command.music.control.SkipCommand;
import fredboat.command.util.HelpCommand;
import fredboat.commandmeta.CommandManager;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.db.EntityReader;
import fredboat.feature.I18n;
import fredboat.feature.togglz.FeatureFlags;
import fredboat.messaging.CentralMessaging;
import fredboat.util.DiscordUtil;
import fredboat.util.Tuple2;
import fredboat.util.ratelimit.Ratelimiter;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.events.http.HttpRequestEvent;
import net.dv8tion.jda.core.events.message.MessageDeleteEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class EventListenerBoat extends AbstractEventListener {

    private static final Logger log = LoggerFactory.getLogger(EventListenerBoat.class);

    //first string is the users message ID, second string the id of fredboat's message that should be deleted if the
    // user's message is deleted
    public static final Cache<Long, Long> messagesToDeleteIfIdDeleted = CacheBuilder.newBuilder()
            .expireAfterWrite(6, TimeUnit.HOURS)
            .build();


    // ***********************************************************************************
    //                      Metrics objects (yes there are a ton)
    // ***********************************************************************************

    private static final Counter totalMessagesReceived = Counter.build()
            .name("fredboat_messages_received_total")
            .help("Total messages received")
            .register();
    private static final Counter totalBlacklistedMessagesReceived = Counter.build()
            .name("fredboat_messages_received_blacklisted_total")
            .help("Total messages by users that are blacklisted")
            .register();
    private static final Counter totalPrivateMessagesReceived = Counter.build()
            .name("fredboat_messages_received_private_total")
            .help("Total private messages received")
            .register();

    private static final Counter totalMessagesWithPrefixReceived = Counter.build()
            .name("fredboat_messages_received_prefix_total")
            .help("Total received messages with our prefix")
            .register();

    private static final Counter totalCommandsReceived = Counter.build()
            .name("fredboat_commands_received_total")
            .help("Total received commands")
            .labelNames("class") // use the simple name of the command class
            .register();

    //includes commands that get ratelimited
    private static final Histogram processingTime = Histogram.build()
            .name("fredboat_command_processing_duration_seconds")
            .help("Command processing time")
            .labelNames("class") // use the simple name of the command class
            .register();

    //actual commands execution
    private static final Histogram executionTime = Histogram.build()
            .name("fredboat_command_execution_duration_seconds")
            .help("Command execution time")
            .labelNames("class") // use the simple name of the command class
            .register();



    public EventListenerBoat() {
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        totalMessagesReceived.inc();

        if (FeatureFlags.RATE_LIMITER.isActive()) {
            if (Ratelimiter.getRatelimiter().isBlacklisted(event.getAuthor().getIdLong())) {
                totalBlacklistedMessagesReceived.inc();
                return;
            }
        }

        if (event.getPrivateChannel() != null) {
            log.info("PRIVATE" + " \t " + event.getAuthor().getName() + " \t " + event.getMessage().getRawContent());
            totalPrivateMessagesReceived.inc();
            return;
        }

        if (event.getAuthor().equals(event.getJDA().getSelfUser())) {
            log.info(event.getGuild().getName() + " \t " + event.getAuthor().getName() + " \t " + event.getMessage().getRawContent());
            return;
        }

        if (event.getAuthor().isBot()) {
            return;
        }

        String content = event.getMessage().getContent();
        if (content.length() <= Config.CONFIG.getPrefix().length()) {
            return;
        }

        if (content.startsWith(Config.CONFIG.getPrefix())) {
            log.info(event.getGuild().getName() + " \t " + event.getAuthor().getName() + " \t " + event.getMessage().getRawContent());
            totalMessagesWithPrefixReceived.inc();

            CommandContext context = CommandContext.parse(event);

            if (context == null) {
                return;
            }

            //ignore all commands in channels where we can't write, except for the help command
            if (!context.hasPermissions(Permission.MESSAGE_WRITE) && !(context.command instanceof HelpCommand)) {
                log.debug("Ignored command because this bot cannot write in that channel");
                return;
            }

            String commandClassName = context.command.getClass().getSimpleName();
            Histogram.Timer processingTimer = null;
            if (FeatureFlags.FULL_METRICS.isActive()) {
                totalCommandsReceived.labels(commandClassName).inc();
                processingTimer = processingTime.labels(commandClassName).startTimer();
            }
            try {
                limitOrExecuteCommand(context);
            } finally {
                //NOTE: Some commands, like ;;mal, run async and will not reflect the real performance of FredBoat
                if (FeatureFlags.FULL_METRICS.isActive() && processingTimer != null) {
                    processingTimer.observeDuration();
                }
            }
        }
    }

    /**
     * Check the rate limit of the user and execute the command if everything is fine.
     * @param context Command context of the command to be invoked.
     */
    private void limitOrExecuteCommand(CommandContext context) {
        Tuple2<Boolean, Class> ratelimiterResult = new Tuple2<>(true, null);
        if (FeatureFlags.RATE_LIMITER.isActive()) {
            ratelimiterResult = Ratelimiter.getRatelimiter().isAllowed(context, context.command, 1);

        }
        if (ratelimiterResult.a) {
            Histogram.Timer executionTimer = null;
            if (FeatureFlags.FULL_METRICS.isActive()) {
                executionTimer = executionTime.labels(context.command.getClass().getSimpleName()).startTimer();
            }
            try {
                CommandManager.prefixCalled(context);
            } finally {
                if (FeatureFlags.FULL_METRICS.isActive() && executionTimer != null) {
                    executionTimer.observeDuration();
                }
            }
        } else {
            String out = context.i18n("ratelimitedGeneralInfo");
            if (ratelimiterResult.b == SkipCommand.class) { //we can compare classes with == as long as we are using the same classloader (which we are)
                //add a nice reminder on how to skip more than 1 song
                out += "\n" + context.i18nFormat("ratelimitedSkipCommand",
                        "`" + Config.CONFIG.getPrefix() + "skip n-m`");
            }
            context.replyWithMention(out);
        }
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        Long toDelete = messagesToDeleteIfIdDeleted.getIfPresent(event.getMessageIdLong());
        if (toDelete != null) {
            messagesToDeleteIfIdDeleted.invalidate(toDelete);
            CentralMessaging.deleteMessageById(event.getChannel(), toDelete);
        }
    }

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {

        if (FeatureFlags.RATE_LIMITER.isActive()) {
            if (Ratelimiter.getRatelimiter().isBlacklisted(event.getAuthor().getIdLong())) {
                return;
            }
        }

        //technically not possible anymore to receive private messages from bots but better safe than sorry
        //also ignores our own messages since we're a bot
        if (event.getAuthor().isBot()) {
            return;
        }

        //quick n dirty bot admin / owner check
        if (Config.CONFIG.getAdminIds().contains(event.getAuthor().getId())
                || DiscordUtil.getApplicationInfo(event.getJDA()).getOwner().getId().equals(event.getAuthor().getId())) {

            //hack in / hardcode some commands; this is not meant to look clean
            String raw = event.getMessage().getRawContent().toLowerCase();
            if (raw.contains("shard")) {
                for (Message message : ShardsCommand.getShardStatus(event.getMessage())) {
                    CentralMessaging.sendMessage(event.getChannel(), message);
                }
                return;
            } else if (raw.contains("stats")) {
                CentralMessaging.sendMessage(event.getChannel(), StatsCommand.getStats(null, event.getJDA()));
                return;
            }
        }

        HelpCommand.sendGeneralHelp(event);
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
        //ignore bot users that arent us joining / moving
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
            player.setPause(false);
            TextChannel activeTextChannel = player.getActiveTextChannel();
            if (activeTextChannel != null) {
                CentralMessaging.sendMessage(activeTextChannel, I18n.get(guild).getString("eventAutoResumed"));
            }
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
        VoiceChannel currentVc = player.getCurrentVoiceChannel();
        if (currentVc != null && currentVc.getIdLong() != channelLeft.getIdLong()) {
            return;
        }

        if (player.getHumanUsersInVC(currentVc).isEmpty() && !player.isPaused()) {
            player.pause();
            TextChannel activeTextChannel = player.getActiveTextChannel();
            if (activeTextChannel != null) {
                CentralMessaging.sendMessage(activeTextChannel, I18n.get(guild).getString("eventUsersLeftVC"));
            }
        }
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        PlayerRegistry.destroyPlayer(event.getGuild());
    }

    @Override
    public void onHttpRequest(HttpRequestEvent event) {
        if (event.getResponse().code >= 300) {
            log.warn("Unsuccessful JDA HTTP Request:\n{}\nResponse:{}\n",
                    event.getRequestRaw(), event.getResponseRaw());
        }
    }
}
