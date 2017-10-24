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
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);

    public static String DEFAULT_PREFIX = ";;";
    //see https://github.com/brettwooldridge/HikariCP connectionTimeout
    public static int HIKARI_TIMEOUT_MILLISECONDS = 1000;

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

    private final DistributionEnum distribution;
    private final String botToken;
    private final String jdbcUrl;
    private final int hikariPoolSize;
    private int numShards;
    private String malUser;
    private String malPassword;
    private String imgurClientId;
    private List<String> googleKeys = new ArrayList<>();
    private final String[] lavaplayerNodes;
    private final boolean lavaplayerNodesEnabled;
    private String carbonKey;
    private String spotifyId;
    private String spotifySecret;
    private String prefix = DEFAULT_PREFIX;
    private boolean restServerEnabled = true;
    private List<String> adminIds = new ArrayList<>();
    private boolean useAutoBlacklist = false;
    private String game = "";
    private boolean continuePlayback = false;
    private List<LavalinkHost> lavalinkHosts = new ArrayList<>();
    private String openWeatherKey;
    private String sentryDsn;
    private long eventLogWebhookId;
    private String eventLogWebhookToken;

    //testing related stuff
    private String testBotToken;
    private String testChannelId;

    // SSH tunnel stuff
    private final boolean useSshTunnel;
    private final String sshHost; //Eg localhost:22
    private final String sshUser; //Eg fredboat
    private final String sshPrivateKeyFile;
    private final int forwardToPort; //port where the remote database is listening, postgres default: 5432

    //AudioManager Stuff
    private Boolean youtubeAudio;
    private Boolean soundcloudAudio;
    private Boolean bandcampAudio;
    private Boolean twitchAudio;
    private Boolean vimeoAudio;
    private Boolean mixerAudio;
    private Boolean spotifyAudio;
    private Boolean httpAudio;

    //Temp configs
    private boolean useVoiceChannelCleanup = true;

    @SuppressWarnings("unchecked")
    public Config(File credentialsFile, File configFile) {
        try {
            Yaml yaml = new Yaml();
            String credsFileStr = FileUtils.readFileToString(credentialsFile, "UTF-8");
            String configFileStr = FileUtils.readFileToString(configFile, "UTF-8");
            //remove those pesky tab characters so a potential json file is YAML conform
            credsFileStr = cleanTabs(credsFileStr, "credentials.yaml");
            configFileStr = cleanTabs(configFileStr, "config.yaml");
            Map<String, Object> creds;
            Map<String, Object> config;

            creds = (Map<String, Object>) yaml.load(credsFileStr);
            config = (Map<String, Object>) yaml.load(configFileStr);

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

            // Determine distribution
            if ((boolean) config.getOrDefault("patron", false)) {
                distribution = DistributionEnum.PATRON;
            } else if ((boolean) config.getOrDefault("development", false)) {//Determine distribution
                distribution = DistributionEnum.DEVELOPMENT;
            } else {
                distribution = DistributionEnum.MUSIC;
            }

            log.info("Determined distribution: " + distribution);

            prefix = (String) config.getOrDefault("prefix", prefix);
            restServerEnabled = (boolean) config.getOrDefault("restServerEnabled", restServerEnabled);

            Object admins = config.get("admins");
            if (admins instanceof List) {
                ((List) admins).forEach((Object str) -> adminIds.add(str + ""));
            } else if (admins instanceof String) {
                adminIds.add(admins + "");
            }
            useAutoBlacklist = (boolean) config.getOrDefault("useAutoBlacklist", useAutoBlacklist);
            game = (String) config.getOrDefault("game", "");
            continuePlayback = (boolean) config.getOrDefault("continuePlayback", continuePlayback);

            log.info("Using prefix: " + prefix);

            malUser = (String) creds.getOrDefault("malUser", "");
            malPassword = (String) creds.getOrDefault("malPassword", "");
            carbonKey = (String) creds.getOrDefault("carbonKey", "");
            Map<String, String> token = (Map) creds.get("token");
            if (token != null) {
                botToken = token.getOrDefault(distribution.getId(), "");
            } else botToken = "";
            if (botToken == null || botToken.isEmpty()) {
                throw new RuntimeException("No discord bot token provided for the started distribution " + distribution
                        + "\nMake sure to put a " + distribution.getId() + " token in your credentials file.");
            }

            spotifyId = (String) creds.getOrDefault("spotifyId", "");
            spotifySecret = (String) creds.getOrDefault("spotifySecret", "");

            jdbcUrl = (String) creds.getOrDefault("jdbcUrl", "");

            openWeatherKey = (String) creds.getOrDefault("openWeatherKey", "");

            eventLogWebhookId = (long) creds.getOrDefault("eventLogWebhookId", 0L);
            eventLogWebhookToken = (String) creds.getOrDefault("eventLogWebhookToken", "");

            Object gkeys = creds.get("googleServerKeys");
            if (gkeys instanceof List) {
                ((List) gkeys).forEach((Object str) -> googleKeys.add((String) str));
            } else if (gkeys instanceof String) {
                googleKeys.add((String) gkeys);
            } else {
                log.warn("No google API keys found. Some commands may not work, check the documentation.");
            }

            List<String> nodesArray = (List) creds.get("lavaplayerNodes");
            if (nodesArray != null) {
                lavaplayerNodesEnabled = true;
                log.info("Using lavaplayer nodes");
                lavaplayerNodes = nodesArray.toArray(new String[nodesArray.size()]);
            } else {
                lavaplayerNodesEnabled = false;
                lavaplayerNodes = new String[0];
                //log.info("Not using lavaplayer nodes. Audio playback will be processed locally.");
            }

            Map<String, String> linkNodes = (Map<String, String>) creds.get("lavalinkHosts");
            if (linkNodes != null) {
                linkNodes.forEach((s, s2) -> {
                    try {
                        lavalinkHosts.add(new LavalinkHost(new URI(s), s2));
                        log.info("Lavalink node added: " + new URI(s));
                    } catch (URISyntaxException e) {
                        throw new RuntimeException("Failed parsing URI", e);
                    }
                });
            }

            //this is the first request on start
            //it sometimes fails cause network isn't set up yet. wait 10 sec and try one more time in that case
            try {
                numShards = DiscordUtil.getRecommendedShardCount(getBotToken());
            } catch (Exception e) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                    //duh
                }
                numShards = DiscordUtil.getRecommendedShardCount(getBotToken());
            }
            log.info("Discord recommends " + numShards + " shard(s)");

            //more database connections don't help with performance, so use a value based on available cores
            //http://www.dailymotion.com/video/x2s8uec_oltp-performance-concurrent-mid-tier-connections_tech
            if (jdbcUrl == null || "".equals(jdbcUrl) || distribution == DistributionEnum.DEVELOPMENT)
                //more than one connection for the fallback sqlite db is problematic as there is currently (2017-04-16)
                // no supported way in the custom driver and/or dialect to set lock timeouts
                hikariPoolSize = 1;
            else hikariPoolSize = Runtime.getRuntime().availableProcessors() * 2;
            log.info("Hikari max pool size set to " + hikariPoolSize);

            imgurClientId = (String) creds.getOrDefault("imgurClientId", "");

            testBotToken = (String) creds.getOrDefault("testToken", "");
            testChannelId = creds.getOrDefault("testChannelId", "") + "";

            PlayerLimitManager.setLimit((Integer) config.getOrDefault("playerLimit", -1));

            useSshTunnel = (boolean) creds.getOrDefault("useSshTunnel", false);
            sshHost = (String) creds.getOrDefault("sshHost", "localhost:22");
            sshUser = (String) creds.getOrDefault("sshUser", "fredboat");
            sshPrivateKeyFile = (String) creds.getOrDefault("sshPrivateKeyFile", "database.ppk");
            forwardToPort = (int) creds.getOrDefault("forwardToPort", 5432);

            //Modularise audiomanagers; load from "config.yaml"

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

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (YAMLException | ClassCastException e) {
            log.error("Could not parse the credentials and/or config yaml files! They are probably misformatted. " +
                    "Try using an online yaml validator.");
            throw e;
        }
    }

    /**
     * Makes sure the requested config file exists in the current format. Will attempt to migrate old formats to new ones
     * old files will be renamed to filename.ext.old to preserve any data
     *
     * @param name relative name of a config file, without the file extension
     * @return a handle on the requested file
     */
    private static File loadConfigFile(String name) throws IOException {
        String yamlPath = "./" + name + ".yaml";
        String jsonPath = "./" + name + ".json";
        File yamlFile = new File(yamlPath);
        if (!yamlFile.exists() || yamlFile.isDirectory()) {
            log.warn("Could not find file '" + yamlPath + "', looking for legacy '" + jsonPath + "' to rewrite");
            File json = new File(jsonPath);
            if (!json.exists() || json.isDirectory()) {
                //file is missing
                log.error("No " + name + " file is present. Bot cannot run without it. Check the documentation.");
                throw new FileNotFoundException("Neither '" + yamlPath + "' nor '" + jsonPath + "' present");
            } else {
                //rewrite the json to yaml
                Yaml yaml = new Yaml();
                String fileStr = FileUtils.readFileToString(json, "UTF-8");
                //remove tab character from json file to make it a valid YAML file
                fileStr = cleanTabs(fileStr, name);
                @SuppressWarnings("unchecked")
                Map<String, Object> configFile = (Map) yaml.load(fileStr);
                yaml.dump(configFile, new FileWriter(yamlFile));
                Files.move(Paths.get(jsonPath), Paths.get(jsonPath + ".old"), REPLACE_EXISTING);
                log.info("Migrated file '" + jsonPath + "' to '" + yamlPath + "'");
            }
        }

        return yamlFile;
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

    public String getRandomGoogleKey() {
        if (googleKeys.isEmpty()) {
            throw new MessagingException("No Youtube API key detected. Please read the documentation of the credentials file on how to obtain one.");
        }
        return googleKeys.get((int) Math.floor(Math.random() * getGoogleKeys().size()));
    }

    public DistributionEnum getDistribution() {
        return distribution;
    }

    public boolean isPatronDistribution() {
        return distribution == DistributionEnum.PATRON;
    }

    public boolean isDevDistribution() {
        return distribution == DistributionEnum.DEVELOPMENT;
    }

    public String getBotToken() {
        return botToken;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public int getHikariPoolSize() {
        return hikariPoolSize;
    }

    public int getNumShards() {
        return numShards;
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

    public List<String> getGoogleKeys() {
        return googleKeys;
    }

    public String[] getLavaplayerNodes() {
        return lavaplayerNodes;
    }

    public boolean isLavaplayerNodesEnabled() {
        return lavaplayerNodesEnabled;
    }

    public String getCarbonKey() {
        return carbonKey;
    }

    public String getSpotifyId() {
        return spotifyId;
    }

    public String getSpotifySecret() {
        return spotifySecret;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getOpenWeatherKey() {
        return openWeatherKey;
    }

    public long getEventLogWebhookId() {
        return eventLogWebhookId;
    }

    public String getEventLogWebhookToken() {
        return eventLogWebhookToken;
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

    public boolean getContinuePlayback() { return continuePlayback; }

    public String getTestBotToken() {
        return testBotToken;
    }

    public String getTestChannelId() {
        return testChannelId;
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

    public class LavalinkHost {

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

    public boolean useVoiceChannelCleanup() {return useVoiceChannelCleanup; }
}
