/*
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
 *
 */

package fredboat;

import com.google.common.base.CharMatcher;
import fredboat.audio.player.PlayerLimitManager;
import fredboat.command.admin.SentryDsnCommand;
import fredboat.commandmeta.MessagingException;
import fredboat.shared.constant.DistributionEnum;
import fredboat.util.DiscordUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);

    public static String DEFAULT_PREFIX = ";;";

    public static final Config CONFIG;

    static {
        Config c;
        try {
            c = new Config(
                    loadConfigFile("credentials"),
                    loadConfigFile("config")
            );
        } catch (final IOException e) {
            c = null;
            log.error("Could not load config files!", e);
        }
        CONFIG = c;
    }

    //Config

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
    private boolean httpAudio;

    // temporary config values todo remove after merging main + music
    private boolean useVoiceChannelCleanup;


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

    // database + SSH tunnel
    private String jdbcUrl;
    private boolean useSshTunnel;
    private String sshHost; //Eg localhost:22
    private String sshUser; //Eg fredboat
    private String sshPrivateKeyFile;
    private int forwardToPort; //port where the remote database is listening, postgres default: 5432

    // misc
    private String sentryDsn;
    private List<LavalinkHost> lavalinkHosts = new ArrayList<>();
    private String eventLogWebhook;
    private String testBotToken;
    private String testChannelId;


    // unofficial creds
    private String carbonKey;


    //Derived Config values
    private int hikariPoolSize;
    private int numShards;

    @SuppressWarnings("unchecked")
    public Config(File credentialsFile, File configFile) {
        try {
            Yaml yaml = new Yaml();
            String credsFileStr = FileUtils.readFileToString(credentialsFile, "UTF-8");
            String configFileStr = FileUtils.readFileToString(configFile, "UTF-8");
            //remove those pesky tab characters so a potential json file is YAML conform
            credsFileStr = cleanTabs(credsFileStr, "credentials.yaml");
            configFileStr = cleanTabs(configFileStr, "config.yaml");

            Map<String, Object> creds = yaml.load(credsFileStr);
            Map<String, Object> config = yaml.load(configFileStr);

            //avoid null values, rather change them to empty strings
            creds.keySet().forEach((String key) -> creds.putIfAbsent(key, ""));
            config.keySet().forEach((String key) -> config.putIfAbsent(key, ""));

            //create the sentry appender as early as possible
            sentryDsn = (String) creds.getOrDefault("sentryDsn", "");
            if (!sentryDsn.isEmpty()) {
                SentryDsnCommand.turnOn(sentryDsn);
            } else {
                SentryDsnCommand.turnOff();
            }


            //Load Config values

            // Determine distribution
            if ((boolean) config.getOrDefault("patron", false)) {
                distribution = DistributionEnum.PATRON;
            } else if ((boolean) config.getOrDefault("development", false)) {
                distribution = DistributionEnum.DEVELOPMENT;
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
            httpAudio = (Boolean) config.getOrDefault("enableHttp", false);

            //temp configs
            useVoiceChannelCleanup = (boolean) config.getOrDefault("tempUseVoiceChannelCleanup", true);


            //Load Credential values

            Map<String, String> token = (Map) creds.get("token");
            if (token != null) {
                botToken = token.getOrDefault(distribution.getId(), "");
            } else botToken = "";
            if (botToken == null || botToken.isEmpty()) {
                throw new RuntimeException("No discord bot token provided for the started distribution " + distribution
                        + "\nMake sure to put a " + distribution.getId() + " token in your credentials file.");
            }

            Object gkeys = creds.get("googleServerKeys");
            if (gkeys instanceof List) {
                ((List) gkeys).forEach((Object str) -> googleKeys.add((String) str));
            } else if (gkeys instanceof String) {
                googleKeys.add((String) gkeys);
            } else {
                log.warn("No google API keys found. Some commands may not work, check the documentation.");
            }

            // apis
            malUser = (String) creds.getOrDefault("malUser", "");
            malPassword = (String) creds.getOrDefault("malPassword", "");

            imgurClientId = (String) creds.getOrDefault("imgurClientId", "");

            spotifyId = (String) creds.getOrDefault("spotifyId", "");
            spotifySecret = (String) creds.getOrDefault("spotifySecret", "");

            openWeatherKey = (String) creds.getOrDefault("openWeatherKey", "");


            // database
            jdbcUrl = (String) creds.getOrDefault("jdbcUrl", "");
            useSshTunnel = (boolean) creds.getOrDefault("useSshTunnel", false);
            sshHost = (String) creds.getOrDefault("sshHost", "localhost:22");
            sshUser = (String) creds.getOrDefault("sshUser", "fredboat");
            sshPrivateKeyFile = (String) creds.getOrDefault("sshPrivateKeyFile", "database.ppk");
            forwardToPort = (int) creds.getOrDefault("forwardToPort", 5432);


            // misc
            Map<String, String> linkNodes = (Map<String, String>) creds.get("lavalinkHosts");
            if (linkNodes != null) {
                linkNodes.forEach((s, s2) -> {
                    try {
                        lavalinkHosts.add(new LavalinkHost(new URI(s), s2));
                        log.info("Lavalink node added: " + new URI(s));
                    } catch (URISyntaxException e) {
                        throw new RuntimeException("Failed parsing lavalink URI", e);
                    }
                });
            }

            eventLogWebhook = (String) creds.getOrDefault("eventLogWebhook", "");

            testBotToken = (String) creds.getOrDefault("testToken", "");
            testChannelId = creds.getOrDefault("testChannelId", "") + "";


            // unofficial creds
            carbonKey = (String) creds.getOrDefault("carbonKey", "");


            // Derived Config values
            // these are calculated in some way and don't necessarily correspond to values from the config/credentials files

            //this is the first request on start
            //it sometimes fails cause network isn't set up yet. wait 10 sec and try one more time in that case
            int recommendedShardCount;
            try {
                recommendedShardCount = DiscordUtil.getRecommendedShardCount(getBotToken());
            } catch (Exception e) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ignored) {
                }
                recommendedShardCount = DiscordUtil.getRecommendedShardCount(getBotToken());
            }
            numShards = recommendedShardCount;
            log.info("Discord recommends " + numShards + " shard(s)");

            //more database connections don't help with performance, so use a value based on available cores, but not too low
            //http://www.dailymotion.com/video/x2s8uec_oltp-performance-concurrent-mid-tier-connections_tech
            hikariPoolSize = Math.max(4, Runtime.getRuntime().availableProcessors());
            log.info("Hikari max pool size set to " + hikariPoolSize);

            PlayerLimitManager.setLimit((Integer) config.getOrDefault("playerLimit", -1));
        } catch (IOException e) {
            log.error("Failed to read config and or credentials files into strings.", e);
            throw new RuntimeException(e);
        } catch (YAMLException | ClassCastException e) {
            log.error("Could not parse the credentials and/or config yaml files! They are probably misformatted. " +
                    "Try using an online yaml validator.", e);
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


    public static class LavalinkHost {

        private final URI uri;
        private final String password;

        public LavalinkHost(URI uri, String password) {
            this.uri = uri;
            this.password = password;
        }

        public URI getUri() {
            return uri;
        }

        public String getPassword() {
            return password;
        }
    }


    // ********************************************************************************
    //                           Config Getters
    // ********************************************************************************

    public DistributionEnum getDistribution() {
        return distribution;
    }

    public boolean isPatronDistribution() {
        return distribution == DistributionEnum.PATRON;
    }

    public boolean isDevDistribution() {
        return distribution == DistributionEnum.DEVELOPMENT;
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean isRestServerEnabled() {
        return restServerEnabled;
    }

    public List<String> getAdminIds() {
        return adminIds;
    }

    public boolean useAutoBlacklist() {
        return useAutoBlacklist;
    }

    public String getGame() {
        if (game == null || game.isEmpty()) {
            return "Say " + getPrefix() + "help";
        } else {
            return game;
        }
    }

    public boolean getContinuePlayback() {
        return continuePlayback;
    }

    public boolean isYouTubeEnabled() {
        return youtubeAudio;
    }

    public boolean isSoundCloudEnabled() {
        return soundcloudAudio;
    }

    public boolean isBandCampEnabled() {
        return bandcampAudio;
    }

    public boolean isTwitchEnabled() {
        return twitchAudio;
    }

    public boolean isVimeoEnabled() {
        return vimeoAudio;
    }

    public boolean isMixerEnabled() {
        return mixerAudio;
    }

    public boolean isSpotifyEnabled() {
        return spotifyAudio;
    }

    public boolean isHttpEnabled() {
        return httpAudio;
    }


    public boolean useVoiceChannelCleanup() {
        return useVoiceChannelCleanup;
    }

    // ********************************************************************************
    //                           Credentials Getters
    // ********************************************************************************

    public String getBotToken() {
        return botToken;
    }

    public List<String> getGoogleKeys() {
        return googleKeys;
    }

    public String getRandomGoogleKey() {
        if (googleKeys.isEmpty()) {
            throw new MessagingException("No Youtube API key detected. Please read the documentation of the credentials file on how to obtain one.");
        }
        return googleKeys.get((int) Math.floor(Math.random() * getGoogleKeys().size()));
    }

    public String getMalUser() {
        return malUser;
    }

    public String getMalPassword() {
        return malPassword;
    }

    public String getImgurClientId() {
        return imgurClientId;
    }

    public String getSpotifyId() {
        return spotifyId;
    }

    public String getSpotifySecret() {
        return spotifySecret;
    }

    public String getOpenWeatherKey() {
        return openWeatherKey;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public boolean isUseSshTunnel() {
        return useSshTunnel;
    }

    public String getSshHost() {
        return sshHost;
    }

    public String getSshUser() {
        return sshUser;
    }

    public String getSshPrivateKeyFile() {
        return sshPrivateKeyFile;
    }

    public int getForwardToPort() {
        return forwardToPort;
    }

    public List<LavalinkHost> getLavalinkHosts() {
        return lavalinkHosts;
    }

    public String getEventLogWebhook() {
        return eventLogWebhook;
    }

    public String getTestBotToken() {
        return testBotToken;
    }

    public String getTestChannelId() {
        return testChannelId;
    }


    // ********************************************************************************
    //                       Derived and unofficial values
    // ********************************************************************************

    //this static method works even when called from tests with invalid config files leading to a null config
    public static int getNumShards() {
        if (CONFIG != null) {
            return CONFIG.numShards;
        } else {
            return 1;
        }
    }

    public int getHikariPoolSize() {
        return hikariPoolSize;
    }

    public String getCarbonKey() {
        return carbonKey;
    }
}
