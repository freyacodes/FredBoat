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

import fredboat.feature.metrics.Metrics;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Created by napster on 17.02.18.
 *
 * Part of the code adapted from https://stackoverflow.com/a/45877770, many thanks!
 */
@Configuration
public class RequestLoggerConfiguration {

    @Bean
    public RequestLogger logFilter() {
        return new RequestLogger();
    }

    private static class RequestLogger implements WebFilter {
        private static final Logger log = LoggerFactory.getLogger(RequestLogger.class);

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
            countAndLogRequest(exchange);
            Mono<Void> filter = chain.filter(exchange);
            exchange.getResponse().beforeCommit(() -> {
                logResponse(exchange);
                return Mono.empty();
            });
            return filter;
        }

        private void countAndLogRequest(ServerWebExchange exchange) {
            ServerHttpRequest request = exchange.getRequest();
            HttpMethod method = request.getMethod();
            String path = request.getURI().getPath();
            List<MediaType> acceptableMediaTypes = request.getHeaders().getAccept();
            MediaType contentType = request.getHeaders().getContentType();

            Metrics.apiServed.labels(path).inc();
            log.debug(">>> {} {} {}: {} {}: {}", method, path, HttpHeaders.ACCEPT, acceptableMediaTypes, HttpHeaders.CONTENT_TYPE, contentType);
        }

        private void logResponse(ServerWebExchange exchange) {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();
            HttpMethod method = request.getMethod();
            String path = request.getURI().getPath();
            HttpStatus statusCode = getStatus(response);
            MediaType contentType = response.getHeaders().getContentType();

            log.debug("<<< {} {} HTTP{} {} {}: {}", method, path, statusCode.value(), statusCode.getReasonPhrase(),
                    HttpHeaders.CONTENT_TYPE, contentType);
        }

        private HttpStatus getStatus(ServerHttpResponse response) {
            HttpStatus statusCode = response.getStatusCode();
            return statusCode != null ? statusCode : HttpStatus.CONTINUE;
        }
    }
}
