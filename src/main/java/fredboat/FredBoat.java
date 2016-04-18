package fredboat;

import fredboat.command.fun.DanceCommand;
import fredboat.command.util.AvatarCommand;
import fredboat.command.util.BrainfuckCommand;
import fredboat.command.maintenance.DBGetCommand;
import fredboat.command.maintenance.ExitCommand;
import fredboat.command.util.FindCommand;
import fredboat.command.util.HelpCommand;
import fredboat.command.fun.JokeCommand;
import fredboat.command.fun.LeetCommand;
import fredboat.command.fun.TalkCommand;
import fredboat.command.fun.TextCommand;
import fredboat.command.mafia.MafiaStartCommand;
import fredboat.command.maintenance.EvalCommand;
import fredboat.command.util.LuaCommand;
import fredboat.command.maintenance.RestartCommand;
import fredboat.command.util.SayCommand;
import fredboat.command.maintenance.TestCommand;
import fredboat.command.maintenance.UptimeCommand;
import fredboat.command.util.ClearCommand;
import fredboat.commandmeta.CommandRegistry;
import fredboat.event.EventListenerBoat;
import fredboat.event.EventListenerSelf;
import frederikam.jca.JCA;
import frederikam.jca.JCABuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Scanner;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.JDAInfo;
import net.dv8tion.jda.client.JDAClientBuilder;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import org.json.JSONObject;

public class FredBoat {

    public static final boolean IS_BETA = "Windows 10".equals(System.getProperty("os.name"));
    public static volatile JDA jdaBot;
    public static volatile JDA jdaSelf;
    public static JCA jca;
    public static final String PREFIX = IS_BETA ? "¤" : ";;";
    public static final String SELF_PREFIX = IS_BETA ? "::" : "<<";
    public static final String OWNER_ID = "81011298891993088";
    public static final long START_TIME = System.currentTimeMillis();
    //public static final String ACCOUNT_EMAIL_KEY = IS_BETA ? "emailBeta" : "emailProduction";
    //public static final String ACCOUNT_PASSWORD_KEY = IS_BETA ? "passwordBeta" : "passwordProduction";
    public static final String ACCOUNT_TOKEN_KEY = IS_BETA ? "tokenBeta" : "tokenProduction";
    private static String accountToken;
    public static String CLIENT_ID = IS_BETA ? "168672778860494849" : "168686772216135681";
    //public static String accountEmail = IS_BETA ? "frederikmikkelsen2@outlook.com" : "frederikmikkelsen@outlook.com";
    //private static String accountPassword;
    public static String mashapeKey;
    public static String helpMsg = "";

    public static String myUserId = "";
    public static volatile User myUser;

    public static int readyEvents = 0;
    public static final int READY_EVENTS_REQUIRED = 2;

    public static EventListenerBoat listenerBot;
    public static EventListenerSelf listenerSelf;

    public static void main(String[] args) throws LoginException, IllegalArgumentException, InterruptedException, IOException {
        //Load credentials file
        FredBoat instance = new FredBoat();
        InputStream is = instance.getClass().getClassLoader().getResourceAsStream("credentials.json");
        Scanner scanner = new Scanner(is);
        JSONObject credsjson = new JSONObject(scanner.useDelimiter("\\A").next());

        InputStream helpIS = instance.getClass().getClassLoader().getResourceAsStream("help.txt");
        BufferedReader in = new BufferedReader(new InputStreamReader(helpIS));

        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            helpMsg = helpMsg + inputLine + "\n";
        }
        in.close();
        helpMsg = helpMsg.replace("%CLIENT_ID%", CLIENT_ID);
        helpMsg = helpMsg.replace("%SHRUG%", "¯\\_(ツ)_/¯");

        //accountEmail = credsjson.getString(ACCOUNT_EMAIL_KEY);
        //accountPassword = credsjson.getString(ACCOUNT_PASSWORD_KEY);
        accountToken = credsjson.getString(ACCOUNT_TOKEN_KEY);
        mashapeKey = credsjson.getString("mashapeKey");
        String cbUser = credsjson.getString("cbUser");
        String cbKey = credsjson.getString("cbKey");
        String accountEmail = credsjson.getString("email");
        String accountPassword = credsjson.getString("password");

