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
        CommandRegistry.registerCommand(new HelpCommand(), "help", "info");

        CommandRegistry.registerCommand(new UnblacklistCommand(), "unblacklist", "unlimit");
        CommandRegistry.registerCommand(new CommandsCommand(), "commands", "comms");
        CommandRegistry.registerCommand(new VersionCommand(), "version");
        CommandRegistry.registerCommand(new SayCommand(), "say");
        CommandRegistry.registerCommand(new StatsCommand(), "uptime", "stats");
        CommandRegistry.registerCommand(new fredboat.command.util.ServerInfoCommand(), "serverinfo", "guildinfo");
        CommandRegistry.registerCommand(new InviteCommand(), "invite");
        CommandRegistry.registerCommand(new fredboat.command.util.UserInfoCommand(), "userinfo", "memberinfo");
        CommandRegistry.registerCommand(new GitInfoCommand(), "gitinfo", "git");
        CommandRegistry.registerCommand(new ExitCommand(), "exit");
        CommandRegistry.registerCommand(new AvatarCommand(), "avatar");
        CommandRegistry.registerCommand(new TestCommand(), "test");
        CommandRegistry.registerCommand(new BrainfuckCommand(), "brainfuck");
        CommandRegistry.registerCommand(new JokeCommand(), "joke");
        //TODO LeetCommand is borken. Don't throw unnecessary error reports until it's fixed or removed.
//        CommandRegistry.registerCommand(new LeetCommand(), "leet", "1337", "l33t", "1ee7");
        CommandRegistry.registerCommand(new RiotCommand(), "riot");
        CommandRegistry.registerCommand(new UpdateCommand(), "update");
        CommandRegistry.registerCommand(new CompileCommand(), "compile");
        CommandRegistry.registerCommand(new MavenTestCommand(), "mvntest");
        CommandRegistry.registerCommand(new BotRestartCommand(), "botrestart");
        CommandRegistry.registerCommand(new DanceCommand(), "dance");
        CommandRegistry.registerCommand(new EvalCommand(), "eval");
        CommandRegistry.registerCommand(new TextCommand("¯\\_(ツ)_/¯"), "s", "shrug");
        CommandRegistry.registerCommand(new TextCommand("( ͡° ͜ʖ ͡°)"), "lenny");
        CommandRegistry.registerCommand(new TextCommand("This command is useless."), "useless");
        CommandRegistry.registerCommand(new ClearCommand(), "clear");
        CommandRegistry.registerCommand(new TalkCommand(), "talk");
        CommandRegistry.registerCommand(new MALCommand(), "mal");
        CommandRegistry.registerCommand(new AkinatorCommand(), "akinator");
        CommandRegistry.registerCommand(new FuzzyUserSearchCommand(), "fuzzy");
        CommandRegistry.registerCommand(new HardbanCommand(), "hardban");
        CommandRegistry.registerCommand(new KickCommand(), "kick");
        CommandRegistry.registerCommand(new SoftbanCommand(), "softban");
        CommandRegistry.registerCommand(new CatgirlCommand(), "catgirl", "neko", "catgrill");
        CommandRegistry.registerCommand(new ShardsCommand(), "shards");
        CommandRegistry.registerCommand(new ReviveCommand(), "revive");

        /* Other Anime Discord, Sergi memes or any other memes */
        // saved in this album https://imgur.com/a/wYvDu
        CommandRegistry.registerCommand(new RemoteFileCommand("http://i.imgur.com/DYToB2e.jpg"), "ram");
        CommandRegistry.registerCommand(new RemoteFileCommand("http://i.imgur.com/utPRe0e.gif"), "welcome");
        CommandRegistry.registerCommand(new RemoteFileCommand("http://i.imgur.com/j8VvjOT.png"), "rude");
        CommandRegistry.registerCommand(new RemoteFileCommand("http://i.imgur.com/oJL7m7m.png"), "fuck");
        CommandRegistry.registerCommand(new RemoteFileCommand("http://i.imgur.com/BrCCbfx.png"), "idc");
        CommandRegistry.registerCommand(new RemoteFileCommand("http://i.imgur.com/jjoz783.png"), "beingraped");
        CommandRegistry.registerCommand(new RemoteFileCommand("http://i.imgur.com/93VahIh.png"), "anime");
        CommandRegistry.registerCommand(new RemoteFileCommand("http://i.imgur.com/w7x1885.png"), "wow");
        CommandRegistry.registerCommand(new RemoteFileCommand("http://i.imgur.com/GNsAxkh.png"), "what");
        CommandRegistry.registerCommand(new RemoteFileCommand("http://i.imgur.com/sBfq3wM.png"), "pun");
        CommandRegistry.registerCommand(new RemoteFileCommand("http://i.imgur.com/pQiT26t.jpg"), "cancer");
        CommandRegistry.registerCommand(new RemoteFileCommand("http://i.imgur.com/YT1Bkhj.png"), "stupidbot");
        CommandRegistry.registerCommand(new RemoteFileCommand("http://i.imgur.com/QmI469j.png"), "escape");
        CommandRegistry.registerCommand(new RemoteFileCommand("http://i.imgur.com/qz6g1vj.gif"), "explosion");
        CommandRegistry.registerCommand(new RemoteFileCommand("http://i.imgur.com/eBUFNJq.gif"), "gif");
        CommandRegistry.registerCommand(new RemoteFileCommand("http://i.imgur.com/mKdTGlg.png"), "noods");
        CommandRegistry.registerCommand(new RemoteFileCommand("http://i.imgur.com/84nbpQe.png"), "internetspeed");
        CommandRegistry.registerCommand(new RemoteFileCommand("http://i.imgur.com/i65ss6p.png"), "powerpoint");
        CommandRegistry.registerCommand(new DogCommand(), "cooldog", "dog", "dogmeme");
        CommandRegistry.registerCommand(new TextCommand("T-that's l-lewd, baka!!!"), "lood", "lewd");

        CommandRegistry.registerCommand(new TextCommand("https://github.com/Frederikam"), "github");
        CommandRegistry.registerCommand(new TextCommand("https://github.com/Frederikam/FredBoat"), "repo");

        CommandRegistry.registerCommand(new HugCommand("https://imgur.com/a/jHJOc"), "hug");
        CommandRegistry.registerCommand(new PatCommand("https://imgur.com/a/WiPTl"), "pat");
        CommandRegistry.registerCommand(new FacedeskCommand("https://imgur.com/a/I5Q4U"), "facedesk");
        CommandRegistry.registerCommand(new RollCommand("https://imgur.com/a/lrEwS"), "roll");
    }

}
