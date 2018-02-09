package fredboat.main;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import fredboat.agent.*;
import fredboat.api.API;
import fredboat.audio.player.LavalinkManager;
import fredboat.commandmeta.CommandInitializer;
import fredboat.commandmeta.CommandRegistry;
import fredboat.db.DatabaseManager;
import fredboat.db.EntityIO;
import fredboat.event.EventListenerBoat;
import fredboat.event.EventLogger;
import fredboat.feature.DikeSessionController;
import fredboat.feature.I18n;
import fredboat.feature.metrics.Metrics;
import fredboat.feature.metrics.OkHttpEventMetrics;
import fredboat.shared.constant.DistributionEnum;
import fredboat.shared.constant.ExitCodes;
import fredboat.util.AppInfo;
import fredboat.util.DiscordUtil;
import fredboat.util.GitRepoState;
import fredboat.util.TextUtils;
import fredboat.util.rest.Http;
import fredboat.util.rest.OpenWeatherAPI;
import fredboat.util.rest.models.weather.RetrievedWeather;
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.utils.SessionController;
import net.dv8tion.jda.core.utils.SessionControllerAdapter;
import okhttp3.Credentials;
import okhttp3.Response;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.sqlsauce.DatabaseConnection;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.sqlsauce.DatabaseWrapper;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * The class responsible for launching FredBoat
 */
public class Launcher {

    private static final Logger log = LoggerFactory.getLogger(Launcher.class);
    public static final long START_TIME = System.currentTimeMillis();
    private static final BotController FBC = BotController.INS;

    public static void main(String[] args) throws IllegalArgumentException, InterruptedException, DatabaseException {
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

        Runtime.getRuntime().addShutdownHook(new Thread(FBC.shutdownHook, "FredBoat main shutdownhook"));
        log.info(getVersionInfo());

        String javaVersionMinor = null;
        try {
            javaVersionMinor = System.getProperty("java.version").split("\\.")[1];
        } catch (Exception e) {
            log.error("Exception while checking if java 8", e);
        }

        if (!Objects.equals(javaVersionMinor, "8")) {
            log.warn("\n\t\t __      ___   ___ _  _ ___ _  _  ___ \n" +
                    "\t\t \\ \\    / /_\\ | _ \\ \\| |_ _| \\| |/ __|\n" +
                    "\t\t  \\ \\/\\/ / _ \\|   / .` || || .` | (_ |\n" +
                    "\t\t   \\_/\\_/_/ \\_\\_|_\\_|\\_|___|_|\\_|\\___|\n" +
                    "\t\t                                      ");
            log.warn("FredBoat only officially supports Java 8. You are running Java {}", System.getProperty("java.version"));
        }

        FBC.postInit();

        Metrics.setup();

        I18n.start();

        try {
            API.start();
        } catch (Exception e) {
            log.info("Failed to ignite Spark, FredBoat API unavailable", e);
        }

        //attempt to connect to the database a few times
        // this is relevant in a dockerized environment because after a reboot there is no guarantee that the db
        // container will be started before the fredboat one
        int dbConnectionAttempts = 0;
        DatabaseConnection mainDbConn = null;
        while ((mainDbConn == null || !mainDbConn.isAvailable()) && dbConnectionAttempts++ < 10) {
            try {
                if (mainDbConn != null) {
                    mainDbConn.shutdown();
                }
                mainDbConn = DatabaseManager.main();
            } catch (Exception e) {
                log.info("Could not connect to the database. Retrying in a moment...", e);
                Thread.sleep(6000);
            }
        }
        if (mainDbConn == null || !mainDbConn.isAvailable()) {
            log.error("Could not establish database connection. Exiting...");
            FBC.shutdown(ExitCodes.EXIT_CODE_ERROR);
            return;
        }
        FredBoatAgent.start(new DBConnectionWatchdogAgent(mainDbConn));
        FBC.setMainDbWrapper(new DatabaseWrapper(mainDbConn));

        DatabaseConnection cacheDbConn = null;
        try {
            cacheDbConn = DatabaseManager.cache();
            if (cacheDbConn != null) {
                FBC.setCacheDbWrapper(new DatabaseWrapper(cacheDbConn));
            }
        } catch (Exception e) {
            log.error("Exception when connecting to cache db", e);
            FBC.shutdown(ExitCodes.EXIT_CODE_ERROR);
        }
        Metrics.instance().hibernateStats.register(); //call this exactly once after all db connections have been created

        FBC.setEntityIO(new EntityIO(mainDbConn, cacheDbConn));

        //Initialise event listeners
        FBC.setMainEventListener(new EventListenerBoat());
        LavalinkManager.ins.start();

        //Commands
        CommandInitializer.initCommands();
        log.info("Loaded commands, registry size is " + CommandRegistry.getTotalSize());

        if (!Config.CONFIG.isPatronDistribution()) {
            log.info("Starting VoiceChannelCleanupAgent.");
            FredBoatAgent.start(new VoiceChannelCleanupAgent());
        } else {
            log.info("Skipped setting up the VoiceChannelCleanupAgent, " +
                    "either running Patron distro or overridden by temp config");
        }

        ExecutorService executor = FBC.getExecutor();

        //Check MAL creds
        executor.submit(Launcher::hasValidMALLogin);

        //Check imgur creds
        executor.submit(Launcher::hasValidImgurCredentials);

        //Check OpenWeather key
        executor.submit(Launcher::hasValidOpenWeatherKey);

        String carbonKey = Config.CONFIG.getCarbonKey();
        if (Config.CONFIG.getDistribution() == DistributionEnum.MUSIC && carbonKey != null && !carbonKey.isEmpty()) {
            FredBoatAgent.start(new CarbonitexAgent(carbonKey));
        }

        // Log into Discord
        try {
            FBC.setShardManager(buildShardManager());
        } catch (LoginException e) {
            throw new RuntimeException("Failed to log in to Discord! Is your token invalid?", e);
        }

        enableMetrics();
    }

