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
import fredboat.jda.ShardProvider;
import fredboat.util.func.NonnullSupplier;
import net.dv8tion.jda.core.JDA;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by napster on 25.02.18.
 */
@Component
public class ShardStatsCounterProvider {

    private final ConcurrentHashMap<Integer, ShardStatsCounter> shardStatsCounters = new ConcurrentHashMap<>();
    private final StatsAgent statsAgent;
    private final ShardProvider shardProvider;

    public ShardStatsCounterProvider(StatsAgent statsAgent, ShardProvider shardProvider) {
        this.statsAgent = statsAgent;
        this.shardProvider = shardProvider;
    }

    public void registerShard(JDA.ShardInfo shardInfo) {
        int shardId = shardInfo.getShardId();
        shardStatsCounters.computeIfAbsent(shardId, id -> {
            ShardStatsCounter shardStatsCounter = create(shardInfo, () -> shardProvider.getShardById(shardId));
            statsAgent.addAction(shardStatsCounter);
            return shardStatsCounter;
        });
    }

    private ShardStatsCounter create(JDA.ShardInfo shardInfo, NonnullSupplier<JDA> jdaSupplier) {
        return new ShardStatsCounter(shardInfo,
                        () -> new BotMetrics.JdaEntityCounts().count(
                                () -> Collections.singletonList(jdaSupplier.get())
                        )
        );
    }

    private static class ShardStatsCounter implements StatsAgent.Action {
        private final JDA.ShardInfo shardInfo;
        private final Runnable action;

        ShardStatsCounter(JDA.ShardInfo shardInfo, Runnable action) {
            this.shardInfo = shardInfo;
            this.action = action;
        }


        @Override
        public String getName() {
            return "jda entity stats for shard " + shardInfo.getShardString();
        }


        @Override
        public void act() {
            action.run();
        }
    }

}
