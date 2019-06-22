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

package fredboat.config;

import io.prometheus.client.logback.InstrumentedAppender;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by napster on 24.02.18.
 */
@Configuration
public class MetricsConfiguration {

    //guava cache metrics
    @Bean
    public io.prometheus.client.guava.cache.CacheMetricsCollector guavaCacheMetrics() {
        return new io.prometheus.client.guava.cache.CacheMetricsCollector().register();
    }

    @Bean
    public io.prometheus.client.cache.caffeine.CacheMetricsCollector caffeineCacheMetrics() {
        return new io.prometheus.client.cache.caffeine.CacheMetricsCollector().register();
    }

    @Bean
    public InstrumentedAppender instrumentedAppender() {
        return new InstrumentedAppender();
    }
}
