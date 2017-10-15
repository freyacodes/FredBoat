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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class FredBoatAgent implements Runnable {

    private static final String IDLE_NAME = "idle agent worker thread";
    private static final String RUNNING_NAME = "%s agent worker thread";

    private static final Map<Class<? extends FredBoatAgent>, Long> LAST_RUN_TIME = new ConcurrentHashMap<>();

    private static final ScheduledExecutorService AGENTS = Executors.newScheduledThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, IDLE_NAME);
        thread.setPriority(4);
        return thread;
    });

    //only one of each agent, non-static is fine
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String name;
    private final long millisToSleep;

    protected FredBoatAgent(String name, long millisToSleep) {
        this.name = String.format(RUNNING_NAME, name);
        this.millisToSleep = millisToSleep;
    }

    protected FredBoatAgent(String name, long timeToSleep, TimeUnit unitToSleep) {
        this(name, unitToSleep.toMillis(timeToSleep));
    }

    @Override
    public final void run() {
        LAST_RUN_TIME.put(this.getClass(), System.currentTimeMillis());
        try {
            Thread.currentThread().setName(name);
            doRun();
        } catch (Throwable t) {
            log.warn("Whoa! Unhandled throwable!", t);
        } finally {
            Thread.currentThread().setName(IDLE_NAME);
        }
    }

    protected abstract void doRun();

    public static void start(FredBoatAgent agent) {
        AGENTS.scheduleAtFixedRate(agent, agent.millisToSleep, agent.millisToSleep, TimeUnit.MILLISECONDS);
        LAST_RUN_TIME.put(agent.getClass(), 0L);
    }

    public static Map<Class<? extends FredBoatAgent>, Long> getLastRunTimes() {
        return Collections.unmodifiableMap(LAST_RUN_TIME);
    }

    public static void shutdown() {
        AGENTS.shutdown();
    }
}
