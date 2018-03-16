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
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.PlayerRegistry;
import fredboat.command.info.HelloCommand;
import fredboat.command.info.HelpCommand;
import fredboat.command.info.ShardsCommand;
import fredboat.command.info.StatsCommand;
import fredboat.command.music.control.SkipCommand;
import fredboat.commandmeta.CommandContextParser;
import fredboat.commandmeta.CommandInitializer;
import fredboat.commandmeta.CommandManager;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.config.SentryConfiguration;
import fredboat.config.property.AppConfig;
import fredboat.db.api.GuildConfigService;
import fredboat.db.api.GuildDataService;
import fredboat.db.entity.main.GuildData;
import fredboat.definitions.Module;
import fredboat.definitions.PermissionLevel;
import fredboat.feature.I18n;
import fredboat.feature.metrics.Metrics;
import fredboat.feature.metrics.ShardStatsCounterProvider;
import fredboat.feature.togglz.FeatureFlags;
import fredboat.jda.JdaEntityProvider;
import fredboat.messaging.CentralMessaging;
import fredboat.perms.PermsUtil;
import fredboat.util.DiscordUtil;
import fredboat.util.TextUtils;
import fredboat.util.Tuple2;
import fredboat.util.ratelimit.Ratelimiter;
import io.prometheus.client.Histogram;
import io.prometheus.client.guava.cache.CacheMetricsCollector;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
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
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

@Component
public class EventListenerBoat extends AbstractEventListener {

    private static final Logger log = LoggerFactory.getLogger(EventListenerBoat.class);

    //first string is the users message ID, second string the id of fredboat's message that should be deleted if the
    // user's message is deleted
    public static final Cache<Long, Long> messagesToDeleteIfIdDeleted = CacheBuilder.newBuilder()
            .recordStats()
            .expireAfterWrite(6, TimeUnit.HOURS)
            .build();

    private final CommandManager commandManager;
    private final CommandContextParser commandContextParser;
    private final PlayerRegistry playerRegistry;
    private final ShardStatsCounterProvider shardStatsCounterProvider;
    private final JdaEntityProvider jdaEntityProvider;
    private final Ratelimiter ratelimiter;
    private final AppConfig appConfig;
    private final GuildDataService guildDataService;
    private final GuildConfigService guildConfigService;

    public EventListenerBoat(CommandManager commandManager, CommandContextParser commandContextParser,
                             PlayerRegistry playerRegistry, CacheMetricsCollector cacheMetrics,
                             ShardStatsCounterProvider shardStatsCounterProvider, JdaEntityProvider jdaEntityProvider,
                             Ratelimiter ratelimiter, AppConfig appConfig, GuildDataService guildDataService,
                             GuildConfigService guildConfigService) {
        this.commandManager = commandManager;
        this.commandContextParser = commandContextParser;
        this.playerRegistry = playerRegistry;
        this.shardStatsCounterProvider = shardStatsCounterProvider;
        this.jdaEntityProvider = jdaEntityProvider;
        this.ratelimiter = ratelimiter;
        this.appConfig = appConfig;
        this.guildDataService = guildDataService;
        this.guildConfigService = guildConfigService;
        cacheMetrics.addCache("messagesToDeleteIfIdDeleted", messagesToDeleteIfIdDeleted);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        try (// before execution set some variables that can help with finding traces that belong to each other
             MDC.MDCCloseable _guild = MDC.putCloseable(SentryConfiguration.SENTRY_MDC_TAG_GUILD,
                     event.getGuild() != null ? event.getGuild().getId() : "PRIVATE");
             MDC.MDCCloseable _channel = MDC.putCloseable(SentryConfiguration.SENTRY_MDC_TAG_CHANNEL,
                     event.getChannel().getId());
             MDC.MDCCloseable _invoker = MDC.putCloseable(SentryConfiguration.SENTRY_MDC_TAG_INVOKER,
                     event.getAuthor().getId());
                ) {

            doOnMessageReceived(event);
        }
    }

    private void doOnMessageReceived(MessageReceivedEvent event) {
        if (FeatureFlags.RATE_LIMITER.isActive()) {
            if (ratelimiter.isBlacklisted(event.getAuthor().getIdLong())) {
                Metrics.blacklistedMessagesReceived.inc();
                return;
            }
        }

        if (event.getPrivateChannel() != null) {
            log.info("PRIVATE" + " \t " + event.getAuthor().getName() + " \t " + event.getMessage().getContentRaw());
            return;
        }

        if (event.getAuthor().equals(event.getJDA().getSelfUser())) {
            log.info(event.getMessage().getContentRaw());
            return;
        }

        if (event.getAuthor().isBot()) {
            return;
        }

        TextChannel channel = event.getTextChannel(); //never null since we are filtering private messages out above

        //preliminary permission filter to avoid a ton of parsing
        //let messages pass on to parsing that contain "help" since we want to answer help requests even from channels
        // where we can't talk in
        if (!channel.canTalk() && !event.getMessage().getContentRaw().toLowerCase().contains(CommandInitializer.HELP_COMM_NAME)) {
            return;
        }

        CommandContext context = commandContextParser.parse(event);
        if (context == null) {
            return;
        }
        log.info(event.getMessage().getContentRaw());

        //ignore all commands in channels where we can't write, except for the help command
        if (!channel.canTalk() && !(context.command instanceof HelpCommand)) {
            log.info("Ignoring command {} because this bot cannot write in that channel", context.command.name);
            return;
        }

        Metrics.commandsReceived.labels(context.command.getClass().getSimpleName()).inc();

        //BOT_ADMINs can always use all commands everywhere
        if (!PermsUtil.checkPerms(PermissionLevel.BOT_ADMIN, event.getMember())) {

            //ignore commands of disabled modules for plebs
            Module module = context.command.getModule();
            if (module != null && !context.getEnabledModules().contains(module)) {
                log.debug("Ignoring command {} because its module {} is disabled in guild {}",
                        context.command.name, module.name(), event.getGuild().getIdLong());
                return;
            }
        }

        limitOrExecuteCommand(context);
    }

