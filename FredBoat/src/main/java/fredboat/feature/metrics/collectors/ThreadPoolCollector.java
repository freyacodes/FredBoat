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

import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;
import io.prometheus.client.GaugeMetricFamily;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by napster on 19.10.17.
 */
@Component
public class ThreadPoolCollector extends Collector {

    protected final ConcurrentMap<String, ThreadPoolExecutor> pools = new ConcurrentHashMap<>();

    /**
     * Add or replace the pool with the given name.
     * <p>
     * Any references to any previous pool with this name are invalidated.
     *
     * @param poolName The name of the pool, will be the metrics label value
     * @param pool     The pool being monitored
     */
    public void addPool(String poolName, ThreadPoolExecutor pool) {
        pools.put(poolName, pool);
    }

    /**
     * Remove the pool with the given name.
     * <p>
     * Any references to the pool are invalidated.
     *
     * @param poolName pool to be removed
     */
    public ThreadPoolExecutor removePool(String poolName) {
        return pools.remove(poolName);
    }

    /**
     * Remove all pools.
     * <p>
     * Any references to all pools are invalidated.
     */
    public void clear() {
        pools.clear();
    }


    @Override
    public List<MetricFamilySamples> collect() {

        List<MetricFamilySamples> mfs = new ArrayList<>();
        List<String> labelNames = Collections.singletonList("name");

        GaugeMetricFamily activeThreads = new GaugeMetricFamily("fredboat_threadpool_active_threads_current",
                "Amount of active threads in a thread pool", labelNames);
        mfs.add(activeThreads);

        GaugeMetricFamily queueSize = new GaugeMetricFamily("fredboat_threadpool_queue_size_current",
                "Size of queue of a thread pool (including scheduled tasks)", labelNames);
        mfs.add(queueSize);

        CounterMetricFamily completedTasks = new CounterMetricFamily("fredboat_threadpool_completed_tasks_total",
                "Total completed tasks by a thread pool", labelNames);
        mfs.add(completedTasks);

        for (Map.Entry<String, ThreadPoolExecutor> entry : pools.entrySet()) {
            String poolName = entry.getKey();
            ThreadPoolExecutor pool = entry.getValue();
            List<String> labels = Collections.singletonList(poolName);

            activeThreads.addMetric(labels, pool.getActiveCount());
            queueSize.addMetric(labels, pool.getQueue().size());
            completedTasks.addMetric(labels, pool.getCompletedTaskCount()); //guaranteed to always increase, ergo good fit for a counter
        }

        return mfs;
    }
}
