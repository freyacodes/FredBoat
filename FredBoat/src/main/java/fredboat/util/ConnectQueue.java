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

package fredboat.util;

import net.dv8tion.jda.core.requests.SessionReconnectQueue;
import net.dv8tion.jda.core.requests.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by napster on 04.10.17.
 * <p>
 * This class rate limits all Discord logins properly.
 * This means it needs to take care of the following sources of (re)connects:
 * <p>
 * - JDAs reconnects (may happen anytime, especially feared during start up)
 * - Reviving a shard (either by watchdog or admin command) (may happen anytime though they are rather rare)
 * - Creating a shard (happens only during start time)
 * <p>
 * This class achieves its goal by implementing a token like system of coins, which need to be requested before doing
 * any login (reviving or creating shards). In case JDA queues reconnects, those get immediate priority over our own
 * logins. The coin system will wait until the JDA reconnect queue is done and only then issue new login coins.
 */
public class ConnectQueue extends SessionReconnectQueue {

    private static final Logger log = LoggerFactory.getLogger(ConnectQueue.class);
    public static final int CONNECT_DELAY_MS = (WebSocketClient.IDENTIFY_DELAY * 1000) + 500; //5500 ms

    private final CoinProvider coinProvider;

    public ConnectQueue() {
        this(new CoinProvider());
    }

    private ConnectQueue(CoinProvider coinProvider) {
        super(new WebSocketQueue(coinProvider));
        this.coinProvider = coinProvider;
    }

    /**
     * These coins are meant for immediate use.
     * Calling this will block until a coin becomes available
     */
    public void requestCoin(int shardId) throws InterruptedException {
        long start = System.currentTimeMillis();
        log.info("Shard {} requesting coin", shardId);

        //if there is a reconnect going on by JDA, wait for it to be done)
        Thread jdaReconnectThread = this.reconnectThread;
        while (jdaReconnectThread != null) {
            log.info("Waiting on JDA reconnect to be done");
            jdaReconnectThread.join();
            jdaReconnectThread = this.reconnectThread; // handle the race condition mentioned in SessionReconnectQueue#ReconnectThread
            Thread.sleep(CONNECT_DELAY_MS); // back off a few more seconds because the reconnect thread exits early
        }

        coinProvider.takeCoin();
        log.info("Shard {} received coin after {}ms", shardId, System.currentTimeMillis() - start);
    }


    private static class CoinProvider {
        //this queue is not allowed to have more than one coin
        private DelayQueue<Coin> coin = new DelayQueue<>(Collections.singletonList(new Coin(0, TimeUnit.MILLISECONDS)));

        protected void takeCoin() throws InterruptedException {
            Coin c = coin.take();
            log.info("Took coin with delay {}ms", c.getDelay(TimeUnit.MILLISECONDS));
            coin.add(new Coin());
        }

        private static class Coin implements Delayed {

            private long valid; //the point in time when this coin becomes valid

            //create a coin with default timeout
            public Coin() {
                this(CONNECT_DELAY_MS, TimeUnit.MILLISECONDS);
            }

            public Coin(long delay, TimeUnit unit) {
                valid = System.currentTimeMillis() + unit.toMillis(delay);
            }

            @Override
            public long getDelay(@Nonnull TimeUnit unit) {
                return unit.convert(valid - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            }

            @Override
            public int compareTo(@Nonnull Delayed o) {
                throw new UnsupportedOperationException(); //there is only one of these meant to exist at any time
            }
        }
    }

    private static class WebSocketQueue extends LinkedBlockingQueue<WebSocketClient> {
        private static final long serialVersionUID = -7022487258759087625L;

        private final CoinProvider coinProvider;

        public WebSocketQueue(CoinProvider coinProvider) {
            this.coinProvider = coinProvider;
        }

        //this will make sure that the jda reconnect thread waits long enough when requesting their first reconnect
        @Override
        public WebSocketClient poll() {
            try {
                coinProvider.takeCoin();
            } catch (InterruptedException e) {
                log.error("Interrupted while getting coin for jda reconnect");
            }
            return super.poll();
        }
    }
}