    /**
     * Check the rate limit of the user and execute the command if everything is fine.
     * @param context Command context of the command to be invoked.
     */
    private void limitOrExecuteCommand(CommandContext context) {
        Tuple2<Boolean, Class> ratelimiterResult = new Tuple2<>(true, null);
        if (FeatureFlags.RATE_LIMITER.isActive()) {
            ratelimiterResult = ratelimiter.isAllowed(context, context.command, 1);
        }

        if (ratelimiterResult.a) {
            Histogram.Timer executionTimer = null;
            if (FeatureFlags.FULL_METRICS.isActive()) {
                executionTimer = Metrics.executionTime.labels(context.command.getClass().getSimpleName()).startTimer();
            }
            try {
                commandManager.prefixCalled(context);
            } finally {
                //NOTE: Some commands, like ;;mal, run async and will not reflect the real performance of FredBoat
                if (FeatureFlags.FULL_METRICS.isActive() && executionTimer != null) {
                    executionTimer.observeDuration();
                }
            }
        } else {
            String out = context.i18n("ratelimitedGeneralInfo");
            if (ratelimiterResult.b == SkipCommand.class) { //we can compare classes with == as long as we are using the same classloader (which we are)
                //add a nice reminder on how to skip more than 1 song
                out += "\n" + context.i18nFormat("ratelimitedSkipCommand",
                        "`" + TextUtils.escapeMarkdown(context.getPrefix()) + CommandInitializer.SKIP_COMM_NAME + " n-m`");
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
            if (ratelimiter.isBlacklisted(event.getAuthor().getIdLong())) {
                //dont need to inc() the metrics counter here, because private message events are a subset of
                // MessageReceivedEvents where we inc() the blacklisted messages counter already
                return;
            }
        }

        //technically not possible anymore to receive private messages from bots but better safe than sorry
        //also ignores our own messages since we're a bot
        if (event.getAuthor().isBot()) {
            return;
        }

        //quick n dirty bot admin / owner check
        if (appConfig.getAdminIds().contains(event.getAuthor().getIdLong())
                || DiscordUtil.getOwnerId(event.getJDA()) == event.getAuthor().getIdLong()) {

            //hack in / hardcode some commands; this is not meant to look clean
            String raw = event.getMessage().getContentRaw().toLowerCase();
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

        GuildPlayer player = playerRegistry.getExisting(guild);

        if (player != null
                && player.isPaused()
                && player.getPlayingTrack() != null
                && joinedChannel.getMembers().contains(guild.getSelfMember())
                && player.getHumanUsersInCurrentVC().size() > 0
                && guildConfigService.fetchGuildConfig(guild).isAutoResume()
                ) {
            player.setPause(false);
            TextChannel activeTextChannel = player.getActiveTextChannel();
            if (activeTextChannel != null) {
                CentralMessaging.sendMessage(activeTextChannel, I18n.get(guild).getString("eventAutoResumed"));
            }
        }
    }

    private void checkForAutoPause(VoiceChannel channelLeft) {
        if (appConfig.getContinuePlayback())
            return;

        Guild guild = channelLeft.getGuild();
        GuildPlayer player = playerRegistry.getExisting(guild);

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
    public void onGuildJoin(GuildJoinEvent event) {
        //wait a few seconds to allow permissions to be set and applied and propagated
        CentralMessaging.restService.schedule(() -> {
            //retrieve the guild again - many things may have happened in 10 seconds!
            Guild g = jdaEntityProvider.getGuildById(event.getGuild().getIdLong());
            if (g != null) {
                sendHelloOnJoin(g);
            }
        }, 10, TimeUnit.SECONDS);
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        playerRegistry.destroyPlayer(event.getGuild());
    }

    @Override
    public void onHttpRequest(HttpRequestEvent event) {
        if (event.getResponse().code >= 300) {
            log.warn("Unsuccessful JDA HTTP Request:\n{}\nResponse:{}\n",
                    event.getRequestRaw(), event.getResponseRaw());
        }
    }

    /* Shard lifecycle */
    @Override
    public void onReady(ReadyEvent event) {
        log.info("Received ready event for {}", event.getJDA().getShardInfo().toString());

        shardStatsCounterProvider.registerShard(event.getJDA().getShardInfo());
    }

    private void sendHelloOnJoin(@Nonnull Guild guild) {
        //filter guilds that already received a hello message
        // useful for when discord trolls us with fake guild joins
        // or to prevent it send repeatedly due to kick and reinvite
        GuildData gd = guildDataService.fetchGuildData(guild);
        if (gd.getTimestampHelloSent() > 0) {
            return;
        }

        TextChannel channel = guild.getTextChannelById(guild.getIdLong()); //old public channel
        if (channel == null || !channel.canTalk()) {
            //find first channel that we can talk in
            for (TextChannel tc : guild.getTextChannels()) {
                if (tc.canTalk()) {
                    channel = tc;
                    break;
                }
            }
        }
        if (channel == null) {
            //no channel found to talk in
            return;
        }

        //send actual hello message and persist on success
        CentralMessaging.sendMessage(channel, HelloCommand.getHello(guild),
                __ -> guildDataService.transformGuildData(guild, GuildData::helloSent));
    }
}
