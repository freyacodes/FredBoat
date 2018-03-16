/*
 * MIT License
 *
 * Copyright (c) 2017-2018 Frederik Ar. Mikkelsen
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

import fredboat.agent.StatsAgent;
import fredboat.config.property.Credentials;
import fredboat.jda.ShardProvider;
import fredboat.main.BotController;
import fredboat.util.DiscordUtil;
import fredboat.util.JDAUtil;
import net.dv8tion.jda.core.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Metrics for the whole FredBoat
 */
@Component
public class BotMetrics {

    private static final Logger log = LoggerFactory.getLogger(BotMetrics.class);
    private final StatsAgent statsAgent;
    private BotMetrics.JdaEntityCounts jdaEntityCountsTotal = new JdaEntityCounts();
    private BotMetrics.DockerStats dockerStats = new DockerStats();


    public BotMetrics(StatsAgent statsAgent) {
        this.statsAgent = statsAgent;
    }

    @Nonnull
    public JdaEntityCounts getJdaEntityCountsTotal() {
        return jdaEntityCountsTotal;
    }

    @Nonnull
    public DockerStats getDockerStats() {
        return dockerStats;
    }

    //JDA total entity counts
    public int getTotalUniqueUsersCount() {
        return jdaEntityCountsTotal.uniqueUsersCount;
    }

    public int getTotalGuildsCount() {
        return jdaEntityCountsTotal.guildsCount;
    }

    public int getTotalTextChannelsCount() {
        return jdaEntityCountsTotal.textChannelsCount;
    }

    public int getTotalVoiceChannelsCount() {
        return jdaEntityCountsTotal.voiceChannelsCount;
    }

    public int getTotalCategoriesCount() {
        return jdaEntityCountsTotal.categoriesCount;
    }

    public int getTotalEmotesCount() {
        return jdaEntityCountsTotal.emotesCount;
    }

    public int getTotalRolesCount() {
        return jdaEntityCountsTotal.rolesCount;
    }

    //call this once, after shards are all up
    public void start(ShardProvider shardProvider, Credentials credentials) {
        BotMetrics.JdaEntityCounts jdaEntityCountsTotal = getJdaEntityCountsTotal();
        Supplier<Collection<JDA>> shardsSupplier = () -> shardProvider.streamShards().collect(Collectors.toList());
        try {
            jdaEntityCountsTotal.count(shardsSupplier);
        } catch (Exception ignored) {
        }

        statsAgent.addAction(new BotMetrics.JdaEntityStatsCounter(
                () -> jdaEntityCountsTotal.count(shardsSupplier)));

        if (DiscordUtil.isOfficialBot(credentials)) {
            BotMetrics.DockerStats dockerStats = getDockerStats();
            try {
                dockerStats.fetch();
            } catch (Exception ignored) {
            }
            statsAgent.addAction(dockerStats::fetch);
        }
    }

    //holds counts of JDA entities
    //this is a central place for stats agents to make calls to
    //stats agents are preferred to triggering counts by JDA events, since we cannot predict JDA events
    //the resulting lower resolution of datapoints is fine, we don't need a high data resolution for these anyways
    protected static class JdaEntityCounts {

        protected int uniqueUsersCount;
        protected int guildsCount;
        protected int textChannelsCount;
        protected int voiceChannelsCount;
        protected int categoriesCount;
        protected int emotesCount;
        protected int rolesCount;

        private final AtomicInteger expectedUniqueUserCount = new AtomicInteger(-1);

        //counts things
        // also checks shards for readiness and only counts if all of them are ready
        // the force is an option for when we want to do a count when receiving the onReady event, but JDAs status is
        // not CONNECTED at that point
        protected boolean count(Supplier<Collection<JDA>> shardSupplier, boolean... force) {
            Collection<JDA> shards = shardSupplier.get();
            for (JDA shard : shards) {
                if ((shard.getStatus() != JDA.Status.CONNECTED) && (force.length < 1 || !force[0])) {
                    log.info("Skipping counts since not all requested shards are ready.");
                    return false;
                }
            }

            this.uniqueUsersCount = JDAUtil.countUniqueUsers(shards, expectedUniqueUserCount);
            //never shrink the expected user count (might happen due to unready/reloading shards)
            this.expectedUniqueUserCount.accumulateAndGet(uniqueUsersCount, Math::max);

            this.guildsCount = JDAUtil.countGuilds(shards);
            this.textChannelsCount = JDAUtil.countTextChannels(shards);
            this.voiceChannelsCount = JDAUtil.countVoiceChannels(shards);
            this.categoriesCount = JDAUtil.countCategories(shards);
            this.emotesCount = JDAUtil.countEmotes(shards);
            this.rolesCount = JDAUtil.countRoles(shards);

            return true;
        }
    }

    protected static class JdaEntityStatsCounter implements StatsAgent.Action {
        private final Runnable action;

        protected JdaEntityStatsCounter(Runnable action) {
            this.action = action;
        }

        @Override
        public String getName() {
            return "jda entity stats for fredboat";
        }

        @Override
        public void act() {
            action.run();
        }
    }


    protected static class DockerStats {
        private static final String BOT_IMAGE_STATS_URL = "https://hub.docker.com/v2/repositories/fredboat/fredboat/";
        private static final String DB_IMAGE_STATS_URL = "https://hub.docker.com/v2/repositories/fredboat/postgres/";

        protected int dockerPullsBot;
        protected int dockerPullsDb;

        protected void fetch() {
            try {
                dockerPullsBot = BotController.HTTP.get(BOT_IMAGE_STATS_URL).asJson().getInt("pull_count");
                dockerPullsDb = BotController.HTTP.get(DB_IMAGE_STATS_URL).asJson().getInt("pull_count");
            } catch (IOException e) {
                log.error("Failed to fetch docker stats", e);
            }
        }

    }

    //is 0 while uncalculated
    public int getDockerPullsBot() {
        return dockerStats.dockerPullsBot;
    }

    //is 0 while uncalculated
    public int getDockerPullsDb() {
        return dockerStats.dockerPullsDb;
    }
}
