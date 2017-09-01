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
 */

package fredboat.commandmeta.init;

import fredboat.command.admin.*;
import fredboat.command.fun.*;
import fredboat.command.maintenance.*;
import fredboat.command.moderation.ClearCommand;
import fredboat.command.moderation.HardbanCommand;
import fredboat.command.moderation.KickCommand;
import fredboat.command.moderation.SoftbanCommand;
import fredboat.command.util.*;
import fredboat.commandmeta.CommandRegistry;
import fredboat.util.AsciiArtConstant;

public class MainCommandInitializer {

    public static void initCommands() {
        CommandRegistry.registerCommand("help", new HelpCommand(), "info");
        CommandRegistry.registerCommand("commands", new CommandsCommand(), "comms", "cmds");
        CommandRegistry.registerCommand("invite", new InviteCommand());      
        
        /* Bot Maintenance */
        CommandRegistry.registerCommand("unblacklist", new UnblacklistCommand(), "unlimit");
        CommandRegistry.registerCommand("version", new VersionCommand());
        CommandRegistry.registerCommand("uptime", new StatsCommand(), "stats");
        CommandRegistry.registerCommand("update", new UpdateCommand());
        CommandRegistry.registerCommand("compile", new CompileCommand());
        CommandRegistry.registerCommand("mvntest", new MavenTestCommand());
        CommandRegistry.registerCommand("botrestart", new BotRestartCommand());
        CommandRegistry.registerCommand("eval", new EvalCommand());
        CommandRegistry.registerCommand("shards", new ShardsCommand());
        CommandRegistry.registerCommand("revive", new ReviveCommand());
        CommandRegistry.registerCommand("test", new TestCommand());
        CommandRegistry.registerCommand("gitinfo", new GitInfoCommand(), "git");
        CommandRegistry.registerCommand("exit", new ExitCommand());
        
        /* Moderation */
        CommandRegistry.registerCommand("hardban", new HardbanCommand());
        CommandRegistry.registerCommand("kick", new KickCommand());
        CommandRegistry.registerCommand("softban", new SoftbanCommand());
        CommandRegistry.registerCommand("clear", new ClearCommand());
        
        /* Util */
        CommandRegistry.registerCommand("serverinfo", new fredboat.command.util.ServerInfoCommand(), "guildinfo");
        CommandRegistry.registerCommand("userinfo", new fredboat.command.util.UserInfoCommand(), "memberinfo");
        CommandRegistry.registerCommand("ping", new PingCommand());
        CommandRegistry.registerCommand("fuzzy", new FuzzyUserSearchCommand());
        
        /* Fun Commands */
        CommandRegistry.registerCommand("joke", new JokeCommand(), "jk");
        //TODO LeetCommand is borken. Don't throw unnecessary error reports until it's fixed or removed.
//        CommandRegistry.registerCommand("leet", new LeetCommand(), "1337", "l33t", "1ee7");
        CommandRegistry.registerCommand("riot", new RiotCommand());
        CommandRegistry.registerCommand("dance", new DanceCommand());
        CommandRegistry.registerCommand("talk", new TalkCommand());
        CommandRegistry.registerCommand("akinator", new AkinatorCommand());
        CommandRegistry.registerCommand("catgirl", new CatgirlCommand(), "neko", "catgrill");
        CommandRegistry.registerCommand("avatar", new AvatarCommand(), "ava");
        CommandRegistry.registerCommand("say", new SayCommand());

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
        CommandRegistry.registerCommand("powerpoint", new RemoteFileCommand("http://i.imgur.com/i65ss6p.png"));
        
        /* Text Faces & Unicode 'Art' & ASCII 'Art' and Stuff */
        CommandRegistry.registerCommand("shr", new TextCommand("¯\\_(ツ)_/¯"), "shrug");
        CommandRegistry.registerCommand("faceofdisapproval", new TextCommand("ಠ_ಠ"), "fod", "disapproving");
        CommandRegistry.registerCommand("sendenergy", new TextCommand("༼ つ ◕_◕ ༽つ"));
        CommandRegistry.registerCommand("dealwithit", new TextCommand("(•\\_•) ( •\\_•)>⌐■-■ (⌐■_■)"), "dwi");
        CommandRegistry.registerCommand("channelingenergy", new TextCommand("(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧ ✧ﾟ･: *ヽ(◕ヮ◕ヽ)"));
        CommandRegistry.registerCommand("butterfly", new TextCommand("Ƹ̵̡Ӝ̵̨̄Ʒ"));
        CommandRegistry.registerCommand("angrytableflip", new TextCommand("(ノಠ益ಠ)ノ彡┻━┻"), "tableflipbutangry", "atp");
        CommandRegistry.registerCommand("dog", new TextCommand(AsciiArtConstant.DOG), "cooldog", "dogmeme");
        CommandRegistry.registerCommand("lood", new TextCommand("T-that's l-lewd, baka!!!"), "lewd", "l00d");
        CommandRegistry.registerCommand("useless", new TextCommand("This command is useless."));
        CommandRegistry.registerCommand("swtf", new TextCommand("¯\\\\(°_o)/¯"), "shrugwtf");
        CommandRegistry.registerCommand("hurray", new TextCommand("ヽ(^o^)ノ"), "yay", "woot");
        // Lennies
        CommandRegistry.registerCommand("spiderlenny", new TextCommand("/╲/╭( ͡° ͡° ͜ʖ ͡° ͡°)╮/╱\\"));
        CommandRegistry.registerCommand("lenny", new TextCommand("( ͡° ͜ʖ ͡°)"));
        CommandRegistry.registerCommand("peeking", new TextCommand("┬┴┬┴┤ ͜ʖ ͡°) ├┬┴┬┴"), "peekinglenny", "peek");
        CommandRegistry.registerCommand("magicallenny", new TextCommand(AsciiArtConstant.MAGICAL_LENNY), "lennymagical");
        CommandRegistry.registerCommand("eagleoflenny", new TextCommand(AsciiArtConstant.EAGLE_OF_LENNY), "eol", "lennyeagle");

        /* Misc - All commands under this line fall in this category */
        
        CommandRegistry.registerCommand("mal", new MALCommand());
        CommandRegistry.registerCommand("brainfuck", new BrainfuckCommand());

        CommandRegistry.registerCommand("github", new TextCommand("https://github.com/Frederikam"));
        CommandRegistry.registerCommand("repo", new TextCommand("https://github.com/Frederikam/FredBoat"));

        CommandRegistry.registerCommand("hug", new HugCommand("https://imgur.com/a/jHJOc"));
        CommandRegistry.registerCommand("pat", new PatCommand("https://imgur.com/a/WiPTl"));
        CommandRegistry.registerCommand("facedesk", new FacedeskCommand("https://imgur.com/a/I5Q4U"));
        CommandRegistry.registerCommand("roll", new RollCommand("https://imgur.com/a/lrEwS"));
    }

}
