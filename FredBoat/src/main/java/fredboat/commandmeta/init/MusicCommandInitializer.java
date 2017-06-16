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

import fredboat.Config;
import fredboat.agent.VoiceChannelCleanupAgent;
import fredboat.command.admin.*;
import fredboat.command.maintenance.*;
import fredboat.command.config.ConfigCommand;
import fredboat.command.config.LanguageCommand;
import fredboat.command.music.control.*;
import fredboat.command.music.info.ExportCommand;
import fredboat.command.music.info.GensokyoRadioCommand;
import fredboat.command.music.info.ListCommand;
import fredboat.command.music.info.NowplayingCommand;
import fredboat.command.music.seeking.ForwardCommand;
import fredboat.command.music.seeking.RestartCommand;
import fredboat.command.music.seeking.RewindCommand;
import fredboat.command.music.seeking.SeekCommand;
import fredboat.command.util.CommandsCommand;
import fredboat.command.util.HelpCommand;
import fredboat.command.util.MusicHelpCommand;
import fredboat.commandmeta.CommandRegistry;
import fredboat.util.DistributionEnum;
import fredboat.util.SearchUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MusicCommandInitializer {

    private static final Logger log = LoggerFactory.getLogger(MusicCommandInitializer.class);

    public static void initCommands() {
        CommandRegistry.registerCommand(new HelpCommand(), "help", "info");

        CommandRegistry.registerCommand(new GitInfoCommand(), "mgitinfo", "mgit");
        CommandRegistry.registerCommand(new UnblacklistCommand(), "munblacklist", "munlimit");
        CommandRegistry.registerCommand(new ExitCommand(), "mexit");
        CommandRegistry.registerCommand(new BotRestartCommand(), "mbotrestart");
        CommandRegistry.registerCommand(new StatsCommand(), "mstats");
        CommandRegistry.registerCommand(new PlayCommand(SearchUtil.SearchProvider.YOUTUBE), "play", "yt");
        CommandRegistry.registerCommand(new PlayCommand(SearchUtil.SearchProvider.SOUNDCLOUD), "sc", "soundcloud");
        CommandRegistry.registerCommand(new EvalCommand(), "meval");
        CommandRegistry.registerCommand(new SkipCommand(), "skip");
        CommandRegistry.registerCommand(new JoinCommand(), "join", "summon");
        CommandRegistry.registerCommand(new NowplayingCommand(), "nowplaying", "np");
        CommandRegistry.registerCommand(new LeaveCommand(), "leave");
        CommandRegistry.registerCommand(new ListCommand(), "list", "queue");
        CommandRegistry.registerCommand(new UpdateCommand(), "mupdate");
        CommandRegistry.registerCommand(new CompileCommand(), "mcompile");
        CommandRegistry.registerCommand(new MavenTestCommand(), "mmvntest");
        CommandRegistry.registerCommand(new SelectCommand(), "select");
        CommandRegistry.registerCommand(new StopCommand(), "stop");
        CommandRegistry.registerCommand(new PauseCommand(), "pause");
        CommandRegistry.registerCommand(new UnpauseCommand(), "unpause");
        CommandRegistry.registerCommand(new GetIdCommand(), "getid");
        CommandRegistry.registerCommand(new ShuffleCommand(), "shuffle");
        CommandRegistry.registerCommand(new ReshuffleCommand(), "reshuffle");
        CommandRegistry.registerCommand(new RepeatCommand(), "repeat");
        CommandRegistry.registerCommand(new VolumeCommand(), "volume", "vol");
        CommandRegistry.registerCommand(new RestartCommand(), "restart");
        CommandRegistry.registerCommand(new ExportCommand(), "export");
        CommandRegistry.registerCommand(new PlayerDebugCommand(), "playerdebug");
        CommandRegistry.registerCommand(new MusicHelpCommand(), "music", "musichelp");
        CommandRegistry.registerCommand(new CommandsCommand(), "commands", "comms");
        CommandRegistry.registerCommand(new NodesCommand(), "nodes");
        CommandRegistry.registerCommand(new GensokyoRadioCommand(), "gr", "gensokyo", "gensokyoradio");
        CommandRegistry.registerCommand(new ShardsCommand(), "mshards");
        CommandRegistry.registerCommand(new PlaySplitCommand(), "split");
        CommandRegistry.registerCommand(new ConfigCommand(), "config");
        CommandRegistry.registerCommand(new LanguageCommand(), "lang");
        CommandRegistry.registerCommand(new ReviveCommand(), "mrevive");
        CommandRegistry.registerCommand(new AudioDebugCommand(), "adebug");
        CommandRegistry.registerCommand(new AnnounceCommand(), "announce");
        CommandRegistry.registerCommand(new DestroyCommand(), "destroy");

        CommandRegistry.registerCommand(new SeekCommand(), "seek");
        CommandRegistry.registerCommand(new ForwardCommand(), "forward", "fwd");
        CommandRegistry.registerCommand(new RewindCommand(), "rewind", "rew");

        // The null check is to ensure we can run this in a test run
        if (Config.CONFIG == null || Config.CONFIG.getDistribution() != DistributionEnum.PATRON) {
            new VoiceChannelCleanupAgent().start();
        } else {
            log.info("Skipped setting up the VoiceChannelCleanupAgent since we are running as PATRON distribution.");
        }
    }

}
