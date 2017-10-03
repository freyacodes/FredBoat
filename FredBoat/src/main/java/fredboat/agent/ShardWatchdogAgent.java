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
import fredboat.feature.togglz.FeatureFlags;
import fredboat.shared.constant.DistributionEnum;
import net.dv8tion.jda.core.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ShardWatchdogAgent extends FredBoatAgent {

    private static final Logger log = LoggerFactory.getLogger(ShardWatchdogAgent.class);
    private static final int ACCEPTABLE_SILENCE = getAcceptableSilenceThreshold();

    public ShardWatchdogAgent() {
        super("shard watchdog", 10, TimeUnit.SECONDS);
    }

    @Override
    public void doRun() {
        try {
            inspect();
        } catch (Exception e) {
            log.error("Caught an exception while trying kill dead shards!", e);
        }
    }

    private void inspect() throws InterruptedException {
        if (!FeatureFlags.SHARD_WATCHDOG.isActive()) {
            return;
        }

        List<FredBoat> shards = FredBoat.getShards();

        for(FredBoat shard : shards) {
            ShardWatchdogListener listener = shard.getShardWatchdogListener();

            long diff = System.currentTimeMillis() - listener.getLastEventTime();

            JDA.Status status = shard.getJda().getStatus();
            if (diff > ACCEPTABLE_SILENCE) {
                if (listener.getEventCount() < 100) {
                    log.warn("Did not revive shard {} status {} because it did not receive enough events since construction!", shard.getShardInfo(), status);
                } else if (isReconnecting(status)) {
                    log.warn("Did not revive shard {} status {} because it is reconnecting already!", shard.getShardInfo(), status);
                } else {
                    log.warn("Reviving shard {} after {} seconds of no events. Status: {}. Last event received was {}",
                            shard.getShardInfo(), (diff / 1000), status, listener.getLastEvent());

                    /*try {
                        log.info("Thread dump for shard's JDA threads at time of death: " + getShardThreadDump(shard.getShardInfo().getShardId()));
                    } catch (Exception e) {
                        log.error("Got exception while printing thread dump after shard death was detected");
                    }*/

                    shard.revive();
                    Thread.sleep(5000);
                }
            }
        }
    }

    private boolean isReconnecting(JDA.Status status) {
        return status == JDA.Status.ATTEMPTING_TO_RECONNECT || status == JDA.Status.WAITING_TO_RECONNECT;
    }

    private static int getAcceptableSilenceThreshold() {
        if (Config.CONFIG.getDistribution() == DistributionEnum.DEVELOPMENT) {
            return Integer.MAX_VALUE;
        }

        return Config.CONFIG.getNumShards() != 1 ? 30 * 1000 : 600 * 1000; //30 seconds or 10 minutes depending on shard count
    }
}
