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

package fredboat.agent;

import fredboat.Config;
import fredboat.FredBoat;
import fredboat.event.ShardWatchdogListener;
import fredboat.util.DistributionEnum;
import net.dv8tion.jda.core.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ShardWatchdogAgent extends Thread {

    private static final Logger log = LoggerFactory.getLogger(ShardWatchdogAgent.class);
    private static final int INTERVAL_MILLIS = 10000; // 10 secs
    private static final int ACCEPTABLE_SILENCE = getAcceptableSilenceThreshold();

    @Override
    public void run() {
        log.info("Started shard watchdog");

        //noinspection InfiniteLoopStatement
        while (true) {
            try {
                inspect();
                sleep(INTERVAL_MILLIS);
            } catch (Exception e) {
                log.error("Caught an exception while trying kill dead shards!", e);
                try {
                    sleep(1000);
                } catch (InterruptedException e1) {
                    throw new RuntimeException(e1);
                }
            }
        }
    }

    private void inspect() throws InterruptedException {
        List<FredBoat> shards = FredBoat.getShards();

        for(FredBoat shard : shards) {
            ShardWatchdogListener listener = shard.getShardWatchdogListener();

            long diff = System.currentTimeMillis() - listener.getLastEventTime();

            if(diff > ACCEPTABLE_SILENCE) {
                if (shard.getJda().getStatus() == JDA.Status.SHUTDOWN) {
                    log.warn("Did not revive shard " + shard.getShardInfo() + " because it was shut down!");
                } else if(listener.getEventCount() < 100) {
                    log.warn("Did not revive shard " + shard.getShardInfo() + " because it did not receive enough events since construction!");
                } else {
                    log.warn("Reviving shard " + shard.getShardInfo() + " after " + (diff / 1000) +
                            " seconds of no events. Last event received was " + listener.getLastEvent());
                    shard.revive();
                    sleep(5000);
                }
            }
        }
    }

    private static int getAcceptableSilenceThreshold() {
        if(Config.CONFIG.getDistribution() == DistributionEnum.DEVELOPMENT) {
            return Integer.MAX_VALUE;
        }

        return Config.CONFIG.getNumShards() != 1 ? 30 * 1000 : 600 * 1000; //30 seconds or 10 minutes depending on shard count
    }
}
