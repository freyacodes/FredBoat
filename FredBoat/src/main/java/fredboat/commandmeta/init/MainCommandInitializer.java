package fredboat.commandmeta.init;

import fredboat.command.admin.*;
import fredboat.command.fun.*;
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
        CommandRegistry.registerCommand("serverinfo",new fredboat.command.util.ServerInfoCommand());
        CommandRegistry.registerAlias("serverinfo", "guildinfo");
        CommandRegistry.registerCommand("invite", new InviteCommand());
        CommandRegistry.registerCommand("userinfo",new fredboat.command.util.UserInfoCommand());
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
        CommandRegistry.registerCommand("ram", new RemoteFileCommand("http://i.imgur.com/jeGVLk3.jpg"));
        CommandRegistry.registerCommand("welcome", new RemoteFileCommand("http://i.imgur.com/yjpmmBk.gif"));
        CommandRegistry.registerCommand("rude", new RemoteFileCommand("http://i.imgur.com/pUn7ijx.png"));
        CommandRegistry.registerCommand("fuck", new RemoteFileCommand("http://i.imgur.com/1bllKNh.png"));
        CommandRegistry.registerCommand("idc", new RemoteFileCommand("http://i.imgur.com/0ZPjpNg.png"));
        CommandRegistry.registerCommand("beingraped", new RemoteFileCommand("http://i.imgur.com/dPsYRYV.png"));
        CommandRegistry.registerCommand("anime", new RemoteFileCommand("https://cdn.discordapp.com/attachments/132490115137142784/177751190333816834/animeeee.png"));
        CommandRegistry.registerCommand("wow", new RemoteFileCommand("http://img.prntscr.com/img?url=http://i.imgur.com/aexiMAG.png"));
        CommandRegistry.registerCommand("what", new RemoteFileCommand("http://i.imgur.com/CTLraK4.png"));
        CommandRegistry.registerCommand("pun", new RemoteFileCommand("http://i.imgur.com/2hFMrjt.png"));
        CommandRegistry.registerCommand("die", new RemoteFileCommand("http://nekomata.moe/i/k2B0f6.png"));
        CommandRegistry.registerCommand("stupid", new RemoteFileCommand("http://nekomata.moe/i/c056y7.png"));
        CommandRegistry.registerCommand("cancer", new RemoteFileCommand("http://puu.sh/oQN2j/8e09872842.jpg"));
        CommandRegistry.registerCommand("stupidbot", new RemoteFileCommand("https://cdn.discordapp.com/attachments/143976784545841161/183171963399700481/unknown.png"));
        CommandRegistry.registerCommand("escape", new RemoteFileCommand("http://i.imgur.com/kk7Zu3C.png"));
        CommandRegistry.registerCommand("explosion", new RemoteFileCommand("https://cdn.discordapp.com/attachments/143976784545841161/182893975965794306/megumin7.gif"));
        CommandRegistry.registerCommand("gif", new RemoteFileCommand("https://cdn.discordapp.com/attachments/132490115137142784/182907929765085185/spacer.gif"));
        CommandRegistry.registerCommand("noods", new RemoteFileCommand("http://i.imgur.com/CUE3gm2.png"));
        CommandRegistry.registerCommand("internetspeed", new RemoteFileCommand("http://www.speedtest.net/result/5529046933.png"));
        CommandRegistry.registerCommand("hug", new RemoteFileCommand("http://i.imgur.com/U2l7mnr.gif"));
        CommandRegistry.registerCommand("powerpoint", new RemoteFileCommand("http://puu.sh/rISIl/1cc927ece3.PNG"));
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
