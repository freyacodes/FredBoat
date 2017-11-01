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

package fredboat.feature.metrics;

import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.EventListener;
import okhttp3.Handshake;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;

/**
 * Created by napster on 18.10.17.
 * <p>
 * for okhttp
 * just a really dumb counter, we'll see how useful it is
 */
public class OkHttpEventMetrics extends EventListener {

    //a recognizable name of the instance of the okhttp client. should not be changed between releases to keep metrics
    private String instanceLabel;

    public OkHttpEventMetrics(String instanceLabel) {
        this.instanceLabel = instanceLabel;
    }

    @Override
    public void callStart(Call call) {
        Metrics.httpEventCounter.labels(instanceLabel, "callStart").inc();
    }

    @Override
    public void dnsStart(Call call, String domainName) {
        Metrics.httpEventCounter.labels(instanceLabel, "dnsStart").inc();
    }

    @Override
    public void dnsEnd(Call call, String domainName, @Nullable List<InetAddress> inetAddressList) {
        Metrics.httpEventCounter.labels(instanceLabel, "dnsEnd").inc();
    }

    @Override
    public void connectStart(Call call, InetSocketAddress inetSocketAddress, Proxy proxy) {
        Metrics.httpEventCounter.labels(instanceLabel, "connectStart").inc();
    }

    @Override
    public void secureConnectStart(Call call) {
        Metrics.httpEventCounter.labels(instanceLabel, "secureConnectStart").inc();
    }

    @Override
    public void secureConnectEnd(Call call, @Nullable Handshake handshake) {
        Metrics.httpEventCounter.labels(instanceLabel, "secureConnectEnd").inc();
    }

    @Override
    public void connectEnd(Call call, InetSocketAddress inetSocketAddress, @Nullable Proxy proxy, @Nullable Protocol protocol) {
        Metrics.httpEventCounter.labels(instanceLabel, "connectEnd").inc();
    }

    @Override
    public void connectFailed(Call call, InetSocketAddress inetSocketAddress, @Nullable Proxy proxy, @Nullable Protocol protocol, @Nullable IOException ioe) {
        Metrics.httpEventCounter.labels(instanceLabel, "connectFailed").inc();
    }

    @Override
    public void connectionAcquired(Call call, Connection connection) {
        Metrics.httpEventCounter.labels(instanceLabel, "connectionAcquired").inc();
    }

    @Override
    public void connectionReleased(Call call, Connection connection) {
        Metrics.httpEventCounter.labels(instanceLabel, "connectionReleased").inc();
    }

    @Override
    public void requestHeadersStart(Call call) {
        Metrics.httpEventCounter.labels(instanceLabel, "requestHeadersStart").inc();
    }

    @Override
    public void requestHeadersEnd(Call call, Request request) {
        Metrics.httpEventCounter.labels(instanceLabel, "requestHeadersEnd").inc();
    }

    @Override
    public void requestBodyStart(Call call) {
        Metrics.httpEventCounter.labels(instanceLabel, "requestBodyStart").inc();
    }

    @Override
    public void requestBodyEnd(Call call, long byteCount) {
        Metrics.httpEventCounter.labels(instanceLabel, "requestBodyEnd").inc();
    }

    @Override
    public void responseHeadersStart(Call call) {
        Metrics.httpEventCounter.labels(instanceLabel, "responseHeadersStart").inc();
    }

    @Override
    public void responseHeadersEnd(Call call, Response response) {
        Metrics.httpEventCounter.labels(instanceLabel, "responseHeadersEnd").inc();
    }

    @Override
    public void responseBodyStart(Call call) {
        Metrics.httpEventCounter.labels(instanceLabel, "responseBodyStart").inc();
    }

    @Override
    public void responseBodyEnd(Call call, long byteCount) {
        Metrics.httpEventCounter.labels(instanceLabel, "responseBodyEnd").inc();
    }

    @Override
    public void callEnd(Call call) {
        Metrics.httpEventCounter.labels(instanceLabel, "callEnd").inc();
    }

    @Override
    public void callFailed(Call call, IOException ioe) {
        Metrics.httpEventCounter.labels(instanceLabel, "callFailed").inc();
    }
}

