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

package fredboat.feature.metrics.collectors

import fredboat.audio.lavalink.SentinelLavalink
import fredboat.db.mongo.ActivityMetricsController
import fredboat.feature.metrics.BotMetrics
import fredboat.sentinel.GuildCache
import io.prometheus.client.Collector
import io.prometheus.client.CounterMetricFamily
import io.prometheus.client.GaugeMetricFamily
import lavalink.client.io.Link
import org.springframework.stereotype.Component
import java.util.*

/**
 * Created by napster on 19.10.17.
 *
 * Collects various FredBoat stats for prometheus
 */
@Component
class FredBoatCollector(
        private val botMetrics: BotMetrics,
        private val ll: SentinelLavalink,
        private val guildCache: GuildCache,
        private val activityMetricsController: ActivityMetricsController
) : Collector() {

    private var lastEntityCountHash = 0

    override fun collect(): List<Collector.MetricFamilySamples> {
        val mfs = ArrayList<Collector.MetricFamilySamples>()
        //NOTE: shard specific metrics have been disabled, because we were not really using them, but they take up a ton
        // of samples (Example: 800 shards x 7 metrics = 5600 unused samples). the label has been kept to not break
        // existing graphs. Use "total" for it when adding botwide metrics.
        val labelNames = Arrays.asList("shard", "entity")

        val jdaEntities = GaugeMetricFamily("fredboat_jda_entities",
                "Amount of JDA entities", labelNames)
        mfs.add(jdaEntities)

        val playersPlaying = GaugeMetricFamily("fredboat_playing_music_players",
                "Currently playing music players", labelNames)
        mfs.add(playersPlaying)

        val dockerPulls = CounterMetricFamily("fredboat_docker_pulls",
                "Total fredboat docker image pulls as reported by the docker hub.", labelNames)
        mfs.add(dockerPulls)

        val linkStateGauge = GaugeMetricFamily("fredboat_lavalink_link_states",
                "Number of Lavalink Links in different states", listOf("state"))
        mfs.add(linkStateGauge)

        val guildCacheSize = GaugeMetricFamily("fredboat_guild_cache_size",
                "Number of subscribed guilds", listOf("total"))
        mfs.add(guildCacheSize)

        val listenerActivity = GaugeMetricFamily("fredboat_active_listeners",
                "Users on a daily, weekly, or monthly basis", listOf("duration"))
        mfs.add(listenerActivity)

        //global jda entity stats
        if (botMetrics.entityCounts != null && botMetrics.entityCounts?.hashCode() != lastEntityCountHash) {
            val countsPair = botMetrics.entityCounts!!
            val counts = countsPair.t1
            jdaEntities.addMetric(listOf("total", "User"), countsPair.t2.toDouble())
            jdaEntities.addMetric(listOf("total", "Guild"), counts.guilds.toDouble())
            jdaEntities.addMetric(listOf("total", "TextChannel"), counts.textChannels.toDouble())
            jdaEntities.addMetric(listOf("total", "VoiceChannel"), counts.voiceChannels.toDouble())
            jdaEntities.addMetric(listOf("total", "Category"), counts.categories.toDouble())
            jdaEntities.addMetric(listOf("total", "Emote"), counts.emotes.toDouble())
            jdaEntities.addMetric(listOf("total", "Role"), counts.roles.toDouble())
            lastEntityCountHash = countsPair.hashCode()
        }

        //music player stats
        val musicPlayerStats = botMetrics.musicPlayerStats
        if (musicPlayerStats.isCounted) {
            playersPlaying.addMetric(Arrays.asList("total", "Players"), musicPlayerStats.playing.toDouble()) //entity could be better named "PlayingPlayers", but dont break existing graphs...besides, players will hopefully one day be stateless entities in the database instead of paused objects in the JVM.
            playersPlaying.addMetric(Arrays.asList("total", "TotalPlayers"), musicPlayerStats.total.toDouble())
        }

        //docker stats
        val dockerStats = botMetrics.dockerStats
        if (dockerStats.isFetched) {
            dockerPulls.addMetric(Arrays.asList("total", "Bot"), dockerStats.dockerPullsBot.toDouble())
            dockerPulls.addMetric(Arrays.asList("total", "Db"), dockerStats.dockerPullsDb.toDouble())
        }

        val statesMap = Link.State.values().associate { it to 0 }.toMutableMap()
        ll.links.forEach { statesMap[it.state] = statesMap[it.state]!! + 1 }
        statesMap.forEach { state, count ->
            linkStateGauge.addMetric(listOf(state.name), count.toDouble())
        }

        guildCacheSize.addMetric(listOf("total"), guildCache.cache.size.toDouble())

        activityMetricsController.acquireStats()?.apply {
            listenerActivity.addMetric(listOf("daily"), dau)
            listenerActivity.addMetric(listOf("weekly"), wau)
            listenerActivity.addMetric(listOf("monthly"), mau)
        }

        return mfs
    }
}
