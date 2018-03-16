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

import fredboat.config.property.Credentials;
import fredboat.feature.metrics.BotMetrics;
import fredboat.jda.ShardProvider;
import fredboat.main.BotController;
import fredboat.util.rest.Http;
import net.dv8tion.jda.core.JDA;
import okhttp3.Response;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CarbonitexAgent extends FredBoatAgent {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(CarbonitexAgent.class);

    private final Credentials credentials;
    private final BotMetrics botMetrics;
    private final ShardProvider shardProvider;

    public CarbonitexAgent(Credentials credentials, BotMetrics botMetrics, ShardProvider shardProvider) {
        super("carbonitex", 30, TimeUnit.MINUTES);
        this.credentials = credentials;
        this.botMetrics = botMetrics;
        this.shardProvider = shardProvider;
    }

    @Override
    public void doRun() {
        synchronized (this) {
            sendStats();
        }
    }

    private void sendStats() {
        AtomicBoolean allConnected = new AtomicBoolean(true);
        AtomicInteger shardCounter = new AtomicInteger(0);
        shardProvider.streamShards().forEach(shard -> {
            shardCounter.incrementAndGet();
            if (shard.getStatus() != JDA.Status.CONNECTED) {
                allConnected.set(false);
            }
        });
        if (!allConnected.get()) {
            log.warn("Skipping posting stats because not all shards are online!");
            return;
        }

        if (shardCounter.get() < credentials.getRecommendedShardCount()) {
            log.warn("Skipping posting stats because not all shards initialized!");
            return;
        }

        try (Response response = BotController.HTTP.post("https://www.carbonitex.net/discord/data/botdata.php",
                Http.Params.of(
                        "key", credentials.getCarbonKey(),
                        "servercount", Integer.toString(botMetrics.getTotalGuildsCount())
                ))
                .execute()) {

            //noinspection ConstantConditions
            String content = response.body().string();
            if (response.isSuccessful()) {
                log.info("Successfully posted the bot data to carbonitex.com: {}", content);
            } else {
                log.warn("Failed to post stats to Carbonitex: {}\n{}", response.toString(), content);
            }
        } catch (Exception e) {
            log.error("An error occurred while posting the bot data to carbonitex.com", e);
        }
    }

}
