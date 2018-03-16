/*
 *
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
 */

package fredboat.feature.metrics;

import ch.qos.logback.classic.LoggerContext;
import fredboat.agent.FredBoatAgent;
import fredboat.command.info.HelpCommand;
import fredboat.feature.metrics.collectors.FredBoatCollector;
import fredboat.feature.metrics.collectors.ThreadPoolCollector;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import io.prometheus.client.guava.cache.CacheMetricsCollector;
import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.client.logback.InstrumentedAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by napster on 08.09.17.
 * <p>
 * This is a central place for all Counters and Gauges and whatever else we are using so that the available stats can be
 * seen at one glance.
 */
@Component
public class Metrics {
    private static final Logger log = LoggerFactory.getLogger(Metrics.class);

    public Metrics(CacheMetricsCollector cacheMetrics, InstrumentedAppender prometheusAppender,
                   FredBoatCollector fredBoatCollector, ThreadPoolCollector threadPoolCollector) {
        log.info("Setting up metrics");

        //log metrics
        final LoggerContext factory = (LoggerContext) LoggerFactory.getILoggerFactory();
        final ch.qos.logback.classic.Logger root = factory.getLogger(Logger.ROOT_LOGGER_NAME);
        prometheusAppender.setContext(root.getLoggerContext());
        prometheusAppender.start();
        root.addAppender(prometheusAppender);

        //jvm (hotspot) metrics
        DefaultExports.initialize();

        //add some of our guava caches that are currently only statically reachable
        cacheMetrics.addCache("HELP_RECEIVED_RECENTLY", HelpCommand.HELP_RECEIVED_RECENTLY);

        try {
            fredBoatCollector.register();
            threadPoolCollector.register();
        } catch (IllegalArgumentException e) {
            log.error("This should not happen outside of tests.", e);
        }

        //register some of our "important" thread pools
        threadPoolCollector.addPool("agents-scheduler", (ThreadPoolExecutor) FredBoatAgent.getScheduler());

        log.info("Metrics set up");
    }


    // ################################################################################
    // ##                              JDA Stats
    // ################################################################################

    public static final Counter jdaEvents = Counter.build()
            .name("fredboat_jda_events_received_total")
            .help("All events that JDA provides us with by class")
            .labelNames("class") //GuildJoinedEvent, MessageReceivedEvent, ReconnectEvent etc
            .register();

    public static final Counter successfulRestActions = Counter.build()
            .name("fredboat_jda_restactions_successful_total")
            .help("Total successful JDA restactions sent by FredBoat")
            .labelNames("restaction") // sendMessage, deleteMessage, sendTyping etc
            .register();

    public static final Counter failedRestActions = Counter.build()
            .name("fredboat_jda_restactions_failed_total")
            .help("Total failed JDA restactions sent by FredBoat")
            .labelNames("error_response_code") //Use the error response codes like: 50013, 10008 etc
            .register();


    // ################################################################################
    // ##                        FredBoat Stats
    // ################################################################################

    //ratelimiter & blacklist

    public static final Counter autoBlacklistsIssued = Counter.build()
            .name("fredboat_autoblacklists_issued_total")
            .help("How many users were blacklisted on a particular level")
            .labelNames("level") //blacklist level
            .register();

    public static final Counter blacklistedMessagesReceived = Counter.build()
            .name("fredboat_messages_received_by_blacklisted_users_total")
            .help("Total messages received by users that are blacklisted. Might include bots.")
            .register();

    public static final Counter commandsRatelimited = Counter.build()
            .name("fredboat_commands_ratelimited_total")
            .help("Total ratelimited commands")
            .labelNames("class") // use the simple name of the command class
            .register();


    //music stuff

    public static final Counter searchRequests = Counter.build()//search requests issued by users
            .name("fredboat_music_search_requests_total")
            .help("Total search requests")
            .register();

    public static final Counter searchHits = Counter.build()//actual sources of the returned results
            .name("fredboat_music_search_hits_total")
            .help("Total search hits")
            .labelNames("source") //cache, youtube, soundcloud etc
            .register();

    public static final Counter tracksLoaded = Counter.build()
            .name("fredboat_music_tracks_loaded_total")
            .help("Total tracks loaded by the audio loader")
            .register();

    public static final Counter trackLoadsFailed = Counter.build()
            .name("fredboat_music_track_loads_failed_total")
            .help("Total failed track loads by the audio loader")
            .register();

    public static final Counter voiceChannelsCleanedUp = Counter.build()
            .name("fredboat_music_voicechannels_cleanedup_total")
            .help("Total voice channels that were cleaned up by the voice channel agent")
            .register();


    //commands

    public static final Counter prefixParsed = Counter.build()
            .name("fredboat_prefix_parsed_total")
            .help("Total times a prefix was parsed.")
            .labelNames("type") // default, mention, custom
            .register();

    public static final Counter commandsReceived = Counter.build()
            .name("fredboat_commands_received_total")
            .help("Total received commands. Some of these might get ratelimited.")
            .labelNames("class") // use the simple name of the command class: PlayCommand, DanceCommand, ShardsCommand etc
            .register();

    public static final Counter commandsExecuted = Counter.build()
            .name("fredboat_commands_executed_total")
            .help("Total executed commands by class")
            .labelNames("class") // use the simple name of the command class: PlayCommand, DanceCommand, ShardsCommand etc
            .register();

    public static final Histogram executionTime = Histogram.build()//commands execution time, excluding ratelimited ones
            .name("fredboat_command_execution_duration_seconds")
            .help("Command execution time, excluding handling ratelimited commands.")
            .labelNames("class") // use the simple name of the command class: PlayCommand, DanceCommand, ShardsCommand etc
            .register();

    public static final Counter handledExceptions = Counter.build()
            .name("fredboat_handled_exceptions_total")
            .help("Total uncaught exceptions bubbled up by command invocation or other moving parts")
            .labelNames("class") //class of the exception, messaging exceptions should be summed up into one, as they have their own stat, see below
            .register();

    public static final Counter messagingExceptions = Counter.build()
            .name("fredboat_messaging_exceptions_total")
            .help("Total messaging exceptions bubbled by command invocation or other moving parts")
            .labelNames("class") //subclass of the messaging exception
            .register();

    // ################################################################################
    // ##                           Http stats
    // ################################################################################

    //outgoing
    public static Counter httpEventCounter = Counter.build()
            .name("fredboat_okhttp_events_total")
            .help("Total okhttp events")
            .labelNames("okhttp_instance", "event") //see OkHttpEventMetrics for details
            .register();

    //incoming
    public static final Counter apiServed = Counter.build()
            .name("fredboat_api_served_total")
            .help("Total api calls served")
            .labelNames("path") // like /stats, /metrics, etc
            .register();


    // ################################################################################
    // ##                           Various
    // ################################################################################

    public static final Counter databaseExceptionsCreated = Counter.build()
            .name("fredboat_db_exceptions_created_total")
            .help("Total database exceptions created")
            .register();

}
