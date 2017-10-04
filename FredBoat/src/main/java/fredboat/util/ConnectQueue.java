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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
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

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final Object jdaReconnectWorkerLock = new Object();
    private volatile Future scheduledJdaWorker;

    //this queue is not allowed to have more than one coin
    private DelayQueue<Coin> coinService = new DelayQueue<>(Collections.singletonList(new Coin(0, TimeUnit.MILLISECONDS)));

    /**
     * These coins are meant for immediate use.
     * Calling this will block until a coin becomes available
     */
    public void getCoin(int shardId) throws InterruptedException {
        long start = System.currentTimeMillis();
        log.info("Shard {} requesting coin", shardId);
        getCoin(false);
        log.info("Shard {} received coin after {}ms", shardId, System.currentTimeMillis() - start);
    }

    private void getCoin(boolean isJdaWorker) throws InterruptedException {
        //do not give out coins while the JDA reconnect queue is running
        if (!isJdaWorker) { //dont let the jda worker wait for itself
            Future jdaWorker = null;
            synchronized (jdaReconnectWorkerLock) {
                if (isJdaWorkerRunning()) {
                    jdaWorker = scheduledJdaWorker;
                    log.info("JDA reconnect queue is running, shards left to reconnect: {}", reconnectQueue.size());
                }
            }
            //wait for the jdaWorker to be done
            if (jdaWorker != null) {
                try {
                    jdaWorker.get();
                } catch (ExecutionException e) {
                    log.error("jda worker exception", e);
                }
            }
        }

        Coin c = coinService.take();
        log.info("Took coin with delay {}ms", c.getDelay(TimeUnit.MILLISECONDS));
        if (!isJdaWorker) { //the JDA worker will add its own coin when it's done
            coinService.add(new Coin());
        }
    }

    //implementation notes:
    //this is called each time a session is appended by JDA
    //in the super method it would cause a worker to start if there isnt one started yet, a worker that will handle JDA reconnects only
    //we need to make sure that a new worker is started with a proper delay to our own connection stuff
    //we need to make sure that after the JDA worker is done, we wait an appropriate amount of time, since it will not sleep after being done with its queue
    //further on, we need to respect the JDA worker running itself again (rare race condition :tm:)
    //this method must never be blocking
    @Override
    protected void runWorker() {
        // handle the rare race condition:tm: case where the jda SessionReconnectQueue.ReconnectThread calls runWorker() again
        if (Thread.currentThread().equals(reconnectThread)) {
            log.warn(":clap: race :clap: condition :clap: triggered :clap:, lets hope the following code handles it properly." +
                    "\nIf FredBoat dies shortly after this log line...blame Napster.");
        }

        synchronized (jdaReconnectWorkerLock) {
            if (!isJdaWorkerRunning() ||
                    //if this is the JDA reconnect thread, it is allowed to schedule another worker
                    // (see the rare race condition:tm: comment in SessionReconnectQueue.ReconnectThread)
                    Thread.currentThread().equals(reconnectThread)) {

                log.info("Scheduling jda reconnect worker");
                scheduledJdaWorker = scheduler.submit(this::runJdaReconnectWorker);
            } else {
                log.info("jda reconnect worker is already running/scheduled");
            }
        }
    }

    private boolean isJdaWorkerRunning() {
        return scheduledJdaWorker != null
                && !scheduledJdaWorker.isDone()
                && !scheduledJdaWorker.isCancelled();
    }

    //this method blocks until the JDA reconnect worker is done
    private void runJdaReconnectWorker() {
        try {
            //make sure we wait enough before allowing JDA to do its reconnects
            getCoin(true);
            log.info("Starting jda reconnect worker");
            super.runWorker();

            //wait for JDAs reconnect thread to be done
            Thread recThread = reconnectThread;
            //if it is null at this point it got done running before we reached this point so we can skip right ahead
            if (recThread != null && !Thread.currentThread().equals(recThread)) {//don't Thread.join() on ourselves accidently
                log.info("Waiting on the jda reconnect worker to be done");
                recThread.join();
            }

            log.info("We done here bois, pull me out");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Got interrupted while waiting for the jda reconnect worker", e);
        } finally {
            //jda reconnect worker is done, add a coin with a fresh delay
            coinService.add(new Coin());
        }
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
            throw new UnsupportedOperationException();
        }
    }
}
