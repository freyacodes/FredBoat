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

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import fredboat.agent.CarbonitexAgent;
import fredboat.agent.DBConnectionWatchdogAgent;
import fredboat.agent.FredBoatAgent;
import fredboat.agent.ShardWatchdogAgent;
import fredboat.api.API;
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.LavalinkManager;
import fredboat.audio.player.PlayerRegistry;
import fredboat.audio.queue.MusicPersistenceHandler;
import fredboat.commandmeta.CommandRegistry;
import fredboat.commandmeta.init.MainCommandInitializer;
import fredboat.commandmeta.init.MusicCommandInitializer;
import fredboat.db.DatabaseManager;
import fredboat.event.EventListenerBoat;
import fredboat.event.ShardWatchdogListener;
import fredboat.feature.I18n;
import fredboat.shared.constant.DistributionEnum;
import fredboat.util.AppInfo;
import fredboat.util.GitRepoState;
import fredboat.util.JDAUtil;
import fredboat.util.rest.Http;
import fredboat.util.rest.OpenWeatherAPI;
import fredboat.util.rest.models.weather.RetrievedWeather;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.managers.AudioManager;
import okhttp3.Credentials;
import okhttp3.Response;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class FredBoat {

    private static final Logger log = LoggerFactory.getLogger(FredBoat.class);

    static final int SHARD_CREATION_SLEEP_INTERVAL = 5500;

    private static final List<FredBoat> shards = new CopyOnWriteArrayList<>();
    public static final long START_TIME = System.currentTimeMillis();
    public static final int UNKNOWN_SHUTDOWN_CODE = -991023;
    public static int shutdownCode = UNKNOWN_SHUTDOWN_CODE;//Used when specifying the intended code for shutdown hooks
    static EventListenerBoat listenerBot;
    ShardWatchdogListener shardWatchdogListener = null;
    private static AtomicInteger numShardsReady = new AtomicInteger(0);

    //For when we need to join a revived shard with it's old GuildPlayers
    final ArrayList<String> channelsToRejoin = new ArrayList<>();

    //unlimited threads = http://i.imgur.com/H3b7H1S.gif
    //use this executor for various small async tasks
    public final static ExecutorService executor = Executors.newCachedThreadPool();

    JDA jda;

    private static DatabaseManager dbManager;

    public static void main(String[] args) throws LoginException, IllegalArgumentException, InterruptedException, IOException {
        //just post the info to the console
        if (args.length > 0 &&
                (args[0].equalsIgnoreCase("-v")
                        || args[0].equalsIgnoreCase("--version")
                        || args[0].equalsIgnoreCase("-version"))) {
            System.out.println("Version flag detected. Printing version info, then exiting.");
            System.out.println(getVersionInfo());
            System.out.println("Version info printed, exiting.");
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(ON_SHUTDOWN, "FredBoat main shutdownhook"));
        log.info(getVersionInfo());

        String javaVersionMinor = System.getProperty("java.version").split("\\.")[1];

        if (!javaVersionMinor.equals("8")) {
            log.warn("\n\t\t __      ___   ___ _  _ ___ _  _  ___ \n" +
                    "\t\t \\ \\    / /_\\ | _ \\ \\| |_ _| \\| |/ __|\n" +
                    "\t\t  \\ \\/\\/ / _ \\|   / .` || || .` | (_ |\n" +
                    "\t\t   \\_/\\_/_/ \\_\\_|_\\_|\\_|___|_|\\_|\\___|\n" +
                    "\t\t                                      ");
            log.warn("FredBoat only supports Java 8. You are running Java " + javaVersionMinor);
        }

        I18n.start();

        Config.loadDefaultConfig();

        try {
            API.start();
        } catch (Exception e) {
            log.info("Failed to ignite Spark, FredBoat API unavailable", e);
        }

        if (!Config.CONFIG.getJdbcUrl().equals("")) {
            dbManager = new DatabaseManager(Config.CONFIG.getJdbcUrl(), null, Config.CONFIG.getHikariPoolSize());
            dbManager.startup();
            FredBoatAgent.start(new DBConnectionWatchdogAgent(dbManager));
        } else if (Config.CONFIG.getNumShards() > 2) {
            log.warn("No JDBC URL and more than 2 shard found! Initializing the SQLi DB is potentially dangerous too. Skipping...");
        } else {
            log.warn("No JDBC URL found, skipped database connection, falling back to internal SQLite db.");
            dbManager = new DatabaseManager("jdbc:sqlite:fredboat.db", "org.hibernate.dialect.SQLiteDialect",
                    Config.CONFIG.getHikariPoolSize());
            dbManager.startup();
        }

        //Initialise event listeners
        listenerBot = new EventListenerBoat();
        LavalinkManager.ins.start();

        //Commands
        if (Config.CONFIG.getDistribution() == DistributionEnum.DEVELOPMENT)
            MainCommandInitializer.initCommands();

        if (Config.CONFIG.getDistribution() == DistributionEnum.DEVELOPMENT
                || Config.CONFIG.getDistribution() == DistributionEnum.MUSIC
                || Config.CONFIG.getDistribution() == DistributionEnum.PATRON)
            MusicCommandInitializer.initCommands();

        log.info("Loaded commands, registry size is " + CommandRegistry.getSize());

        //Check MAL creds
        executor.submit(FredBoat::hasValidMALLogin);

        //Check imgur creds
        executor.submit(FredBoat::hasValidImgurCredentials);

        //Check OpenWeather key
        executor.submit(FredBoat::hasValidOpenWeatherKey);

        /* Init JDA */
        initBotShards(listenerBot);

        if (Config.CONFIG.getDistribution() == DistributionEnum.MUSIC && Config.CONFIG.getCarbonKey() != null) {
            FredBoatAgent.start(new CarbonitexAgent(Config.CONFIG.getCarbonKey()));
        }

        FredBoatAgent.start(new ShardWatchdogAgent());
    }

    private static boolean hasValidMALLogin() {
        String malUser = Config.CONFIG.getMalUser();
        String malPassWord = Config.CONFIG.getMalPassword();
        if (malUser == null || malUser.isEmpty() || malPassWord == null || malPassWord.isEmpty()) {
            log.info("MAL credentials not found. MAL related commands will not be available.");
            return false;
        }

        Http.SimpleRequest request = Http.get("https://myanimelist.net/api/account/verify_credentials.xml")
                .auth(Credentials.basic(malUser, malPassWord));

        try (Response response = request.execute()) {
            if (response.isSuccessful()) {
                log.info("MAL login successful");
                return true;
            } else {
                //noinspection ConstantConditions
                log.warn("MAL login failed with {}\n{}", response.toString(), response.body().string());
            }
        } catch (IOException e) {
            log.warn("MAL login failed, it seems to be down.", e);
        }
        return false;
    }

    private static boolean hasValidImgurCredentials() {
        String imgurClientId = Config.CONFIG.getImgurClientId();
        if (imgurClientId == null || imgurClientId.isEmpty()) {
            log.info("Imgur credentials not found. Commands relying on Imgur will not work properly.");
            return false;
        }
        Http.SimpleRequest request = Http.get("https://api.imgur.com/3/credits")
                .auth("Client-ID " + imgurClientId);
        try (Response response = request.execute()) {
            //noinspection ConstantConditions
            String content = response.body().string();
            if (response.isSuccessful()) {
                JSONObject data = new JSONObject(content).getJSONObject("data");
                //https://api.imgur.com/#limits
                //at the time of the introduction of this code imgur offers daily 12500 and hourly 500 GET requests for open source software
                //hitting the daily limit 5 times in a month will blacklist the app for the rest of the month
                //we use 3 requests per hour (and per restart of the bot), so there should be no problems with imgur's rate limit
                int hourlyLimit = data.getInt("UserLimit");
                int hourlyLeft = data.getInt("UserRemaining");
                long seconds = data.getLong("UserReset") - (System.currentTimeMillis() / 1000);
                String timeTillReset = String.format("%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, (seconds % 60));
                int dailyLimit = data.getInt("ClientLimit");
                int dailyLeft = data.getInt("ClientRemaining");
                log.info("Imgur credentials are valid. " + hourlyLeft + "/" + hourlyLimit +
                        " requests remaining this hour, resetting in " + timeTillReset + ", " +
                        dailyLeft + "/" + dailyLimit + " requests remaining today.");
                return true;
            } else {
                log.warn("Imgur login failed with {}\n{}", response.toString(), content);
            }
        } catch (IOException e) {
            log.warn("Imgur login failed, it seems to be down.", e);
        }
        return false;
    }

    /**
     * Method to check if there is an error to retrieve open weather data.
     *
     * @return True if it can retrieve data, else return false.
     */
    private static boolean hasValidOpenWeatherKey() {
        if ("".equals(Config.CONFIG.getOpenWeatherKey())) {
            log.warn("Open Weather API credentials not found. Weather related commands will not work properly.");
            return false;
        }

        OpenWeatherAPI api = new OpenWeatherAPI();
        RetrievedWeather weather = api.getCurrentWeatherByCity("san francisco");

        boolean isSuccess = !(weather == null || weather.isError());

        if (isSuccess) {
            log.info("Open Weather API check successful");
        } else {
            log.warn("Open Weather API check failed. It may be down, the provided credentials may be invalid, or temporarily blocked.");
        }
        return isSuccess;
    }

    private static void initBotShards(EventListener listener) {
        for (int i = 0; i < Config.CONFIG.getNumShards(); i++) {
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

    public void onInit(ReadyEvent readyEvent) {
        log.info("Received ready event for " + FredBoat.getInstance(readyEvent.getJDA()).getShardInfo().getShardString());

        int ready = numShardsReady.get();
        if (ready == Config.CONFIG.getNumShards()) {
            log.info("All " + ready + " shards are ready.");

            if (Config.CONFIG.getNumShards() <= 10) {
                MusicPersistenceHandler.reloadPlaylists();
            } else {
                log.warn("Skipped music persistence loading! We are using more than 10 shards, so probably not a good idea to run that.");
            }
        }

        //Rejoin old channels if revived
        channelsToRejoin.forEach(vcid -> {
            VoiceChannel channel = readyEvent.getJDA().getVoiceChannelById(vcid);
            if (channel == null) return;
            GuildPlayer player = PlayerRegistry.getOrCreate(channel.getGuild());

            LavalinkManager.ins.openConnection(channel);

            if (!LavalinkManager.ins.isEnabled()) {
                AudioManager am = channel.getGuild().getAudioManager();
                am.setSendingHandler(player);
            }
        });

        channelsToRejoin.clear();
    }

    //Shutdown hook
    private static final Runnable ON_SHUTDOWN = () -> {
        int code = shutdownCode != UNKNOWN_SHUTDOWN_CODE ? shutdownCode : -1;

        FredBoatAgent.shutdown();

        try {
            MusicPersistenceHandler.handlePreShutdown(code);
        } catch (Exception e) {
            log.error("Critical error while handling music persistence.", e);
        }

        for (FredBoat fb : shards) {
            fb.getJda().shutdown();
        }

        executor.shutdown();
        dbManager.shutdown();
    };

    public static void shutdown(int code) {
        log.info("Shutting down with exit code " + code);
        shutdownCode = code;

        System.exit(code);
    }

    public static EventListenerBoat getListenerBot() {
        return listenerBot;
    }

    /* Sharding */

    public JDA getJda() {
        return jda;
    }

    public static List<FredBoat> getShards() {
        return shards;
    }

    public static List<Guild> getAllGuilds() {
        return JDAUtil.getAllGuilds(shards);
    }

    public static int countAllGuilds() {
        return JDAUtil.countAllGuilds(shards);
    }

    //this probably takes horribly long and should be solved in a different way
    //rewrite it when we actually come up with a use case for needing all user objects
//    @Deprecated
//    public static Map<String, User> getAllUsersAsMap() {
//        HashMap<String, User> map = new HashMap<>();
//
//        for (FredBoat fb : shards) {
//            for (User usr : fb.getJda().getUsers()) {
//                map.put(usr.getId(), usr);
//            }
//        }
//        return map;
//    }

    private static AtomicInteger biggestUserCount = new AtomicInteger(-1);

    //IMPORTANT: do not use this for actually counting, it will not be accurate; it is meant to be used to initialize
    // sets or maps that are about to hold all those user values
    public static int getExpectedUserCount() {
        if (biggestUserCount.get() <= 0) { //initialize
            countAllUniqueUsers();
        }
        return biggestUserCount.get();
    }

    public static long countAllUniqueUsers() {
        return JDAUtil.countAllUniqueUsers(shards, biggestUserCount);
    }

    @Nullable
    public static TextChannel getTextChannelById(String id) {
        for (FredBoat fb : shards) {
            for (TextChannel channel : fb.getJda().getTextChannels()) {
                if (channel.getId().equals(id)) return channel;
            }
        }

        return null;
    }

    @Nullable
    public static VoiceChannel getVoiceChannelById(String id) {
        for (FredBoat fb : shards) {
            for (VoiceChannel channel : fb.getJda().getVoiceChannels()) {
                if (channel.getId().equals(id)) return channel;
            }
        }

        return null;
    }

    @Nullable
    public static Guild getGuildById(long id) {
        for (FredBoat fb : shards) {
            Guild g = fb.getJda().getGuildById(id);
            if (g != null) return g;
        }

        return null;
    }

    public static FredBoat getInstance(JDA jda) {
        int sId = jda.getShardInfo() == null ? 0 : jda.getShardInfo().getShardId();
        for (FredBoat fb : shards) {
            if (((FredBoatBot) fb).getShardId() == sId) {
                return fb;
            }
        }
        throw new IllegalStateException("Attempted to get instance for JDA shard that is not indexed, shardId: " + sId);
    }

    public static FredBoat getInstance(int id) {
        return shards.get(id);
    }

    public static JDA getFirstJDA() {
        return shards.get(0).getJda();
    }

    public ShardInfo getShardInfo() {
        int sId = jda.getShardInfo() == null ? 0 : jda.getShardInfo().getShardId();

        if (jda.getAccountType() == AccountType.CLIENT) {
            return new ShardInfo(0, 1);
        } else {
            return new ShardInfo(sId, Config.CONFIG.getNumShards());
        }
    }

    public long getGuildCount() {
        return JDAUtil.countAllGuilds(Collections.singletonList(this));
    }

    public long getUserCount() {
        return JDAUtil.countAllUniqueUsers(Collections.singletonList(this), biggestUserCount);
    }

    public abstract String revive(boolean... force);

    public ShardWatchdogListener getShardWatchdogListener() {
        return shardWatchdogListener;
    }

    @SuppressWarnings("WeakerAccess")
    public static class ShardInfo {

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

        @Override
        public String toString() {
            return getShardString();
        }
    }

    @Nullable
    public static DatabaseManager getDbManager() {
        return dbManager;
    }

    private static volatile long lastCoinGivenOut = 0;

    // if you get a coin, you are allowed to build a shard (= perform a login to discord)
    public static synchronized boolean getShardCoin(int shardId) {
        long now = System.currentTimeMillis();
        if (now - lastCoinGivenOut >= SHARD_CREATION_SLEEP_INTERVAL) {
            lastCoinGivenOut = now;
            log.info("Coin for shard {}", shardId);
            return true;
        }
        return false;
    }

    private static String getVersionInfo() {
        return "\n\n" +
                "  ______            _ ____              _   \n" +
                " |  ____|          | |  _ \\            | |  \n" +
                " | |__ _ __ ___  __| | |_) | ___   __ _| |_ \n" +
                " |  __| '__/ _ \\/ _` |  _ < / _ \\ / _` | __|\n" +
                " | |  | | |  __/ (_| | |_) | (_) | (_| | |_ \n" +
                " |_|  |_|  \\___|\\__,_|____/ \\___/ \\__,_|\\__|\n\n"

                + "\n\tVersion:       " + AppInfo.getAppInfo().VERSION
                + "\n\tBuild:         " + AppInfo.getAppInfo().BUILD_NUMBER
                + "\n\tCommit:        " + GitRepoState.getGitRepositoryState().commitIdAbbrev + " (" + GitRepoState.getGitRepositoryState().branch + ")"
                + "\n\tCommit time:   " + GitRepoState.getGitRepositoryState().commitTime
                + "\n\tJVM:           " + System.getProperty("java.version")
                + "\n\tJDA:           " + JDAInfo.VERSION
                + "\n\tLavaplayer     " + PlayerLibrary.VERSION
                + "\n";
    }
}
