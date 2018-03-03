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

package fredboat.test;

import fredboat.config.property.*;
import fredboat.shared.constant.DistributionEnum;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import space.npstr.sqlsauce.ssh.SshTunnel;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by napster on 19.02.18.
 * <p>
 * A default fake config to be used in tests.
 */
public class MockConfig implements AppConfig, AudioSourcesConfig, Credentials, DatabaseConfig, EventLoggerConfig,
        LavalinkConfig, TestingConfig {

    private static final Logger log = LoggerFactory.getLogger(MockConfig.class);

    private final DistributionEnum distributionEnum = DistributionEnum.DEVELOPMENT;

    private String testBotToken;
    private String testChannelId;

    public MockConfig() {
        try {
            Yaml yaml = new Yaml();
            String credsFileStr = FileUtils.readFileToString(new File("fredboat.yaml"), "UTF-8");
            Map<String, Object> creds = yaml.load(credsFileStr);
            creds.keySet().forEach((String key) -> creds.putIfAbsent(key, ""));

            testBotToken = (String) creds.getOrDefault("testToken", "");
            testChannelId = (String) creds.getOrDefault("testChannelId", "");
        } catch (Exception e) {
            log.info("Failed to load test values", e);
            testBotToken = "";
            testChannelId = "";
        }
    }

    @Override
    public String getTestBotToken() {
        return testBotToken;
    }

    @Override
    public String getTestChannelId() {
        return testChannelId;
    }

    @Override
    public DistributionEnum getDistribution() {
        return distributionEnum;
    }

    @Override
    public int getRecommendedShardCount() {
        return 1;
    }

    @Override
    public boolean isRestServerEnabled() {
        return false;
    }

    @Override
    public List<String> getAdminIds() {
        return Collections.emptyList();
    }

    @Override
    public boolean useAutoBlacklist() {
        return false;
    }

    @Override
    public String getGame() {
        return "Passing all tests";
    }

    @Override
    public boolean getContinuePlayback() {
        return false;
    }

    @Override
    public boolean isYouTubeEnabled() {
        return false;
    }

    @Override
    public boolean isSoundCloudEnabled() {
        return false;
    }

    @Override
    public boolean isBandCampEnabled() {
        return false;
    }

    @Override
    public boolean isTwitchEnabled() {
        return false;
    }

    @Override
    public boolean isVimeoEnabled() {
        return false;
    }

    @Override
    public boolean isMixerEnabled() {
        return false;
    }

    @Override
    public boolean isSpotifyEnabled() {
        return false;
    }

    @Override
    public boolean isLocalEnabled() {
        return false;
    }

    @Override
    public boolean isHttpEnabled() {
        return false;
    }

    @Override
    public String getBotToken() {
        return "";
    }

    @Override
    public List<String> getGoogleKeys() {
        return Collections.emptyList();
    }

    @Override
    public String getMalUser() {
        return "";
    }

    @Override
    public String getMalPassword() {
        return "";
    }

    @Override
    public String getImgurClientId() {
        return "";
    }

    @Override
    public String getSpotifyId() {
        return "";
    }

    @Override
    public String getSpotifySecret() {
        return "";
    }

    @Override
    public String getOpenWeatherKey() {
        return "";
    }

    @Override
    public String getSentryDsn() {
        return "";
    }

    @Override
    public String getMainJdbcUrl() {
        return "";
    }

    @Nullable
    @Override
    public SshTunnel.SshDetails getMainSshTunnelConfig() {
        return null;
    }

    @Nullable
    @Override
    public String getCacheJdbcUrl() {
        return null;
    }

    @Nullable
    @Override
    public SshTunnel.SshDetails getCacheSshTunnelConfig() {
        return null;
    }

    @Override
    public List<LavalinkHost> getLavalinkHosts() {
        return Collections.emptyList();
    }

    @Override
    public String getEventLogWebhook() {
        return "";
    }

    @Override
    public int getEventLogInterval() {
        return 1;
    }

    @Override
    public String getGuildStatsWebhook() {
        return "";
    }

    @Override
    public int getGuildStatsInterval() {
        return 1;
    }

    @Override
    public int getHikariPoolSize() {
        return 4;
    }

    @Override
    public String getCarbonKey() {
        return "";
    }

    @Override
    public String getDikeUrl() {
        return "";
    }
}
