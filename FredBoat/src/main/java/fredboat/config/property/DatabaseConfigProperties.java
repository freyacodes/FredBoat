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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfigProperties.class);

    private Db main = new Db();
    private Db cache = new Db();

    @Override
    public String getMainJdbcUrl() {
        String jdbcUrl = main.getJdbcUrl();
        //noinspection ConstantConditions
        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            if ("docker".equals(System.getenv("ENV"))) {
                log.info("No main JDBC URL found, docker environment detected. Using default docker main JDBC url");
                jdbcUrl = "jdbc:postgresql://db:5432/fredboat?user=fredboat";
                main.setJdbcUrl(jdbcUrl);
            } else {
                String message = "No main jdbcUrl provided in a non-docker environment. FredBoat cannot work without a database.";
                log.error(message);
                throw new RuntimeException(message);
            }
        }
        return jdbcUrl;
    }

    @Nullable
    @Override
    public SshTunnel.SshDetails getMainSshTunnelConfig() {
        return main.getTunnel().toDetails();
    }

    private boolean hasWarnedEmptyCacheJdbc = false;
    private boolean hasWarnedSameJdbcs = false;


    @Nullable
    @Override
    public String getCacheJdbcUrl() {
        String jdbcUrl = cache.getJdbcUrl();
        //noinspection ConstantConditions
        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            if ("docker".equals(System.getenv("ENV"))) {
                log.info("No cache jdbcUrl found, docker environment detected. Using default docker cache JDBC url");
                jdbcUrl = "jdbc:postgresql://db:5432/fredboat_cache?user=fredboat";
                cache.setJdbcUrl(jdbcUrl);
            } else {
                if (!hasWarnedEmptyCacheJdbc) {
                    log.warn("No cache jdbcUrl provided in a non-docker environment. This may lead to a degraded performance, "
                            + "especially in a high usage environment, or when using Spotify playlists.");
                    hasWarnedEmptyCacheJdbc = true;
                }
                jdbcUrl = "";
            }
        }

        if (!jdbcUrl.isEmpty() && jdbcUrl.equals(getMainJdbcUrl())) {
            if (!hasWarnedSameJdbcs) {
                log.warn("The main and cache jdbc urls may not point to the same database due to how flyway handles migrations. "
                        + "The cache database will not be available in this execution of FredBoat. This may lead to a degraded performance, "
                        + "especially in a high usage environment, or when using Spotify playlists.");
                hasWarnedSameJdbcs = true;
            }
            jdbcUrl = "";
        }

        return !jdbcUrl.isEmpty() ? jdbcUrl : null;
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
