/*
 * MIT License
 *
 * Copyright (c) 2016 Frederik Ar. Mikkelsen
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
import fredboat.audio.MusicPersistenceHandler;
import fredboat.commandmeta.CommandInitializer;
import fredboat.event.EventListenerBoat;
import fredboat.event.EventListenerSelf;
import fredboat.util.BotConstants;
import fredboat.util.DiscordUtil;
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
import net.dv8tion.jda.core.utils.SimpleLog;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class FredBoat {

    private static final Logger log = LoggerFactory.getLogger(FredBoat.class);

    static final int SHARD_CREATION_SLEEP_INTERVAL = 5100;

    private static final ArrayList<FredBoat> shards = new ArrayList<>();
    public static JCA jca;
    public static final long START_TIME = System.currentTimeMillis();
    public static DistributionEnum distribution = DistributionEnum.DEVELOPMENT;
    public static final int UNKNOWN_SHUTDOWN_CODE = -991023;
    public static int shutdownCode = UNKNOWN_SHUTDOWN_CODE;//Used when specifying the intended code for shutdown hooks
    static EventListenerBoat listenerBot;
    static EventListenerSelf listenerSelf;

    /* Config */
    private static JSONObject config = null;
    private static int scopes = 0;
    public static int numShards = 1;
    private static AtomicInteger numShardsReady = new AtomicInteger(0);

    /* Credentials */
    private static JSONObject credsjson;
    static String accountToken;
    static String clientToken;
    public static String mashapeKey;
    public static String MALPassword;
    private final static List<String> GOOGLE_KEYS = new ArrayList<>();
    private static String[] lavaplayerNodes = new String[64];
    private static boolean lavaplayerNodesEnabled = false;
    private static String carbonKey;

    JDA jda;
    private static FredBoatClient fbClient;

    public static void main(String[] args) throws LoginException, IllegalArgumentException, InterruptedException, IOException {
        Runtime.getRuntime().addShutdownHook(new Thread(ON_SHUTDOWN));

        //Attach log adapter
        SimpleLog.addListener(new SimpleLogToSLF4JAdapter());

        //Make JDA not print to console, we have Logback for that
        SimpleLog.LEVEL = SimpleLog.Level.OFF;

        try {
            scopes = Integer.parseInt(args[0]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
            log.info("Invalid scope, defaulting to scopes 0x111");
            scopes = 0x111;
        }

        log.info("Starting with scopes:"
                + "\n\tMain: " + ((scopes & 0x100) == 0x100)
                + "\n\tMusic: " + ((scopes & 0x010) == 0x010)
                + "\n\tSelf: " + ((scopes & 0x001) == 0x001));

        log.info("JDA version:\t" + JDAInfo.VERSION);

        //Load credentials and config files
        InputStream is = new FileInputStream(new File("./credentials.json"));
        Scanner scanner = new Scanner(is);
        credsjson = new JSONObject(scanner.useDelimiter("\\A").next());
        scanner.close();

        is = new FileInputStream(new File("./config.json"));
        scanner = new Scanner(is);
        config = new JSONObject(scanner.useDelimiter("\\A").next());
        scanner.close();

        mashapeKey = credsjson.optString("mashapeKey");
        clientToken = credsjson.optString("clientToken");
        MALPassword = credsjson.optString("malPassword");
        carbonKey = credsjson.optString("carbonKey");

        JSONArray nodesArray = credsjson.optJSONArray("lavaplayerNodes");
        if(nodesArray != null) {
            lavaplayerNodesEnabled = true;
            Iterator<Object> itr = nodesArray.iterator();
            int i = 0;
            while(itr.hasNext()) {
                lavaplayerNodes[i] = (String) itr.next();
                i++;
            }
        }

        JSONArray gkeys = credsjson.optJSONArray("googleServerKeys");
        if (gkeys != null) {
            log.info("Using lavaplayer nodes");
            gkeys.forEach((Object str) -> GOOGLE_KEYS.add((String) str));
        }

        if (config.optBoolean("patron")) {
            distribution = DistributionEnum.PATRON;
        } else //Determine distribution
        if (config.optBoolean("development")) {
            distribution = DistributionEnum.DEVELOPMENT;
        } else {
            distribution = DiscordUtil.isMainBot() ? DistributionEnum.MAIN : DistributionEnum.MUSIC;
        }
        accountToken = credsjson.getJSONObject("token").getString(distribution.getId());

        log.info("Determined distribution: " + distribution);

        //Initialise event listeners
        listenerBot = new EventListenerBoat(scopes & 0x110, distribution == DistributionEnum.DEVELOPMENT ? BotConstants.DEFAULT_BOT_PREFIX_BETA : BotConstants.DEFAULT_BOT_PREFIX);
        listenerSelf = new EventListenerSelf(scopes & 0x001, distribution == DistributionEnum.DEVELOPMENT ? BotConstants.DEFAULT_SELF_PREFIX_BETA : BotConstants.DEFAULT_SELF_PREFIX);

        /* Init JDA */

        if ((scopes & 0x110) != 0) {
            initBotShards();
        }

        if ((scopes & 0x001) != 0) {
            fbClient = new FredBoatClient();
        }

        try {
            API.start();
        } catch (Exception e) {
            log.info("Failed to ignite Spark, FredBoat API unavailable", e);
        }

        //Initialise JCA
        String cbUser = credsjson.optString("cbUser");
        String cbKey = credsjson.optString("cbKey");
        if(cbUser != null && cbKey != null && !cbUser.equals("") && !cbKey.equals("")) {
            log.info("Starting CleverBot");
            jca = new JCABuilder().setKey(cbKey).setUser(cbUser).buildBlocking();
        } else {
            log.warn("Credentials not found for cleverbot authentication. Skipping...");
        }

        if (distribution == DistributionEnum.MAIN && carbonKey != null) {
            CarbonitexAgent carbonitexAgent = new CarbonitexAgent(carbonKey);
            carbonitexAgent.setDaemon(true);
            carbonitexAgent.start();
        }
    }

    private static void initBotShards() {
        if(distribution == DistributionEnum.DEVELOPMENT) {
            log.info("Development distribution; forcing 2 shards");
            numShards = 2;
        } else {
            try {
                numShards = DiscordUtil.getRecommendedShardCount(accountToken);
            } catch (UnirestException e) {
                throw new RuntimeException("Unable to get recommended shard count!", e);
            }

            log.info("Discord recommends " + numShards + " shard(s)");
        }

        for(int i = 0; i < numShards; i++){
            shards.add(i, new FredBoatBot(i));
            try {
                Thread.sleep(SHARD_CREATION_SLEEP_INTERVAL);
            } catch (InterruptedException e) {
                throw new RuntimeException("Got interrupted while setting up bot shards!", e);
            }
        }

        log.info(numShards + " shards have been constructed");

    }

    public static void onInit(ReadyEvent readyEvent) {
        int ready = numShardsReady.incrementAndGet();
        log.info("Received ready event for " + FredBoat.getInstance(readyEvent.getJDA()).getShardInfo().getShardString());

        //Commands
        CommandInitializer.initCommands();

        if(ready == numShards) {
            log.info("All " + ready + " shards are ready.");
            MusicPersistenceHandler.reloadPlaylists();
        }
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

    public static int getScopes() {
        return scopes;
    }

    public static List<String> getGoogleKeys() {
        return GOOGLE_KEYS;
    }

    public static String getRandomGoogleKey() {
        return GOOGLE_KEYS.get((int) Math.floor(Math.random() * GOOGLE_KEYS.size()));
    }

    public static EventListenerBoat getListenerBot() {
        return listenerBot;
    }

    public static EventListenerSelf getListenerSelf() {
        return listenerSelf;
    }

    public static String[] getLavaplayerNodes() {
        return lavaplayerNodes;
    }

    public static boolean isLavaplayerNodesEnabled() {
        return lavaplayerNodesEnabled;
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

    public ShardInfo getShardInfo() {
        int sId = jda.getShardInfo() == null ? 0 : jda.getShardInfo().getShardId();

        if(jda.getAccountType() == AccountType.CLIENT) {
            return new ShardInfo(0, 1);
        } else {
            return new ShardInfo(sId, numShards);
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
            return "[" + this.shardId + " / " + this.shardTotal + "]";
        }

    }
}
