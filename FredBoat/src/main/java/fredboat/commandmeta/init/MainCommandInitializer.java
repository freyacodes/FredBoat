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

public class MainCommandInitializer {

    public static void initCommands() {
        CommandRegistry.registerCommand("help", new HelpCommand(), "info");

        CommandRegistry.registerCommand("unblacklist", new UnblacklistCommand(), "unlimit");
        CommandRegistry.registerCommand("commands", new CommandsCommand(), "comms");
        CommandRegistry.registerCommand("version", new VersionCommand());
        CommandRegistry.registerCommand("say", new SayCommand());
        CommandRegistry.registerCommand("uptime", new StatsCommand(), "stats");
        CommandRegistry.registerCommand("serverinfo", new fredboat.command.util.ServerInfoCommand(), "guildinfo");
        CommandRegistry.registerCommand("invite", new InviteCommand());
        CommandRegistry.registerCommand("userinfo", new fredboat.command.util.UserInfoCommand(), "memberinfo");
        CommandRegistry.registerCommand("gitinfo", new GitInfoCommand(), "git");
        CommandRegistry.registerCommand("exit", new ExitCommand());
        CommandRegistry.registerCommand("avatar", new AvatarCommand());
        CommandRegistry.registerCommand("test", new TestCommand());
        CommandRegistry.registerCommand("brainfuck", new BrainfuckCommand());
        CommandRegistry.registerCommand("joke", new JokeCommand());
        //TODO LeetCommand is borken. Don't throw unnecessary error reports until it's fixed or removed.
//        CommandRegistry.registerCommand("leet", new LeetCommand(), "1337", "l33t", "1ee7");
        CommandRegistry.registerCommand("riot", new RiotCommand());
        CommandRegistry.registerCommand("update", new UpdateCommand());
        CommandRegistry.registerCommand("compile", new CompileCommand());
        CommandRegistry.registerCommand("mvntest", new MavenTestCommand());
        CommandRegistry.registerCommand("botrestart", new BotRestartCommand());
        CommandRegistry.registerCommand("dance", new DanceCommand());
        CommandRegistry.registerCommand("eval", new EvalCommand());
        CommandRegistry.registerCommand("s", new TextCommand("¯\\_(ツ)_/¯"), "shrug");
        CommandRegistry.registerCommand("lenny", new TextCommand("( ͡° ͜ʖ ͡°)"));
        CommandRegistry.registerCommand("useless", new TextCommand("This command is useless."));
        CommandRegistry.registerCommand("clear", new ClearCommand());
        CommandRegistry.registerCommand("talk", new TalkCommand());
        CommandRegistry.registerCommand("mal", new MALCommand());
        CommandRegistry.registerCommand("akinator", new AkinatorCommand());
        CommandRegistry.registerCommand("fuzzy", new FuzzyUserSearchCommand());
        CommandRegistry.registerCommand("hardban", new HardbanCommand());
        CommandRegistry.registerCommand("kick", new KickCommand());
        CommandRegistry.registerCommand("softban", new SoftbanCommand());
        CommandRegistry.registerCommand("catgirl", new CatgirlCommand(), "neko", "catgrill");
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
        CommandRegistry.registerCommand("powerpoint", new RemoteFileCommand("http://i.imgur.com/i65ss6p.png"));
        CommandRegistry.registerCommand("cooldog", new DogCommand(), "dog", "dogmeme");
        CommandRegistry.registerCommand("lood", new TextCommand("T-that's l-lewd, baka!!!"), "lewd");

        CommandRegistry.registerCommand("github", new TextCommand("https://github.com/Frederikam"));
        CommandRegistry.registerCommand("repo", new TextCommand("https://github.com/Frederikam/FredBoat"));

        CommandRegistry.registerCommand("hug", new HugCommand("https://imgur.com/a/jHJOc"));
        CommandRegistry.registerCommand("pat", new PatCommand("https://imgur.com/a/WiPTl"));
        CommandRegistry.registerCommand("facedesk", new FacedeskCommand("https://imgur.com/a/I5Q4U"));
        CommandRegistry.registerCommand("roll", new RollCommand("https://imgur.com/a/lrEwS"));
    }

}
