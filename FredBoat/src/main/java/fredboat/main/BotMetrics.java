package fredboat.main;

import fredboat.agent.StatsAgent;
import fredboat.util.JDAUtil;
import fredboat.util.rest.Http;
import net.dv8tion.jda.core.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public class BotMetrics {

    private static final Logger log = LoggerFactory.getLogger(BotMetrics.class);
    private static BotMetrics.JdaEntityCounts jdaEntityCountsTotal = new JdaEntityCounts();
    private static BotMetrics.DockerStats dockerStats = new DockerStats();

    @Nonnull
    public static JdaEntityCounts getJdaEntityCountsTotal() {
        return jdaEntityCountsTotal;
    }

    @Nonnull
    public static DockerStats getDockerStats() {
        return dockerStats;
    }

    //JDA total entity counts
    public static int getTotalUniqueUsersCount() {
        return jdaEntityCountsTotal.uniqueUsersCount;
    }

    public static int getTotalGuildsCount() {
        return jdaEntityCountsTotal.guildsCount;
    }

    public static int getTotalTextChannelsCount() {
        return jdaEntityCountsTotal.textChannelsCount;
    }

    public static int getTotalVoiceChannelsCount() {
        return jdaEntityCountsTotal.voiceChannelsCount;
    }

    public static int getTotalCategoriesCount() {
        return jdaEntityCountsTotal.categoriesCount;
    }

    public static int getTotalEmotesCount() {
        return jdaEntityCountsTotal.emotesCount;
    }

    public static int getTotalRolesCount() {
        return jdaEntityCountsTotal.rolesCount;
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
        protected boolean count(Collection<JDA> shards, boolean... force) {
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

        JdaEntityStatsCounter(Runnable action) {
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
                dockerPullsBot = Http.get(BOT_IMAGE_STATS_URL).asJson().getInt("pull_count");
                dockerPullsDb = Http.get(DB_IMAGE_STATS_URL).asJson().getInt("pull_count");
            } catch (IOException e) {
                log.error("Failed to fetch docker stats", e);
            }
        }

    }

    //is 0 while uncalculated
    public static int getDockerPullsBot() {
        return dockerStats.dockerPullsBot;
    }

    //is 0 while uncalculated
    public static int getDockerPullsDb() {
        return dockerStats.dockerPullsDb;
    }
}
