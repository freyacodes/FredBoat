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

package fredboat.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Created by napster on 11.10.17.
 * <p>
 * Does stuff regularly to even out workload. Use this to register any hefty counts.
 */
public class StatsAgent extends FredBoatAgent {

    private static final Logger log = LoggerFactory.getLogger(StatsAgent.class);

    private List<Action> countActions = new CopyOnWriteArrayList<>();

    public StatsAgent(String name, int time, TimeUnit timeUnit) {
        super(name, time, timeUnit);
    }

    public StatsAgent(String name) {
        super(name, 5, TimeUnit.MINUTES);
    }

    public void addAction(Action action) {
        countActions.add(action);
    }

    @Override
    protected void doRun() {
        for (Action action : countActions) {
            try {
                action.act();
            } catch (Exception e) {
                log.error("Unexpected exception when counting", e);
            }
        }
    }

    @FunctionalInterface
    public interface Action {
        void act() throws Exception;
    }

}
