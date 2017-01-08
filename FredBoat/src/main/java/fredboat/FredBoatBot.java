/*
 * MIT License
 *
 * Copyright (c) 2016 Frederik Ar. Mikkelsen
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

package fredboat;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import fredboat.event.EventLogger;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FredBoatBot extends FredBoat {

    private static final Logger log = LoggerFactory.getLogger(FredBoatBot.class);
    private final int shardId;

    FredBoatBot(int shardId) {
        this.shardId = shardId;

        log.info("Building shard " + shardId);

        try {
            boolean success = false;
            while (!success) {
                JDABuilder builder = new JDABuilder(AccountType.BOT)
                        .addListener(listenerBot)
                        .addListener(new EventLogger("216689009110417408"))
                        .setToken(accountToken)
                        .setBulkDeleteSplittingEnabled(true)
                        .setEnableShutdownHook(false)
                        .setAudioSendFactory(new NativeAudioSendFactory());
                if (numShards > 1) {
                    builder.useSharding(shardId, numShards);
                }
                try {
                    jda = builder.buildAsync();
                    success = true;
                } catch (RateLimitedException e) {
                    log.warn("Got rate limited while building bot JDA instance! Retrying...", e);
                    Thread.sleep(SHARD_CREATION_SLEEP_INTERVAL);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to start JDA shard " + shardId, e);
        }

    }

    public int getShardId() {
        return shardId;
    }

}
