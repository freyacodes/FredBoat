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

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import fredboat.agent.CarbonitexAgent;
import fredboat.api.API;
import fredboat.api.OAuthManager;
import fredboat.audio.MusicPersistenceHandler;
import fredboat.commandmeta.CommandInitializer;
import fredboat.db.DatabaseManager;
import fredboat.event.EventListenerBoat;
import fredboat.event.EventListenerSelf;
import fredboat.feature.I18n;
import fredboat.util.DistributionEnum;
import fredboat.util.log.SimpleLogToSLF4JAdapter;
import frederikam.jca.JCA;
import frederikam.jca.JCABuilder;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.utils.SimpleLog;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public abstract class FredBoat {

    private static final Logger log = LoggerFactory.getLogger(FredBoat.class);

    static final int SHARD_CREATION_SLEEP_INTERVAL = 5100;

    private static final ArrayList<FredBoat> shards = new ArrayList<>();
    public static JCA jca;
    public static final long START_TIME = System.currentTimeMillis();
    public static final int UNKNOWN_SHUTDOWN_CODE = -991023;
    public static int shutdownCode = UNKNOWN_SHUTDOWN_CODE;//Used when specifying the intended code for shutdown hooks
    static EventListenerBoat listenerBot;
    static EventListenerSelf listenerSelf;
    private static AtomicBoolean firstReadyEventReceived = new AtomicBoolean(false);
    private static AtomicInteger numShardsReady = new AtomicInteger(0);

    JDA jda;
    private static FredBoatClient fbClient;

    public static void main(String[] args) throws LoginException, IllegalArgumentException, InterruptedException, IOException, UnirestException {
        Runtime.getRuntime().addShutdownHook(new Thread(ON_SHUTDOWN));

        log.info("\n\n" +
                "  ______            _ ____              _   \n" +
                " |  ____|          | |  _ \\            | |  \n" +
                " | |__ _ __ ___  __| | |_) | ___   __ _| |_ \n" +
                " |  __| '__/ _ \\/ _` |  _ < / _ \\ / _` | __|\n" +
                " | |  | | |  __/ (_| | |_) | (_) | (_| | |_ \n" +
                " |_|  |_|  \\___|\\__,_|____/ \\___/ \\__,_|\\__|\n\n");

        I18n.start();

        //Attach log adapter
        SimpleLog.addListener(new SimpleLogToSLF4JAdapter());

        //Make JDA not print to console, we have Logback for that
        SimpleLog.LEVEL = SimpleLog.Level.OFF;

        int scope;
        try {
            scope = Integer.parseInt(args[0]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
            log.info("Invalid scope, defaulting to scopes 0x111");
            scope = 0x111;
        }

        log.info("Starting with scopes:"
                + "\n\tMain: " + ((scope & 0x100) == 0x100)
                + "\n\tMusic: " + ((scope & 0x010) == 0x010)
                + "\n\tSelf: " + ((scope & 0x001) == 0x001));

        log.info("JDA version:\t" + JDAInfo.VERSION);

        Config.CONFIG = new Config(
                loadConfigFile("credentials"),
                loadConfigFile("config"),
                scope
        );


        try {
            API.start();
        } catch (Exception e) {
            log.info("Failed to ignite Spark, FredBoat API unavailable", e);
        }
        try {
            if(!Config.CONFIG.getJdbcUrl().equals("") && !Config.CONFIG.getOauthSecret().equals("")) {
                DatabaseManager.startup(Config.CONFIG.getJdbcUrl());
                OAuthManager.start(Config.CONFIG.getBotToken(), Config.CONFIG.getOauthSecret());
            } else {
                log.warn("No JDBC URL and/or secret found, skipped database connection and OAuth2 client");
            }
        } catch (Exception e) {
            log.info("Failed to start DatabaseManager and OAuth2 client", e);
        }

        //Initialise event listeners
        listenerBot = new EventListenerBoat(Config.CONFIG.getScope() & 0x110);
        listenerSelf = new EventListenerSelf(Config.CONFIG.getScope() & 0x001);

        /* Init JDA */

        if ((Config.CONFIG.getScope() & 0x110) != 0) {
            initBotShards(listenerBot);
        }

        if ((Config.CONFIG.getScope() & 0x001) != 0) {
            fbClient = new FredBoatClient();
        }

        //Initialise JCA

        if(!Config.CONFIG.getCbUser().equals("") && !Config.CONFIG.getCbKey().equals("")) {
            log.info("Starting CleverBot");
            jca = new JCABuilder().setKey(Config.CONFIG.getCbKey()).setUser(Config.CONFIG.getCbUser()).buildBlocking();
        } else {
            log.warn("Credentials not found for cleverbot authentication. Skipping...");
        }

        if (Config.CONFIG.getDistribution() == DistributionEnum.MAIN && Config.CONFIG.getCarbonKey() != null) {
            CarbonitexAgent carbonitexAgent = new CarbonitexAgent(Config.CONFIG.getCarbonKey());
            carbonitexAgent.setDaemon(true);
            carbonitexAgent.start();
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
                fileStr = fileStr.replaceAll("\t", "");
                @SuppressWarnings("unchecked")
                Map<String, Object> configFile = (Map) yaml.load(fileStr);
                yaml.dump(configFile, new FileWriter(yamlFile));
                Files.move(Paths.get(jsonPath), Paths.get(jsonPath + ".old"), REPLACE_EXISTING);
                log.info("Migrated file '" + jsonPath + "' to '" + yamlPath + "'");
            }
        }

        return yamlFile;
    }

    private static void initBotShards(EventListener listener) {
        for(int i = 0; i < Config.CONFIG.getNumShards(); i++){
            try {
                shards.add(i, new FredBoatBot(i, listener));
            } catch (Exception e) {
                log.error("Caught an exception while starting shard " + i + "!", e);
                numShardsReady.getAndIncrement();
            }
            try {
                Thread.sleep(SHARD_CREATION_SLEEP_INTERVAL);
            } catch (InterruptedException e) {
                throw new RuntimeException("Got interrupted while setting up bot shards!", e);
            }
        }

        log.info(shards.size() + " shards have been constructed");

    }

    public static void onInit(ReadyEvent readyEvent) {
        int ready = numShardsReady.incrementAndGet();
        log.info("Received ready event for " + FredBoat.getInstance(readyEvent.getJDA()).getShardInfo().getShardString());

        if(!firstReadyEventReceived.getAndSet(true)){
            onInitFirstShard(readyEvent);
        }

        if(ready == Config.CONFIG.getNumShards()) {
            log.info("All " + ready + " shards are ready.");
            MusicPersistenceHandler.reloadPlaylists();
        }
    }

    private static void onInitFirstShard(ReadyEvent readyEvent) {
        //Commands
        CommandInitializer.initCommands();
    }

    //Shutdown hook
    private static final Runnable ON_SHUTDOWN = () -> {
        int code = shutdownCode != UNKNOWN_SHUTDOWN_CODE ? shutdownCode : -1;

        try {
            MusicPersistenceHandler.handlePreShutdown(code);
        } catch (Exception e) {
            log.error("Critical error while handling music persistence.", e);
        }

        for(FredBoat fb : shards) {
            fb.getJda().shutdown(false);
        }

        try {
            Unirest.shutdown();
        } catch (IOException ignored) {}
    };

    public static void shutdown(int code) {
        log.info("Shutting down with exit code " + code);
        shutdownCode = code;

        System.exit(code);
    }

    public static EventListenerBoat getListenerBot() {
        return listenerBot;
    }

    public static EventListenerSelf getListenerSelf() {
        return listenerSelf;
    }

    /* Sharding */

    public JDA getJda() {
        return jda;
    }

    public static List<FredBoat> getShards() {
        return shards;
    }

    public static List<Guild> getAllGuilds() {
        ArrayList<Guild> list = new ArrayList<>();

        for (FredBoat fb : shards) {
            list.addAll(fb.getJda().getGuilds());
        }

        return list;
    }

    public static Map<String, User> getAllUsersAsMap() {
        HashMap<String, User> map = new HashMap<>();

        for (FredBoat fb : shards) {
            for (User usr : fb.getJda().getUsers()) {
                map.put(usr.getId(), usr);
            }
        }

        return map;
    }

    public static TextChannel getTextChannelById(String id) {
        for (FredBoat fb : shards) {
            for (TextChannel channel : fb.getJda().getTextChannels()) {
                if(channel.getId().equals(id)) return channel;
            }
        }

        return null;
    }

    public static VoiceChannel getVoiceChannelById(String id) {
        for (FredBoat fb : shards) {
            for (VoiceChannel channel : fb.getJda().getVoiceChannels()) {
                if(channel.getId().equals(id)) return channel;
            }
        }

        return null;
    }

    public static FredBoatClient getClient() {
        return fbClient;
    }

    public static FredBoat getInstance(JDA jda) {
        if(jda.getAccountType() == AccountType.CLIENT) {
            return fbClient;
        } else {
            int sId = jda.getShardInfo() == null ? 0 : jda.getShardInfo().getShardId();

            for(FredBoat fb : shards) {
                if(((FredBoatBot) fb).getShardId() == sId) {
                    return fb;
                }
            }
        }

        throw new IllegalStateException("Attempted to get instance for JDA shard that is not indexed");
    }

    public static JDA getFirstJDA(){
        return shards.get(0).getJda();
    }

    public ShardInfo getShardInfo() {
        int sId = jda.getShardInfo() == null ? 0 : jda.getShardInfo().getShardId();

        if(jda.getAccountType() == AccountType.CLIENT) {
            return new ShardInfo(0, 1);
        } else {
            return new ShardInfo(sId, Config.CONFIG.getNumShards());
        }
    }

    public class ShardInfo {

        int shardId;
        int shardTotal;

        ShardInfo(int shardId, int shardTotal) {
            this.shardId = shardId;
            this.shardTotal = shardTotal;
        }

        public int getShardId() {
            return this.shardId;
        }

        public int getShardTotal() {
            return this.shardTotal;
        }

        public String getShardString() {
            return String.format("[%02d / %02d]", this.shardId, this.shardTotal);
        }

    }
}
