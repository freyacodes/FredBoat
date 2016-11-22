/*
 * The MIT License (MIT)
 * Copyright (c) 2016 Frederik Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package fredboat;

import fredboat.agent.CarbonAgent;
import fredboat.agent.CarbonitexAgent;
import fredboat.audio.MusicPersistenceHandler;
import fredboat.audio.PlayerRegistry;
import fredboat.command.admin.*;
import fredboat.command.fun.*;
import fredboat.command.maintenance.GetIdCommand;
import fredboat.command.maintenance.StatsCommand;
import fredboat.command.maintenance.TestCommand;
import fredboat.command.maintenance.VersionCommand;
import fredboat.command.music.*;
import fredboat.command.util.*;
import fredboat.commandmeta.CommandRegistry;
import fredboat.event.EventListenerBoat;
import fredboat.event.EventListenerSelf;
import fredboat.event.EventLogger;
import fredboat.sharding.FredBoatAPIServer;
import fredboat.sharding.ShardTracker;
import fredboat.util.BotConstants;
import fredboat.util.DiscordUtil;
import fredboat.util.DistributionEnum;
import fredboat.util.log.SimpleLogToSLF4JAdapter;
import frederikam.jca.JCA;
import frederikam.jca.JCABuilder;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.JDAInfo;
import net.dv8tion.jda.client.JDAClientBuilder;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.utils.SimpleLog;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FredBoat {

    private static final Logger log = LoggerFactory.getLogger(FredBoat.class);
    private static JSONObject config = null;

    private static int scopes = 0;
    public static volatile JDA jdaBot;
    public static volatile JDA jdaSelf;
    public static JCA jca;
    public static final long START_TIME = System.currentTimeMillis();
    private static String accountToken;
    public static String mashapeKey;

    public static String MALPassword;

    public static int readyEvents = 0;
    public static int readyEventsRequired = 0;

    public static int shardId = 0;
    public static int numShards = 1;

    private static JSONObject credsjson = null;
    public static DistributionEnum distribution = DistributionEnum.BETA;

    public static final int UNKNOWN_SHUTDOWN_CODE = -991023;
    public static int shutdownCode = UNKNOWN_SHUTDOWN_CODE;//Used when specifying the intended code for shutdown hooks

    private final static List<String> GOOGLE_KEYS = new ArrayList<>();

    private static EventListenerBoat listenerBot;
    private static EventListenerSelf listenerSelf;

    private FredBoat() {
    }

    public static void main(String[] args) throws LoginException, IllegalArgumentException, InterruptedException, IOException {
        Runtime.getRuntime().addShutdownHook(new Thread(ON_SHUTDOWN));

        //Attach log adapter
        SimpleLog.addListener(new SimpleLogToSLF4JAdapter());

        //Make JDA not print to console, we have Logback for that
        SimpleLog.LEVEL = SimpleLog.Level.OFF;

        try {
            scopes = Integer.parseInt(args[0]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
            log.info("Invalid scope, defaulting to scopes 0x101");
            scopes = 0x110;
        }

        try {
            shardId = Integer.parseInt(args[1]);
            numShards = Integer.parseInt(args[2]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
            log.info("Invalid shards, defaulting to 0 of 1 shards");
        }

        log.info("Starting with scopes:"
                + "\n\tMain: " + ((scopes & 0x100) == 0x100)
                + "\n\tMusic: " + ((scopes & 0x010) == 0x010)
                + "\n\tSelf: " + ((scopes & 0x001) == 0x001));

        log.info("Starting as shard " + shardId + " of " + numShards);

        //Load credentials and config files
        InputStream is = new FileInputStream(new File("./credentials.json"));
        Scanner scanner = new Scanner(is);
        credsjson = new JSONObject(scanner.useDelimiter("\\A").next());
        scanner.close();

        is = new FileInputStream(new File("./config.json"));
        scanner = new Scanner(is);
        config = new JSONObject(scanner.useDelimiter("\\A").next());
        scanner.close();

        mashapeKey = credsjson.getString("mashapeKey");
        String clientToken = credsjson.getString("clientToken");
        MALPassword = credsjson.getString("malPassword");
        String carbonHost = credsjson.optString("carbonHost");

        JSONArray gkeys = credsjson.optJSONArray("googleServerKeys");
        if (gkeys != null) {
            gkeys.forEach((Object str) -> {
                GOOGLE_KEYS.add((String) str);
            });
        }

        if (credsjson.has("scopePasswords")) {
            JSONObject scopePasswords = credsjson.getJSONObject("scopePasswords");
            for (String k : scopePasswords.keySet()) {
                scopePasswords.put(k, scopePasswords.getString(k));
            }
        }

        if (config.optBoolean("patron")) {
            distribution = DistributionEnum.PATRON;
        } else //Determine distribution
        if (BotConstants.IS_BETA) {
            distribution = DistributionEnum.BETA;
        } else {
            distribution = DiscordUtil.isMainBot() ? DistributionEnum.MAIN : DistributionEnum.MUSIC;
        }
        accountToken = credsjson.getJSONObject("token").getString(distribution.getId());

        log.info("Determined distribution: " + distribution);

        //Initialise event listeners
        listenerBot = new EventListenerBoat(scopes & 0x110, BotConstants.DEFAULT_BOT_PREFIX);
        listenerSelf = new EventListenerSelf(scopes & 0x001, BotConstants.DEFAULT_SELF_PREFIX);

        /* Init JDA */
        //Doing increments here because concurrency
        if ((scopes & 0x110) != 0) {
            readyEventsRequired++;
        }

        if ((scopes & 0x001) != 0) {
            readyEventsRequired++;
        }

        if ((scopes & 0x110) != 0) {
            JDABuilder builder = new JDABuilder()
                    .addListener(listenerBot)
                    .addListener(new EventLogger("216689009110417408"))
                    .setBotToken(accountToken)
                    .setBulkDeleteSplittingEnabled(true);
            if (numShards > 1) {
                builder.useSharding(shardId, numShards);
            }
            jdaBot = builder.buildAsync();
        }

        if ((scopes & 0x001) != 0 && shardId == 0) {
            jdaSelf = new JDAClientBuilder()
                    .addListener(listenerSelf)
                    .setClientToken(clientToken)
                    .buildAsync();
        }

        /* JDA initialising */
        log.info("JDA version:\t" + JDAInfo.VERSION);

        //Initialise JCA
        String cbUser = credsjson.getString("cbUser");
        String cbKey = credsjson.getString("cbKey");
        jca = new JCABuilder().setKey(cbKey).setUser(cbUser).buildBlocking();

        if (!BotConstants.IS_BETA) {
            CarbonitexAgent carbonitexAgent = new CarbonitexAgent(jdaBot, credsjson.getString("carbonKey"));
            carbonitexAgent.setDaemon(true);
            carbonitexAgent.start();
        }

        if (!carbonHost.equals("")) {
            //Determine metric name
            String metricName = "beta";
            if (!BotConstants.IS_BETA) {
                metricName = DiscordUtil.isMusicBot() ? "music" : "production";
            }

            CarbonAgent carbonAgent = new CarbonAgent(jdaBot, carbonHost, metricName, !BotConstants.IS_BETA);
            carbonAgent.setDaemon(true);
            carbonAgent.start();
            log.info("Started reporting to carbon-cache at " + carbonHost + " with metric name " + metricName + ".");
        } else {
            log.info("No carbon host configured. Skipping carbon daemon.");
        }
    }

    public static void init(ReadyEvent event) {
        readyEvents = readyEvents + 1;

        log.info("INIT: " + readyEvents + "/" + readyEventsRequired);

        if (readyEvents < readyEventsRequired) {
            return;
        }

        try {
            //Init the REST server
            new FredBoatAPIServer(
                    jdaBot,
                    credsjson.optString("fredboatToken", "NOT_SET"),
                    new String[]{"--server.port=" + distribution.getPort(shardId)}
            ).start();

            new ShardTracker(
                    jdaBot,
                    credsjson.optString("fredboatToken", "NOT_SET")
            ).start();
        } catch (Exception ex) {
            log.error("Failed to start Spring Boot server", ex);
            System.exit(-1);
        }

        //Init music system
        PlayerRegistry.init(jdaBot);

        //Commands
        CommandRegistry.registerCommand(0x110, "help", new HelpCommand());
        CommandRegistry.registerAlias("help", "info");
        CommandRegistry.registerCommand(0x101, "version", new VersionCommand());
        CommandRegistry.registerCommand(0x101, "say", new SayCommand());
        CommandRegistry.registerCommand(0x101, "uptime", new StatsCommand());
        CommandRegistry.registerAlias("uptime", "stats");
        CommandRegistry.registerCommand(0x101, "exit", new ExitCommand());
        CommandRegistry.registerCommand(0x101, "avatar", new AvatarCommand());
        CommandRegistry.registerCommand(0x101, "test", new TestCommand());
        CommandRegistry.registerCommand(0x101, "lua", new LuaCommand());
        CommandRegistry.registerCommand(0x101, "brainfuck", new BrainfuckCommand());
        CommandRegistry.registerCommand(0x101, "joke", new JokeCommand());
        CommandRegistry.registerCommand(0x101, "leet", new LeetCommand());
        CommandRegistry.registerAlias("leet", "1337");
        CommandRegistry.registerAlias("leet", "l33t");
        CommandRegistry.registerAlias("leet", "1ee7");
        CommandRegistry.registerCommand(0x101, "riot", new RiotCommand());
        CommandRegistry.registerCommand(0x101, "update", new UpdateCommand());
        CommandRegistry.registerCommand(0x101, "compile", new CompileCommand());
        CommandRegistry.registerCommand(0x101, "botrestart", new BotRestartCommand());
        CommandRegistry.registerCommand(0x101, "find", new FindCommand());
        CommandRegistry.registerCommand(0x101, "dance", new DanceCommand());
        CommandRegistry.registerCommand(0x101, "eval", new EvalCommand());
        CommandRegistry.registerCommand(0x101, "s", new TextCommand("¯\\_(ツ)_/¯"));
        CommandRegistry.registerAlias("s", "shrug");
        CommandRegistry.registerCommand(0x101, "lenny", new TextCommand("( ͡° ͜ʖ ͡°)"));
        CommandRegistry.registerCommand(0x101, "useless", new TextCommand("This command is useless."));
        CommandRegistry.registerCommand(0x101, "clear", new ClearCommand());
        CommandRegistry.registerCommand(0x101, "talk", new TalkCommand());
        CommandRegistry.registerCommand(0x101, "dump", new DumpCommand());
        CommandRegistry.registerCommand(0x101, "mal", new MALCommand());
        CommandRegistry.registerCommand(0x101, "akinator", new AkinatorCommand());

        /* Music commands */
        CommandRegistry.registerCommand(0x010, "mexit", new ExitCommand());
        CommandRegistry.registerCommand(0x010, "mbotrestart", new BotRestartCommand());
        CommandRegistry.registerCommand(0x010, "mstats", new StatsCommand());
        CommandRegistry.registerCommand(0x010, "play", new PlayCommand());
        CommandRegistry.registerCommand(0x010, "meval", new EvalCommand());
        CommandRegistry.registerCommand(0x010, "skip", new SkipCommand());
        CommandRegistry.registerCommand(0x010, "join", new JoinCommand());
        CommandRegistry.registerAlias("join", "summon");
        CommandRegistry.registerCommand(0x010, "nowplaying", new NowplayingCommand());
        CommandRegistry.registerAlias("nowplaying", "np");
        CommandRegistry.registerCommand(0x010, "leave", new LeaveCommand());
        CommandRegistry.registerCommand(0x010, "list", new ListCommand());
        CommandRegistry.registerAlias("list", "queue");
        CommandRegistry.registerCommand(0x010, "mupdate", new UpdateCommand());
        CommandRegistry.registerCommand(0x010, "mcompile", new CompileCommand());
        CommandRegistry.registerCommand(0x010, "select", new SelectCommand());
        CommandRegistry.registerCommand(0x010, "stop", new StopCommand());
        CommandRegistry.registerCommand(0x010, "pause", new PauseCommand());
        CommandRegistry.registerCommand(0x010, "unpause", new UnpauseCommand());
        CommandRegistry.registerCommand(0x010, "getid", new GetIdCommand());
        CommandRegistry.registerCommand(0x010, "shuffle", new ShuffleCommand());
        CommandRegistry.registerCommand(0x010, "repeat", new RepeatCommand());
        CommandRegistry.registerCommand(0x010, "volume", new VolumeCommand());
        CommandRegistry.registerAlias("volume", "vol");
        CommandRegistry.registerCommand(0x010, "restart", new RestartCommand());
        CommandRegistry.registerCommand(0x010, "export", new ExportCommand());
        CommandRegistry.registerCommand(0x010, "playerdebug", new PlayerDebugCommand());

        /* Other Anime Discord, Sergi memes or any other memes */
        CommandRegistry.registerCommand(0x101, "ram", new RemoteFileCommand("http://i.imgur.com/jeGVLk3.jpg"));
        CommandRegistry.registerCommand(0x101, "welcome", new RemoteFileCommand("http://i.imgur.com/yjpmmBk.gif"));
        CommandRegistry.registerCommand(0x101, "rude", new RemoteFileCommand("http://i.imgur.com/pUn7ijx.png"));
        CommandRegistry.registerCommand(0x101, "fuck", new RemoteFileCommand("http://i.imgur.com/1bllKNh.png"));
        CommandRegistry.registerCommand(0x101, "idc", new RemoteFileCommand("http://i.imgur.com/0ZPjpNg.png"));
        CommandRegistry.registerCommand(0x101, "beingraped", new RemoteFileCommand("http://i.imgur.com/dPsYRYV.png"));
        CommandRegistry.registerCommand(0x101, "anime", new RemoteFileCommand("https://cdn.discordapp.com/attachments/132490115137142784/177751190333816834/animeeee.png"));
        CommandRegistry.registerCommand(0x101, "wow", new RemoteFileCommand("http://img.prntscr.com/img?url=http://i.imgur.com/aexiMAG.png"));
        CommandRegistry.registerCommand(0x101, "what", new RemoteFileCommand("http://i.imgur.com/CTLraK4.png"));
        CommandRegistry.registerCommand(0x101, "pun", new RemoteFileCommand("http://i.imgur.com/2hFMrjt.png"));
        CommandRegistry.registerCommand(0x101, "die", new RemoteFileCommand("http://nekomata.moe/i/k2B0f6.png"));
        CommandRegistry.registerCommand(0x101, "stupid", new RemoteFileCommand("http://nekomata.moe/i/c056y7.png"));
        CommandRegistry.registerCommand(0x101, "cancer", new RemoteFileCommand("http://puu.sh/oQN2j/8e09872842.jpg"));
        CommandRegistry.registerCommand(0x101, "stupidbot", new RemoteFileCommand("https://cdn.discordapp.com/attachments/143976784545841161/183171963399700481/unknown.png"));
        CommandRegistry.registerCommand(0x101, "escape", new RemoteFileCommand("http://i.imgur.com/kk7Zu3C.png"));
        CommandRegistry.registerCommand(0x101, "explosion", new RemoteFileCommand("https://cdn.discordapp.com/attachments/143976784545841161/182893975965794306/megumin7.gif"));
        CommandRegistry.registerCommand(0x101, "gif", new RemoteFileCommand("https://cdn.discordapp.com/attachments/132490115137142784/182907929765085185/spacer.gif"));
        CommandRegistry.registerCommand(0x101, "noods", new RemoteFileCommand("http://i.imgur.com/CUE3gm2.png"));
        CommandRegistry.registerCommand(0x101, "internetspeed", new RemoteFileCommand("http://www.speedtest.net/result/5529046933.png"));
        CommandRegistry.registerCommand(0x101, "hug", new RemoteFileCommand("http://i.imgur.com/U2l7mnr.gif"));
        CommandRegistry.registerCommand(0x101, "powerpoint", new RemoteFileCommand("http://puu.sh/rISIl/1cc927ece3.PNG"));
        CommandRegistry.registerCommand(0x101, "cooldog", new DogCommand());
        CommandRegistry.registerAlias("cooldog", "dog");
        CommandRegistry.registerAlias("cooldog", "dogmeme");

        CommandRegistry.registerCommand(0x101, "github", new TextCommand("https://github.com/Frederikam"));
        CommandRegistry.registerCommand(0x101, "repo", new TextCommand("https://github.com/Frederikam/FredBoat"));

        String[] pats = {
            "http://i.imgur.com/wF1ohrH.gif",
            "http://cdn.photonesta.com/images/i.imgur.com/I3yvqFL.gif",
            "http://i4.photobucket.com/albums/y131/quentinlau/Blog/sola-02-Large15.jpg",
            "http://i.imgur.com/OYiSZWX.gif",
            "http://i.imgur.com/tmidE9Q.gif",
            "http://i.imgur.com/CoW20gH.gif",
            "http://31.media.tumblr.com/e759f2da1f07de37832fc8269e99f1e7/tumblr_n3w02z954N1swm6rso1_500.gif",
            "https://media1.giphy.com/media/ye7OTQgwmVuVy/200.gif",
            "http://data.whicdn.com/images/224314340/large.gif",
            "http://i.imgur.com/BNiNMWC.gifv",
            "http://i.imgur.com/9q6fkSK.jpg",
            "http://i.imgur.com/eOJlnwP.gif",
            "http://i.imgur.com/i7bklkm.gif",
            "http://i.imgur.com/fSDbKwf.jpg",
            "https://66.media.tumblr.com/ec7472fef28b2cdf394dc85132c22ed8/tumblr_mx1asbwrBv1qbvovho1_500.gif",};
        CommandRegistry.registerCommand(0x101, "pat", new PatCommand(pats));

        String[] facedesk = {
            "https://45.media.tumblr.com/tumblr_lpzn2uFp4D1qew6kmo1_500.gif",
            "http://i862.photobucket.com/albums/ab181/Shadow_Milez/Animu/kuroko-facedesk.gif",
            "http://2.bp.blogspot.com/-Uw0i2Xv8r-M/UhyYzSHIiCI/AAAAAAAAAdg/hcI1-V7Y3A4/s1600/facedesk.gif",
            "https://67.media.tumblr.com/dfa4f3c1b65da06d76a271feef0d08f0/tumblr_inline_o6zkkh6VsK1u293td_500.gif",
            "http://stream1.gifsoup.com/webroot/animatedgifs/57302_o.gif",
            "http://img.neoseeker.com/mgv/59301/301/26/facedesk_display.gif"
        };

        CommandRegistry.registerCommand(0x101, "facedesk", new FacedeskCommand(facedesk));

        String[] roll = {
            "https://media.giphy.com/media/3xz2BCBXokf7rag0Ba/giphy.gif",
            "http://i.imgur.com/IWQZaHD.gif",
            "https://warosu.org/data/cgl/img/0077/57/1408042492433.gif",
            "https://media.giphy.com/media/tso0dniqIDWwg/giphy.gif",
            "http://s19.postimg.org/lg5x9zx8z/Hakase_Roll_anime_32552527_500_282.gif",
            "http://i.imgur.com/UJxrB.gif",
            "http://25.media.tumblr.com/tumblr_m4k42bwzNy1qj0i6io1_500.gif",
            "http://i.giphy.com/3o6LXfWUBTdBcccgSc.gif",
            "http://66.media.tumblr.com/23dec349d26317df439099fdcb4c75a4/tumblr_mld6epWdgR1riizqco1_500.gif",
            "https://images-2.discordapp.net/eyJ1cmwiOiJodHRwOi8vZmFybTguc3RhdGljZmxpY2tyLmNvbS83NDUxLzEyMjY4NDM2MjU1XzgwZGIxOWNlOGZfby5naWYifQ.1e8OKozMAx22ZGELeNzRkqT3v-Q.gif",
            "https://images-1.discordapp.net/eyJ1cmwiOiJodHRwOi8vaS5pbWd1ci5jb20vS2VHY1lYSi5naWYifQ.4dCItRLO5l91JuDw-8ls-fi8wWc.gif",
            "http://i.imgur.com/s2TL7A8.gif",
            "http://i.imgur.com/vqTAjp5.gif",
            "https://data.desustorage.org/a/image/1456/58/1456582568150.gif"

        };

        CommandRegistry.registerCommand(0x101, "roll", new RollCommand(roll));

        MusicPersistenceHandler.reloadPlaylists();
    }

    //Shutdown hook
    private static final Runnable ON_SHUTDOWN = () -> {
        Runtime rt = Runtime.getRuntime();
        int code = shutdownCode != UNKNOWN_SHUTDOWN_CODE ? shutdownCode : -1;

        try {
            MusicPersistenceHandler.handlePreShutdown(code);
        } catch (Exception e) {
            log.error("Critical error while handling music persistence.", e);
        }
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

}
