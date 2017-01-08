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

package fredboat.commandmeta;

import fredboat.command.admin.*;
import fredboat.command.fun.*;
import fredboat.command.maintenance.GetIdCommand;
import fredboat.command.maintenance.ShardsCommand;
import fredboat.command.maintenance.StatsCommand;
import fredboat.command.admin.TestCommand;
import fredboat.command.maintenance.VersionCommand;
import fredboat.command.moderation.SoftbanCommand;
import fredboat.command.music.control.*;
import fredboat.command.music.info.ExportCommand;
import fredboat.command.music.info.GensokyoRadioCommand;
import fredboat.command.music.info.ListCommand;
import fredboat.command.music.info.NowplayingCommand;
import fredboat.command.music.seeking.ForwardCommand;
import fredboat.command.music.seeking.RestartCommand;
import fredboat.command.music.seeking.RewindCommand;
import fredboat.command.music.seeking.SeekCommand;
import fredboat.command.util.*;

public class CommandInitializer {

    public static void initCommands() {
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
        CommandRegistry.registerCommand(0x101, "fuzzy", new FuzzyUserSearchCommand());
        CommandRegistry.registerCommand(0x101, "softban", new SoftbanCommand());
        CommandRegistry.registerCommand(0x101, "catgirl", new CatgirlCommand());
        CommandRegistry.registerCommand(0x101, "shards", new ShardsCommand());
        
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
        CommandRegistry.registerCommand(0x010, "music", new MusicHelpCommand());
        CommandRegistry.registerAlias("music", "musichelp");
        CommandRegistry.registerCommand(0x010, "nodes", new NodesCommand());
        CommandRegistry.registerCommand(0x010, "gr", new GensokyoRadioCommand());
        CommandRegistry.registerAlias("gr", "gensokyo");
        CommandRegistry.registerAlias("gr", "gensokyoradio");
        CommandRegistry.registerCommand(0x010, "mshards", new ShardsCommand());

        CommandRegistry.registerCommand(0x010, "seek", new SeekCommand());
        CommandRegistry.registerCommand(0x010, "forward", new ForwardCommand());
        CommandRegistry.registerAlias("forward", "fwd");
        CommandRegistry.registerCommand(0x010, "rewind", new RewindCommand());
        CommandRegistry.registerAlias("rewind", "rew");

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
        CommandRegistry.registerCommand(0x101, "lood", new TextCommand("T-that's l-lewd, baka!!!"));
        CommandRegistry.registerAlias("lood", "lewd");

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
    }

}
