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

package fredboat.feature.metrics.collectors;

import fredboat.FredBoat;
import fredboat.audio.player.PlayerRegistry;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by napster on 19.10.17.
 * <p>
 * Collects various FredBoat stats for prometheus
 */
public class FredBoatCollector extends Collector {

    @Override
    public List<MetricFamilySamples> collect() {

        List<MetricFamilySamples> mfs = new ArrayList<>();
        List<String> labelNames = Arrays.asList("shard", "entity"); //todo decide on 2nd label name

        GaugeMetricFamily jdaEntities = new GaugeMetricFamily("fredboat_jda_entities",
                "Amount of JDA entities", labelNames);
        mfs.add(jdaEntities);

        GaugeMetricFamily playersPlaying = new GaugeMetricFamily("fredboat_playing_music_players",
                "Currently playing music players", labelNames);
        mfs.add(playersPlaying);


        //per shard stats
        for (FredBoat fb : FredBoat.getShards()) {
            String shardId = Integer.toString(fb.getShardId());
            jdaEntities.addMetric(Arrays.asList(shardId, "User"), fb.getUserCount());
            jdaEntities.addMetric(Arrays.asList(shardId, "Guild"), fb.getGuildCount());
            jdaEntities.addMetric(Arrays.asList(shardId, "TextChannel"), fb.getTextChannelCount());
            jdaEntities.addMetric(Arrays.asList(shardId, "VoiceChannel"), fb.getVoiceChannelCount());
            jdaEntities.addMetric(Arrays.asList(shardId, "Category"), fb.getCategoriesCount());
            jdaEntities.addMetric(Arrays.asList(shardId, "Emote"), fb.getEmotesCount());
            jdaEntities.addMetric(Arrays.asList(shardId, "Role"), fb.getRolesCount());
        }

        //global stats
        jdaEntities.addMetric(Arrays.asList("total", "User"), FredBoat.getTotalUniqueUsersCount());
        jdaEntities.addMetric(Arrays.asList("total", "Guild"), FredBoat.getTotalGuildsCount());
        jdaEntities.addMetric(Arrays.asList("total", "TextChannel"), FredBoat.getTotalTextChannelsCount());
        jdaEntities.addMetric(Arrays.asList("total", "VoiceChannel"), FredBoat.getTotalVoiceChannelsCount());
        jdaEntities.addMetric(Arrays.asList("total", "Category"), FredBoat.getTotalCategoriesCount());
        jdaEntities.addMetric(Arrays.asList("total", "Emote"), FredBoat.getTotalEmotesCount());
        jdaEntities.addMetric(Arrays.asList("total", "Role"), FredBoat.getTotalRolesCount());
        playersPlaying.addMetric(Arrays.asList("total", "Players"), PlayerRegistry.playingCount());

        return mfs;
    }
}