        if (credsjson.has("scopePasswords")) {
            JSONObject scopePasswords = credsjson.getJSONObject("scopePasswords");
            for (String k : scopePasswords.keySet()) {
                scopePasswords.put(k, scopePasswords.getString(k));
            }
        }

        scanner.close();

        //Initialise event listeners
        listenerBot = new EventListenerBoat(0x01, PREFIX);
        listenerSelf = new EventListenerSelf(0x10, SELF_PREFIX);

        fredboat.util.HttpUtils.init();
        jdaBot = new JDABuilder().addListener(listenerBot).setBotToken(accountToken).buildAsync();
        jdaSelf = new JDAClientBuilder().addListener(listenerSelf).setEmail(accountEmail).setPassword(accountPassword).buildAsync();
        System.out.println("JDA version:\t" + JDAInfo.VERSION);

        //Initialise JCA
        jca = new JCABuilder().setKey(cbKey).setUser(cbUser).buildBlocking();
    }

    public static void init() {
        readyEvents = readyEvents + 1;

        System.out.println("INIT: " + readyEvents);

        if (readyEvents < READY_EVENTS_REQUIRED) {
            return;
        }

        if (IS_BETA) {
            helpMsg = helpMsg + "\n\n**This is the beta version of Fredboat. Are you sure you are not looking for the non-beta version \"FredBoat\"?**";
        }

        for (Guild guild : jdaBot.getGuilds()) {
            System.out.println(guild.getName());

            for (TextChannel channel : guild.getTextChannels()) {
                System.out.println("\t" + channel.getName());
            }
        }

        myUserId = jdaBot.getSelfInfo().getId();
        myUser = jdaBot.getUserById(myUserId);

        //Commands
        CommandRegistry.registerCommand(0x01, "help", new HelpCommand());
        CommandRegistry.registerCommand(0x11, "dbget", new DBGetCommand());
        CommandRegistry.registerCommand(0x11, "say", new SayCommand());
        CommandRegistry.registerCommand(0x11, "uptime", new UptimeCommand());
        CommandRegistry.registerAlias("uptime", "stats");
        CommandRegistry.registerCommand(0x11, "exit", new ExitCommand());
        CommandRegistry.registerCommand(0x11, "avatar", new AvatarCommand());
        CommandRegistry.registerCommand(0x11, "test", new TestCommand());
        CommandRegistry.registerCommand(0x11, "lua", new LuaCommand());
        CommandRegistry.registerCommand(0x11, "brainfuck", new BrainfuckCommand());
        CommandRegistry.registerCommand(0x11, "joke", new JokeCommand());
        CommandRegistry.registerCommand(0x11, "leet", new LeetCommand());
        CommandRegistry.registerAlias("leet", "1337");
        CommandRegistry.registerAlias("leet", "l33t");
        //CommandRegistry.registerCommand(0x11, "troll", new AudioTrollCommand());
        //CommandRegistry.registerCommand(0x11, "endtroll", new EndTrollCommand());
        CommandRegistry.registerCommand(0x11, "restart", new RestartCommand());
        CommandRegistry.registerCommand(0x11, "find", new FindCommand());
        CommandRegistry.registerCommand(0x11, "dance", new DanceCommand());
        CommandRegistry.registerCommand(0x11, "mafia", new MafiaStartCommand());
        CommandRegistry.registerAlias("mafia", "startmafia");
        CommandRegistry.registerAlias("mafia", "werewolf");
        CommandRegistry.registerAlias("mafia", "startwerewolf");
        CommandRegistry.registerCommand(0x11, "eval", new EvalCommand());
        CommandRegistry.registerCommand(0x11, "s", new TextCommand("¯\\_(ツ)_/¯"));
        CommandRegistry.registerCommand(0x11, "lenny", new TextCommand("( ͡° ͜ʖ ͡°)"));
        CommandRegistry.registerCommand(0x11, "clear", new ClearCommand());
        CommandRegistry.registerCommand(0x11, "talk", new TalkCommand());
    }
}
