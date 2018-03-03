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

import com.google.common.base.CharMatcher;
import com.google.common.base.Suppliers;
import fredboat.audio.player.PlayerLimitManager;
import fredboat.commandmeta.CommandInitializer;
import fredboat.shared.constant.DistributionEnum;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import space.npstr.sqlsauce.ssh.SshTunnel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class FileConfig implements AppConfig, AudioSourcesConfig, Credentials, EventLoggerConfig, DatabaseConfig, LavalinkConfig {

    private static final Logger log = LoggerFactory.getLogger(FileConfig.class);

    private static final Supplier<FileConfig> configSupplier = Suppliers.memoizeWithExpiration(FileConfig::loadConfig,
            1, TimeUnit.MINUTES);

    //a fallback reference to the last sucessfulyy loaded config, in case editing the config files breaks them
    @Nullable
    private static FileConfig lastSuccessfulLoaded;

    //todo: trace down all callers of this and make sure they take advantage of a reloading config like
    //todo  not storing the values once retrieved and recreating objects on value changes where possible/feasible
    public static FileConfig get() {
        return FileConfig.configSupplier.get();
    }


    private static FileConfig loadConfig() {
        long nanoTime = System.nanoTime();
        FileConfig c;
        try {
            c = new FileConfig(loadConfigFile("fredboat"));
        } catch (Exception e) {
            if (lastSuccessfulLoaded != null) {
                log.error("Reloading config file failed! Serving last successfully loaded one.", e);
                return lastSuccessfulLoaded;
            }
            throw new RuntimeException("Could not load config files!", e);
        }
        log.debug("Loading config took {}ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime));
        lastSuccessfulLoaded = c;
        return c;
    }

    //FileConfig

    private DistributionEnum distribution;
    private String prefix;
    private boolean restServerEnabled;
    private List<String> adminIds = new ArrayList<>();
    private boolean useAutoBlacklist;
    private String game;
    private boolean continuePlayback;

    // audio managers
    private boolean youtubeAudio;
    private boolean soundcloudAudio;
    private boolean bandcampAudio;
    private boolean twitchAudio;
    private boolean vimeoAudio;
    private boolean mixerAudio;
    private boolean spotifyAudio;
    private boolean localAudio;
    private boolean httpAudio;


    //Credentials

    // essential
    private String botToken;
    private List<String> googleKeys = new ArrayList<>();

    // apis
    private String malUser;
    private String malPassword;
    private String imgurClientId;
    private String spotifyId;
    private String spotifySecret;
    private String openWeatherKey;

    // main database + SSH tunnel
    @Nonnull
    private String jdbcUrl;
    @Nullable
    private SshTunnel.SshDetails mainSshTunnelConfig;

    // cache database + SSH tunnel
    @Nullable
    private String cacheJdbcUrl;
    @Nullable
    private SshTunnel.SshDetails cacheSshTunnelConfig;

    // misc
    private String sentryDsn;
    private List<LavalinkConfig.LavalinkHost> lavalinkHosts = new ArrayList<>();
    private String eventLogWebhook;
    private int eventLogInterval;
    private String guildStatsWebhook;
    private int guildStatsInterval;


    // Undocumented creds
    private String carbonKey;
    private String dikeUrl;


    /**
     * The config is regularly reloaded, it should not be doing any blocking calls.
     */
    @SuppressWarnings("unchecked")
    private FileConfig(File configFile) {
        try {
            Yaml yaml = new Yaml();
            String configFileStr = FileUtils.readFileToString(configFile, "UTF-8");
            //remove those pesky tab characters so a potential json file is YAML conform
            configFileStr = cleanTabs(configFileStr, "fredboat.yaml");

            Map<String, Object> config = yaml.load(configFileStr);

            //avoid null values, rather change them to empty strings
            config.keySet().forEach((String key) -> config.putIfAbsent(key, ""));


            //Load FileConfig values

            // Determine distribution
            if ((boolean) config.getOrDefault("development", true)) {
                distribution = DistributionEnum.DEVELOPMENT;
            } else if ((boolean) config.getOrDefault("patron", true)) {
                distribution = DistributionEnum.PATRON;
            } else {
                distribution = DistributionEnum.MUSIC;
            }
            log.info("Determined distribution: " + distribution);

            prefix = (String) config.getOrDefault("prefix", DEFAULT_PREFIX);
            log.info("Using prefix: " + prefix);
            restServerEnabled = (boolean) config.getOrDefault("restServerEnabled", true);

            Object admins = config.get("admins");
            if (admins instanceof List) {
                ((List) admins).forEach((Object str) -> adminIds.add(str + ""));
            } else if (admins instanceof String) {
                adminIds.add(admins + "");
            }
            useAutoBlacklist = (boolean) config.getOrDefault("useAutoBlacklist", true);
            game = (String) config.getOrDefault("game", "");
            continuePlayback = (boolean) config.getOrDefault("continuePlayback", false);

            //Modular audiomanagers
            youtubeAudio = (Boolean) config.getOrDefault("enableYouTube", true);
            soundcloudAudio = (Boolean) config.getOrDefault("enableSoundCloud", true);
            bandcampAudio = (Boolean) config.getOrDefault("enableBandCamp", true);
            twitchAudio = (Boolean) config.getOrDefault("enableTwitch", true);
            vimeoAudio = (Boolean) config.getOrDefault("enableVimeo", true);
            mixerAudio = (Boolean) config.getOrDefault("enableMixer", true);
            spotifyAudio = (Boolean) config.getOrDefault("enableSpotify", true);
            localAudio = (Boolean) config.getOrDefault("enableLocal", false);
            httpAudio = (Boolean) config.getOrDefault("enableHttp", false);


            //Load Credential values
            Object token = config.get("token");
            if (token instanceof String) {
                botToken = (String) token;
            } else {
                Map<String, String> tokens = (Map) token;
                botToken = tokens.getOrDefault(distribution.getId(), "");
            }
            if (botToken == null || botToken.isEmpty()) {
                throw new RuntimeException("No discord bot token provided for the started distribution " + distribution
                        + "\nMake sure to put a " + distribution.getId() + " token in your fredboat.yaml file.");
            }

            Object gkeys = config.get("googleServerKeys");
            if (gkeys instanceof List) {
                ((List) gkeys).forEach((Object str) -> googleKeys.add((String) str));
            } else if (gkeys instanceof String) {
                googleKeys.add((String) gkeys);
            } else {
                log.warn("No google API keys found. Some commands may not work, check the documentation.");
            }

            // apis
            malUser = (String) config.getOrDefault("malUser", "");
            malPassword = (String) config.getOrDefault("malPassword", "");

            imgurClientId = (String) config.getOrDefault("imgurClientId", "");

            spotifyId = (String) config.getOrDefault("spotifyId", "");
            spotifySecret = (String) config.getOrDefault("spotifySecret", "");

            openWeatherKey = (String) config.getOrDefault("openWeatherKey", "");
            sentryDsn = (String) config.getOrDefault("sentryDsn", "");


            // main database
            jdbcUrl = (String) config.getOrDefault("jdbcUrl", "");
            if (jdbcUrl == null || jdbcUrl.isEmpty()) {
                if ("docker".equals(System.getenv("ENV"))) {
                    log.info("No main JDBC URL found, docker environment detected. Using default docker main JDBC url");
                    jdbcUrl = "jdbc:postgresql://db:5432/fredboat?user=fredboat";
                } else {
                    String message = "No main jdbcUrl provided in a non-docker environment. FredBoat cannot work without a database.";
                    log.error(message);
                    throw new RuntimeException(message);
                }
            }
            boolean useSshTunnel = (boolean) config.getOrDefault("useSshTunnel", false);
            if (useSshTunnel) {
                //Parse host:port
                String sshHostRaw = (String) config.getOrDefault("sshHost", "localhost:22");
                String sshHost = sshHostRaw.split(":")[0];
                int sshPort;
                try {
                    sshPort = Integer.parseInt(sshHostRaw.split(":")[1]);
                } catch (Exception e) {
                    sshPort = 22;
                }
                String sshUser = (String) config.getOrDefault("sshUser", "fredboat");
                String sshPrivateKeyFile = (String) config.getOrDefault("sshPrivateKeyFile", "database.ppk");
                String sshKeyPassphrase = (String) config.getOrDefault("sshKeyPassphrase", "");
                int tunnelLocalPort = (int) config.getOrDefault("tunnelLocalPort", 9333);//9333 is a legacy port for backwards compatibility
                String tunnelRemotePortKey = "tunnelRemotePort";
                if (config.containsKey("forwardToPort")) {//legacy check
                    tunnelRemotePortKey = "forwardToPort";
                }
                int tunnelRemotePort = (int) config.getOrDefault(tunnelRemotePortKey, 5432);

                mainSshTunnelConfig = new SshTunnel.SshDetails(sshHost, sshUser)
                        .setKeyFile(sshPrivateKeyFile)
                        .setPassphrase(sshKeyPassphrase == null || sshKeyPassphrase.isEmpty() ? null : sshKeyPassphrase)
                        .setSshPort(sshPort)
                        .setLocalPort(tunnelLocalPort)
                        .setRemotePort(tunnelRemotePort);
            }


            // cache database
            cacheJdbcUrl = (String) config.getOrDefault("cacheJdbcUrl", "");
            if (cacheJdbcUrl == null || cacheJdbcUrl.isEmpty()) {
                if ("docker".equals(System.getenv("ENV"))) {
                    log.info("No cache jdbcUrl found, docker environment detected. Using default docker cache JDBC url");
                    cacheJdbcUrl = "jdbc:postgresql://db:5432/fredboat_cache?user=fredboat";
                } else {
                    log.warn("No cache jdbcUrl provided in a non-docker environment. This may lead to a degraded performance, "
                            + "especially in a high usage environment, or when using Spotify playlists.");
                    cacheJdbcUrl = null;
                }
            }
            if (jdbcUrl.equals(cacheJdbcUrl)) {
                log.warn("The main and cache jdbc urls may not point to the same database due to how flyway handles migrations. "
                        + "The cache database will not be available in this execution of FredBoat. This may lead to a degraded performance, "
                        + "especially in a high usage environment, or when using Spotify playlists.");
                cacheJdbcUrl = null;
            }
            boolean cacheUseSshTunnel = (boolean) config.getOrDefault("cacheUseSshTunnel", false);
            if (cacheUseSshTunnel) {
                //Parse host:port
                String cacheSshHostRaw = (String) config.getOrDefault("cacheSshHost", "localhost:22");
                String cacheSshHost = cacheSshHostRaw.split(":")[0];
                int cacheSshPort;
                try {
                    cacheSshPort = Integer.parseInt(cacheSshHostRaw.split(":")[1]);
                } catch (Exception e) {
                    cacheSshPort = 22;
                }
                String cacheSshUser = (String) config.getOrDefault("cacheSshUser", "fredboat");
                String cacheSshPrivateKeyFile = (String) config.getOrDefault("cacheSshPrivateKeyFile", "database.ppk");
                String cacheSshKeyPassphrase = (String) config.getOrDefault("cacheSshKeyPassphrase", "");
                int cacheTunnelLocalPort = (int) config.getOrDefault("cacheTunnelLocalPort", 5433);
                int cacheTunnelRemotePort = (int) config.getOrDefault("cacheTunnelRemotePort", 5432);

                cacheSshTunnelConfig = new SshTunnel.SshDetails(cacheSshHost, cacheSshUser)
                        .setKeyFile(cacheSshPrivateKeyFile)
                        .setPassphrase(cacheSshKeyPassphrase == null || cacheSshKeyPassphrase.isEmpty() ? null : cacheSshKeyPassphrase)
                        .setSshPort(cacheSshPort)
                        .setLocalPort(cacheTunnelLocalPort)
                        .setRemotePort(cacheTunnelRemotePort);
            }

            // misc
            Object linkNodes = config.get("lavalinkHosts");
            if (linkNodes != null) {
                if (linkNodes instanceof Map) {
                    Map<String, String> simpleNodes = (Map<String, String>) linkNodes;
                    AtomicInteger nodeCounter = new AtomicInteger(0);
                    simpleNodes.forEach((host, pass) -> {
                        try {
                            String name = "Lavalink-Node#" + nodeCounter.getAndIncrement();
                            URI uri = new URI(host);
                            lavalinkHosts.add(new LavalinkConfig.LavalinkHost(name, uri, pass));
                            log.info("Lavalink node added: {} {}", name, uri);
                        } catch (URISyntaxException e) {
                            throw new RuntimeException("Failed parsing lavalink URI", e);
                        }
                    });
                } else {
                    List<Map<String, String>> namedNodes = (List<Map<String, String>>) linkNodes;
                    for (Map<String, String> node : namedNodes) {
                        try {
                            String name = node.get("name");
                            URI uri = new URI(node.get("host"));
                            lavalinkHosts.add(new LavalinkConfig.LavalinkHost(name, uri, node.get("pass")));
                        } catch (URISyntaxException e) {
                            throw new RuntimeException("Failed parsing lavalink URI", e);
                        }
                    }
                }
            }

            eventLogWebhook = (String) config.getOrDefault("eventLogWebhook", "");
            eventLogInterval = (int) config.getOrDefault("eventLogInterval", 1); //minutes
            guildStatsWebhook = (String) config.getOrDefault("guildStatsWebhook", "");
            guildStatsInterval = (int) config.getOrDefault("guildStatsInterval", 60); //minutes


            // Undocumented creds
            carbonKey = (String) config.getOrDefault("carbonKey", "");
            dikeUrl = (String) config.getOrDefault("dikeUrl", "");


            PlayerLimitManager.setLimit((Integer) config.getOrDefault("playerLimit", -1));
        } catch (IOException e) {
            log.error("Failed to read one or more yaml files into strings", e);
            throw new RuntimeException("Failed to read one or more yaml files into strings.", e);
        } catch (YAMLException | ClassCastException e) {
            log.error("Could not parse one or more yaml files! They are probably misformatted. " +
                    "Try using an online yaml validator.", e);
            throw e;
        } catch (Exception e) {
            log.error("Could not init file config", e);
            throw e;
        }
    }

    /**
     * @param name relative name of a config file, without the file extension
     * @return a handle on the requested file
     */
    private static File loadConfigFile(String name) throws IOException {
        String path = "./" + name + ".yaml";
        File file = new File(path);
        if (!file.exists() || file.isDirectory()) {
            throw new FileNotFoundException("Could not find '" + path + "' file.");
        }
        return file;
    }

    private static String cleanTabs(String content, String file) {
        CharMatcher tab = CharMatcher.is('\t');
        if (tab.matchesAnyOf(content)) {
            log.warn("{} contains tab characters! Trying a fix-up.", file);
            return tab.replaceFrom(content, "  ");
        } else {
            return content;
        }
    }


    // ********************************************************************************
    //                           Config Getters
    // ********************************************************************************

    @Override
    public DistributionEnum getDistribution() {
        return distribution;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public boolean isRestServerEnabled() {
        return restServerEnabled;
    }

    @Override
    public List<String> getAdminIds() {
        return adminIds;
    }

    @Override
    public boolean useAutoBlacklist() {
        return useAutoBlacklist;
    }

    @Override
    public String getGame() {
        if (game.isEmpty()) {
            return "Say " + getPrefix() + CommandInitializer.HELP_COMM_NAME;
        } else {
            return game;
        }
    }

    @Override
    public boolean getContinuePlayback() {
        return continuePlayback;
    }

    @Override
    public boolean isYouTubeEnabled() {
        return youtubeAudio;
    }

    @Override
    public boolean isSoundCloudEnabled() {
        return soundcloudAudio;
    }

    @Override
    public boolean isBandCampEnabled() {
        return bandcampAudio;
    }

    @Override
    public boolean isTwitchEnabled() {
        return twitchAudio;
    }

    @Override
    public boolean isVimeoEnabled() {
        return vimeoAudio;
    }

    @Override
    public boolean isMixerEnabled() {
        return mixerAudio;
    }

    @Override
    public boolean isSpotifyEnabled() {
        return spotifyAudio;
    }

    @Override
    public boolean isLocalEnabled() {
        return localAudio;
    }

    @Override
    public boolean isHttpEnabled() {
        return httpAudio;
    }


    // ********************************************************************************
    //                           Credentials Getters
    // ********************************************************************************

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public List<String> getGoogleKeys() {
        return googleKeys;
    }

    @Override
    public String getMalUser() {
        return malUser;
    }

    @Override
    public String getMalPassword() {
        return malPassword;
    }

    @Override
    public String getImgurClientId() {
        return imgurClientId;
    }

    @Override
    public String getSpotifyId() {
        return spotifyId;
    }

    @Override
    public String getSpotifySecret() {
        return spotifySecret;
    }

    @Override
    public String getOpenWeatherKey() {
        return openWeatherKey;
    }

    @Override
    public String getSentryDsn() {
        return sentryDsn;
    }

    @Override
    @Nonnull
    public String getMainJdbcUrl() {
        return jdbcUrl;
    }

    @Override
    @Nullable
    public SshTunnel.SshDetails getMainSshTunnelConfig() {
        return mainSshTunnelConfig;
    }

    @Override
    @Nullable
    //may return null if no cache database was provided.
    public String getCacheJdbcUrl() {
        return cacheJdbcUrl;
    }

    @Override
    @Nullable
    public SshTunnel.SshDetails getCacheSshTunnelConfig() {
        return cacheSshTunnelConfig;
    }

    @Override
    public List<LavalinkConfig.LavalinkHost> getLavalinkHosts() {
        return lavalinkHosts;
    }

    @Override
    public String getEventLogWebhook() {
        return eventLogWebhook;
    }

    //minutes
    @Override
    public int getEventLogInterval() {
        return eventLogInterval;
    }

    @Override
    public String getGuildStatsWebhook() {
        return guildStatsWebhook;
    }

    //minutes
    @Override
    public int getGuildStatsInterval() {
        return guildStatsInterval;
    }


    // ********************************************************************************
    //                       Derived and undocumented values
    // ********************************************************************************

    @Override
    public String getCarbonKey() {
        return carbonKey;
    }

    @Override
    public String getDikeUrl() {
        return dikeUrl;
    }
}
