package fredboat.commandmeta.init;

import fredboat.command.admin.*;
import fredboat.command.fun.*;
import fredboat.command.maintenance.ShardsCommand;
import fredboat.command.maintenance.StatsCommand;
import fredboat.command.maintenance.VersionCommand;
import fredboat.command.moderation.SoftbanCommand;
import fredboat.command.util.*;
import fredboat.commandmeta.CommandRegistry;

public class MainCommandInitializer {

    public static void initCommands() {
        CommandRegistry.registerCommand("help", new HelpCommand());
        CommandRegistry.registerAlias("help", "info");

        CommandRegistry.registerCommand("version", new VersionCommand());
        CommandRegistry.registerCommand("say", new SayCommand());
        CommandRegistry.registerCommand("uptime", new StatsCommand());
        CommandRegistry.registerCommand("serverinfo",new fredboat.command.util.ServerInfoCommand());
        CommandRegistry.registerCommand("invite", new InviteCommand());
        CommandRegistry.registerCommand("userinfo",new fredboat.command.util.UserInfoCommand());
        CommandRegistry.registerAlias("uptime", "stats");
        CommandRegistry.registerCommand("exit", new ExitCommand());
        CommandRegistry.registerCommand("avatar", new AvatarCommand());
        CommandRegistry.registerCommand("test", new TestCommand());
        CommandRegistry.registerCommand("lua", new LuaCommand());
        CommandRegistry.registerCommand("brainfuck", new BrainfuckCommand());
        CommandRegistry.registerCommand("joke", new JokeCommand());
        CommandRegistry.registerCommand("leet", new LeetCommand());
        CommandRegistry.registerAlias("leet", "1337");
        CommandRegistry.registerAlias("leet", "l33t");
        CommandRegistry.registerAlias("leet", "1ee7");
        CommandRegistry.registerCommand("riot", new RiotCommand());
        CommandRegistry.registerCommand("update", new UpdateCommand());
        CommandRegistry.registerCommand("compile", new CompileCommand());
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

        String[] pats = {
                "http://i.imgur.com/wF1ohrH.gif",
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
        CommandRegistry.registerCommand("pat", new PatCommand(pats));

        String[] facedesk = {
                "http://i862.photobucket.com/albums/ab181/Shadow_Milez/Animu/kuroko-facedesk.gif",
                "http://2.bp.blogspot.com/-Uw0i2Xv8r-M/UhyYzSHIiCI/AAAAAAAAAdg/hcI1-V7Y3A4/s1600/facedesk.gif",
                "http://stream1.gifsoup.com/webroot/animatedgifs/57302_o.gif",
                "http://img.neoseeker.com/mgv/59301/301/26/facedesk_display.gif"
        };

        CommandRegistry.registerCommand("facedesk", new FacedeskCommand(facedesk));

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
                "http://i.imgur.com/s2TL7A8.gif",
                "http://i.imgur.com/vqTAjp5.gif"
        };

        CommandRegistry.registerCommand("roll", new RollCommand(roll));
    }

}