    // ################################################################################
    // ##                     Login / credential tests
    // ################################################################################

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

    //returns true if all registered shards are reporting back as CONNECTED, false otherwise
    private static boolean areWeReadyYet() {
        for (JDA.Status status : FBC.getShardManager().getStatuses().values()) {
            if (status != JDA.Status.CONNECTED) {
                return false;
            }
        }

        return true;
    }

    private static void enableMetrics() throws InterruptedException {
        //wait for all shards to ready up before requesting a total count of jda entities
        while (!areWeReadyYet()) {
            Thread.sleep(1000);
        }

        StatsAgent statsAgent = FBC.getStatsAgent();
        //force some metrics to be populated, then turn on metrics to be served
        try {
            BotMetrics.JdaEntityCounts jdaEntityCountsTotal = BotMetrics.getJdaEntityCountsTotal();
            try {
                jdaEntityCountsTotal.count(() -> FBC.getShardManager().getShards());
            } catch (Exception ignored) {
            }

            statsAgent.addAction(new BotMetrics.JdaEntityStatsCounter(
                    () -> jdaEntityCountsTotal.count(() -> FBC.getShardManager().getShards())));

            if (DiscordUtil.isOfficialBot()) {
                BotMetrics.DockerStats dockerStats = BotMetrics.getDockerStats();
                try {
                    dockerStats.fetch();
                } catch (Exception ignored) {
                }
                statsAgent.addAction(dockerStats::fetch);
            }
        } finally {
            FredBoatAgent.start(statsAgent);
            API.turnOnMetrics();
        }
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
                + "\n\tCommit time:   " + TextUtils.asTimeInCentralEurope(GitRepoState.getGitRepositoryState().commitTime * 1000)
                + "\n\tJVM:           " + System.getProperty("java.version")
                + "\n\tJDA:           " + JDAInfo.VERSION
                + "\n\tLavaplayer     " + PlayerLibrary.VERSION
                + "\n";
    }

    private static ShardManager buildShardManager() throws LoginException {
        SessionController sessionController = Config.CONFIG.getDikeUrl() == null
                ? new SessionControllerAdapter()
                : new DikeSessionController();

        DefaultShardManagerBuilder builder = new DefaultShardManagerBuilder()
                .setToken(Config.CONFIG.getBotToken())
                .setGame(Game.playing(Config.CONFIG.getGame()))
                .setBulkDeleteSplittingEnabled(false)
                .setEnableShutdownHook(false)
                .setAudioEnabled(true)
                .setAutoReconnect(true)
                .setSessionController(sessionController)
                .setContextEnabled(false)
                .setHttpClientBuilder(Http.defaultHttpClient.newBuilder()
                        .eventListener(new OkHttpEventMetrics("jda")))
                .addEventListeners(BotController.INS.getMainEventListener())
                .addEventListeners(Metrics.instance().jdaEventsMetricsListener)
                .setShardsTotal(Config.getNumShards());

        String eventLogWebhook = Config.CONFIG.getEventLogWebhook();
        if (eventLogWebhook != null && !eventLogWebhook.isEmpty()) {
            try {
                builder.addEventListeners(new EventLogger());
            } catch (Exception e) {
                log.error("Failed to create Eventlogger, events will not be logged to discord via webhook", e);
            }
        }

        if (LavalinkManager.ins.isEnabled()) {
            builder.addEventListeners(LavalinkManager.ins.getLavalink());
        }

        if (!System.getProperty("os.arch").equalsIgnoreCase("arm")
                && !System.getProperty("os.arch").equalsIgnoreCase("arm-linux")) {
            builder.setAudioSendFactory(new NativeAudioSendFactory(800));
        }

        return builder.build();
    }

}
