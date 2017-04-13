package fredboat.commandmeta.init;

import fredboat.command.admin.*;
import fredboat.command.fun.*;
import fredboat.command.maintenance.FuzzyUserSearchCommand;
import fredboat.command.maintenance.ShardsCommand;
import fredboat.command.maintenance.StatsCommand;
import fredboat.command.maintenance.VersionCommand;
import fredboat.command.moderation.ClearCommand;
import fredboat.command.moderation.SoftbanCommand;
import fredboat.command.util.*;
import fredboat.commandmeta.CommandRegistry;

public class MainCommandInitializer {

    public static void initCommands() {
        CommandRegistry.registerCommand("help", new HelpCommand());
        CommandRegistry.registerAlias("help", "info");

        CommandRegistry.registerCommand("commands", new CommandsCommand());
        CommandRegistry.registerAlias("commands", "comms");
        CommandRegistry.registerCommand("version", new VersionCommand());
        CommandRegistry.registerCommand("say", new SayCommand());
        CommandRegistry.registerCommand("uptime", new StatsCommand());
        CommandRegistry.registerCommand("serverinfo", new fredboat.command.util.ServerInfoCommand());
        CommandRegistry.registerAlias("serverinfo", "guildinfo");
        CommandRegistry.registerCommand("invite", new InviteCommand());
        CommandRegistry.registerCommand("userinfo", new fredboat.command.util.UserInfoCommand());
        CommandRegistry.registerAlias("userinfo", "memberinfo");
        CommandRegistry.registerAlias("uptime", "stats");
        CommandRegistry.registerCommand("exit", new ExitCommand());
        CommandRegistry.registerCommand("avatar", new AvatarCommand());
        CommandRegistry.registerCommand("test", new TestCommand());
        CommandRegistry.registerCommand("brainfuck", new BrainfuckCommand());
        CommandRegistry.registerCommand("joke", new JokeCommand());
        CommandRegistry.registerCommand("leet", new LeetCommand());
        CommandRegistry.registerAlias("leet", "1337");
        CommandRegistry.registerAlias("leet", "l33t");
        CommandRegistry.registerAlias("leet", "1ee7");
        CommandRegistry.registerCommand("riot", new RiotCommand());
        CommandRegistry.registerCommand("update", new UpdateCommand());
        CommandRegistry.registerCommand("compile", new CompileCommand());
        CommandRegistry.registerCommand("mvntest", new MavenTestCommand());
        CommandRegistry.registerCommand("botrestart", new BotRestartCommand());
        CommandRegistry.registerCommand("dance", new DanceCommand());
        CommandRegistry.registerCommand("eval", new EvalCommand());
        CommandRegistry.registerCommand("s", new TextCommand("¯\\_(ツ)_/¯"));
        CommandRegistry.registerAlias("s", "shrug");
        CommandRegistry.registerCommand("lenny", new TextCommand("( ͡° ͜ʖ ͡°)"));
        CommandRegistry.registerCommand("useless", new TextCommand("This command is useless."));
        CommandRegistry.registerCommand("clear", new ClearCommand());
        CommandRegistry.registerCommand("talk", new TalkCommand());
        CommandRegistry.registerCommand("mal", new MALCommand());
        CommandRegistry.registerCommand("akinator", new AkinatorCommand());
        CommandRegistry.registerCommand("fuzzy", new FuzzyUserSearchCommand());
        CommandRegistry.registerCommand("softban", new SoftbanCommand());
        CommandRegistry.registerCommand("catgirl", new CatgirlCommand());
        CommandRegistry.registerCommand("shards", new ShardsCommand());
        CommandRegistry.registerCommand("revive", new ReviveCommand());

        /* Other Anime Discord, Sergi memes or any other memes */
        // saved in this album https://imgur.com/a/wYvDu
        CommandRegistry.registerCommand("ram", new RemoteFileCommand("http://i.imgur.com/DYToB2e.jpg"));
        CommandRegistry.registerCommand("welcome", new RemoteFileCommand("http://i.imgur.com/utPRe0e.gif"));
        CommandRegistry.registerCommand("rude", new RemoteFileCommand("http://i.imgur.com/j8VvjOT.png"));
        CommandRegistry.registerCommand("fuck", new RemoteFileCommand("http://i.imgur.com/oJL7m7m.png"));
        CommandRegistry.registerCommand("idc", new RemoteFileCommand("http://i.imgur.com/BrCCbfx.png"));
        CommandRegistry.registerCommand("beingraped", new RemoteFileCommand("http://i.imgur.com/jjoz783.png"));
        CommandRegistry.registerCommand("anime", new RemoteFileCommand("http://i.imgur.com/93VahIh.png"));
        CommandRegistry.registerCommand("wow", new RemoteFileCommand("http://i.imgur.com/w7x1885.png"));
        CommandRegistry.registerCommand("what", new RemoteFileCommand("http://i.imgur.com/GNsAxkh.png"));
        CommandRegistry.registerCommand("pun", new RemoteFileCommand("http://i.imgur.com/sBfq3wM.png"));
        CommandRegistry.registerCommand("cancer", new RemoteFileCommand("http://i.imgur.com/pQiT26t.jpg"));
        CommandRegistry.registerCommand("stupidbot", new RemoteFileCommand("http://i.imgur.com/YT1Bkhj.png"));
        CommandRegistry.registerCommand("escape", new RemoteFileCommand("http://i.imgur.com/QmI469j.png"));
        CommandRegistry.registerCommand("explosion", new RemoteFileCommand("http://i.imgur.com/qz6g1vj.gif"));
        CommandRegistry.registerCommand("gif", new RemoteFileCommand("http://i.imgur.com/eBUFNJq.gif"));
        CommandRegistry.registerCommand("noods", new RemoteFileCommand("http://i.imgur.com/mKdTGlg.png"));
        CommandRegistry.registerCommand("internetspeed", new RemoteFileCommand("http://i.imgur.com/84nbpQe.png"));
        CommandRegistry.registerCommand("hug", new RemoteFileCommand("http://i.imgur.com/E8zQ4yX.gif"));
        CommandRegistry.registerCommand("powerpoint", new RemoteFileCommand("http://i.imgur.com/i65ss6p.png"));
        CommandRegistry.registerCommand("cooldog", new DogCommand());
        CommandRegistry.registerAlias("cooldog", "dog");
        CommandRegistry.registerAlias("cooldog", "dogmeme");
        CommandRegistry.registerCommand("lood", new TextCommand("T-that's l-lewd, baka!!!"));
        CommandRegistry.registerAlias("lood", "lewd");

        CommandRegistry.registerCommand("github", new TextCommand("https://github.com/Frederikam"));
        CommandRegistry.registerCommand("repo", new TextCommand("https://github.com/Frederikam/FredBoat"));

        CommandRegistry.registerCommand("pat", new PatCommand("https://imgur.com/a/WiPTl"));
        CommandRegistry.registerCommand("facedesk", new FacedeskCommand("https://imgur.com/a/I5Q4U"));
        CommandRegistry.registerCommand("roll", new RollCommand("https://imgur.com/a/lrEwS"));
    }

}
