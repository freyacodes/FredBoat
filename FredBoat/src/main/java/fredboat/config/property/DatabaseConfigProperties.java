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

package fredboat.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import space.npstr.sqlsauce.ssh.SshTunnel;

import javax.annotation.Nullable;

/**
 * Created by napster on 03.03.18.
 */
@Component
@ConfigurationProperties(prefix = "database")
@SuppressWarnings("unused")
public class DatabaseConfigProperties implements DatabaseConfig {

    private Db main = new Db();
    private Db cache = new Db();

    @Override
    public String getMainJdbcUrl() {
        return main.getJdbcUrl();
    }

    @Nullable
    @Override
    public SshTunnel.SshDetails getMainSshTunnelConfig() {
        return main.getTunnel().toDetails();
    }

    @Nullable
    @Override
    public String getCacheJdbcUrl() {
        String cacheJdbcUrl = cache.getJdbcUrl();
        return !cacheJdbcUrl.isEmpty() ? cacheJdbcUrl : null;
    }

    @Nullable
    @Override
    public SshTunnel.SshDetails getCacheSshTunnelConfig() {
        return cache.getTunnel().toDetails();
    }

    public void setMain(Db main) {
        this.main = main;
    }

    public void setCache(Db cache) {
        this.cache = cache;
    }

    private static class Db {
        private String jdbcUrl = "";
        private TunnelProperties tunnel = new TunnelProperties();

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        public void setJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }

        public TunnelProperties getTunnel() {
            return tunnel;
        }

        public void setTunnel(TunnelProperties tunnel) {
            this.tunnel = tunnel;
        }
    }

    private static class TunnelProperties {

        private String host = "";
        private String user = "";
        private String privateKeyFile = "";
        private String keyPass = "";
        private int localPort = 9333;
        private int remotePort = 5432;

        @Nullable
        public SshTunnel.SshDetails toDetails() {
            return host.isEmpty()
                    ? null
                    : new SshTunnel.SshDetails(host, user)
                    .setKeyFile(privateKeyFile)
                    .setPassphrase(keyPass)
                    .setLocalPort(localPort)
                    .setRemotePort(remotePort);
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getPrivateKeyFile() {
            return privateKeyFile;
        }

        public void setPrivateKeyFile(String privateKeyFile) {
            this.privateKeyFile = privateKeyFile;
        }

        public String getKeyPass() {
            return keyPass;
        }

        public void setKeyPass(String keyPass) {
            this.keyPass = keyPass;
        }

        public int getLocalPort() {
            return localPort;
        }

        public void setLocalPort(int localPort) {
            this.localPort = localPort;
        }

        public int getRemotePort() {
            return remotePort;
        }

        public void setRemotePort(int remotePort) {
            this.remotePort = remotePort;
        }
    }
}
