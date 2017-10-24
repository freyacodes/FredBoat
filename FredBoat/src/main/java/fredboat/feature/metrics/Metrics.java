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
import com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory;
import fredboat.FredBoat;
import fredboat.agent.FredBoatAgent;
import fredboat.audio.player.VideoSelection;
import fredboat.feature.metrics.collectors.FredBoatCollector;
import fredboat.feature.metrics.collectors.ThreadPoolCollector;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.guava.cache.CacheMetricsCollector;
import io.prometheus.client.hibernate.HibernateStatisticsCollector;
import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.client.logback.InstrumentedAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by napster on 08.09.17.
 * <p>
 * This is a central place for all Counters and Gauges and whatever else we are using so that the available stats can be
 * seen at one glance.
 */
public class Metrics {
    private static final Logger log = LoggerFactory.getLogger(Metrics.class);

    //call this once at the start of the bot to set up things
    // further calls won't have any effect
    public static void setup() {
        log.info("Metrics set up {}", instance().toString());
    }

    //holder pattern
    public static Metrics instance() {
        return MetricHolder.INSTANCE;
    }

    private static class MetricHolder {
        private static final Metrics INSTANCE = new Metrics();
    }

    //call register on the hibernate stats after all connections are set up
    public final HibernateStatisticsCollector hibernateStats;
    public final PrometheusMetricsTrackerFactory hikariStats;

    public final SparkMetricsServlet metricsServlet = new SparkMetricsServlet();
    public final CacheMetricsCollector cacheMetrics = new CacheMetricsCollector().register();
    // collect jda events metrics
    public final JdaEventsMetricsListener jdaEventsMetricsListener = new JdaEventsMetricsListener();

    //our custom collectors / listeners etc:
    // fredboat stuff
    public final FredBoatCollector fredBoatCollector = new FredBoatCollector().register();
    // threadpools
    public final ThreadPoolCollector threadPoolCollector = new ThreadPoolCollector().register();

    private Metrics() {
        //log metrics
        final LoggerContext factory = (LoggerContext) LoggerFactory.getILoggerFactory();
        final ch.qos.logback.classic.Logger root = factory.getLogger(Logger.ROOT_LOGGER_NAME);
        final InstrumentedAppender prometheusAppender = new InstrumentedAppender();
        prometheusAppender.setContext(root.getLoggerContext());
        prometheusAppender.start();
        root.addAppender(prometheusAppender);

        //jvm (hotspot) metrics
        DefaultExports.initialize();

        hibernateStats = new HibernateStatisticsCollector();
        hikariStats = new PrometheusMetricsTrackerFactory();

        cacheMetrics.addCache("videoSelections", VideoSelection.SELECTIONS);

        //register some of our "important" thread pools
        threadPoolCollector.addPool("main-executor", (ThreadPoolExecutor) FredBoat.executor);
        threadPoolCollector.addPool("agents-scheduler", (ThreadPoolExecutor) FredBoatAgent.getScheduler());
    }


    //todo check naming conventions
    //todo check for proper label usage
    //todo check for inc() usage
    //todo check FULL_METRICS feature flag
    //todo any missing counters n gauges n stuff

    // ################################################################################
    // ##                              JDA Stats
    // ################################################################################

    public static final Counter totalJdaEvents = Counter.build()
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

    //messages

    public static final Counter totalMessagesWithPrefixReceived = Counter.build()
            .name("fredboat_messages_received_prefix_total")
            .help("Total received messages with our prefix")
            .register();


    //agent stuff

    public static final Counter voiceChannelsCleanedUp = Counter.build()
            .name("fredboat_music_voicechannels_cleanedup_total")
            .help("Total voice channels that were cleaned up")
            .register();


    //ratelimiter & blacklist

    public static final Counter autoBlacklistsIssued = Counter.build()
            .name("fredboat_commands_autoblacklists_issued_total")
            .help("How many user were blacklisted on a particular level")
            .labelNames("level") //blacklist level
            .register();

    public static final Counter totalBlacklistedMessagesReceived = Counter.build()
            .name("fredboat_messages_received_blacklisted_total")
            .help("Total messages by users that are blacklisted")
            .register();

    public static final Counter totalCommandsRatelimited = Counter.build()
            .name("fredboat_commands_ratelimited_total")
            .help("Total ratelimited commands")
            .labelNames("class") // use the simple name of the command class
            .register();


    //music stuff

    public static final Counter totalSearchRequests = Counter.build()//search requests issued by users
            .name("fredboat_music_search_requests_total")
            .help("Total search requests")
            .register();

    public static final Counter searchHits = Counter.build()//actual sources of the returned results
            .name("fredboat_music_searches_hits_total")
            .labelNames("source")
            .help("Total search hits")
            .register();


    //commands

    public static final Counter prefixUsed = Counter.build()
            .name("fredboat_prefix_used_total")
            .help("Total times a prefix was used")
            .labelNames("prefix") // ;;, @mention (soon:tm:), possibly custom in the future?
            .register();

    public static final Counter totalCommandsReceived = Counter.build()
            .name("fredboat_commands_received_total")
            .help("Total received commands")
            .labelNames("class") // use the simple name of the command class: PlayCommand, DanceCommand, ShardsCommand etc
            .register();

    public static final Counter totalCommandsExecuted = Counter.build()
            .name("fredboat_commands_executed_total")
            .help("Total executed commands")
            .register();

    public static final Counter commandsExecuted = Counter.build()
            .name("fredboat_commands_executed_by_class_total")
            .help("Total executed commands by class")
            .labelNames("class") // use the simple name of the command class
            .register();

    public static final Histogram processingTime = Histogram.build()//includes commands that get ratelimited
            .name("fredboat_command_processing_duration_seconds")
            .help("Command processing time")
            .labelNames("class") // use the simple name of the command class: PlayCommand, DanceCommand, ShardsCommand etc
            .register();

    public static final Histogram executionTime = Histogram.build()//actual commands execution
            .name("fredboat_command_execution_duration_seconds")
            .help("Command execution time")
            .labelNames("class") // use the simple name of the command class: PlayCommand, DanceCommand, ShardsCommand etc
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

    public static final Counter databaseExceptionsCaught = Counter.build()//todo use this
            .name("fredboat_db_exceptions_caught_total")
            .help("Total database exceptions caught")
            .register();

    public static final Gauge threadPoolActiveThreads = Gauge.build()//todo use this
            .name("fredboat_threadpool_active_threads_current")
            .help("Current active threads on various thread pools / executors")
            .labelNames("name") //identifier for the threadpool
            .register();
}
