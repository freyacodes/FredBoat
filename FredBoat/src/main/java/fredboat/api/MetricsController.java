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

package fredboat.api;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by napster on 18.10.17.
 * <p>
 * Used to expose the prometheus metrics with a reactive web server. Some code copied from prometheus own MetricsServlet
 */
@RestController
@RequestMapping("/metrics")
public class MetricsController {

    private CollectorRegistry registry;

    public MetricsController() {
        registry = CollectorRegistry.defaultRegistry;
    }

    @GetMapping(produces = TextFormat.CONTENT_TYPE_004)
    public Mono<ResponseEntity<String>> getMetrics(@RequestParam(name = "name[]", required = false) String[] includedParam) {
        return Mono.fromCallable(() -> buildAnswer(includedParam));
    }

    private ResponseEntity<String> buildAnswer(String[] includedParam) throws IOException {
        Set<String> params;
        if (includedParam == null) {
            params = Collections.emptySet();
        } else {
            params = new HashSet<>(Arrays.asList(includedParam));
        }

        Writer writer = new StringWriter();
        try {
            TextFormat.write004(writer, registry.filteredMetricFamilySamples(params));
            writer.flush();
        } finally {
            writer.close();
        }

        return new ResponseEntity<>(writer.toString(), HttpStatus.OK);
    }
}
