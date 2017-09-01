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
import fredboat.command.moderation.ConfigCommand;
import fredboat.command.moderation.LanguageCommand;
import fredboat.command.moderation.PermissionsCommand;
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
import fredboat.command.util.UserInfoCommand;
import fredboat.commandmeta.CommandRegistry;
import fredboat.perms.PermissionLevel;
import fredboat.shared.constant.DistributionEnum;
import fredboat.util.rest.SearchUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MusicCommandInitializer {

    private static final Logger log = LoggerFactory.getLogger(MusicCommandInitializer.class);

    public static void initCommands() {
        CommandRegistry.registerCommand("help", new HelpCommand(), "info");
        CommandRegistry.registerCommand("music", new MusicHelpCommand(), "musichelp");
        CommandRegistry.registerCommand("commands", new CommandsCommand(), "comms", "cmds");
        
        /* Control */
        CommandRegistry.registerCommand("play", new PlayCommand(SearchUtil.SearchProvider.YOUTUBE, SearchUtil.SearchProvider.SOUNDCLOUD), "p");
        CommandRegistry.registerCommand("yt", new PlayCommand(SearchUtil.SearchProvider.YOUTUBE), "youtube");
        CommandRegistry.registerCommand("sc", new PlayCommand(SearchUtil.SearchProvider.SOUNDCLOUD), "soundcloud");
        CommandRegistry.registerCommand("skip", new SkipCommand(), "sk", "s");
        CommandRegistry.registerCommand("join", new JoinCommand(), "summon", "jn");
        CommandRegistry.registerCommand("leave", new LeaveCommand(), "lv");
        CommandRegistry.registerCommand("select", new SelectCommand(), "sel");
        CommandRegistry.registerCommand("stop", new StopCommand(), "st");
        CommandRegistry.registerCommand("pause", new PauseCommand(), "pa", "ps");
        CommandRegistry.registerCommand("shuffle", new ShuffleCommand(), "sh");
        CommandRegistry.registerCommand("reshuffle", new ReshuffleCommand(), "resh");
        CommandRegistry.registerCommand("repeat", new RepeatCommand(), "rep");
        CommandRegistry.registerCommand("volume", new VolumeCommand(), "vol");
        CommandRegistry.registerCommand("unpause", new UnpauseCommand(), "unp", "resume");
        CommandRegistry.registerCommand("split", new PlaySplitCommand());
        CommandRegistry.registerCommand("destroy", new DestroyCommand());
        
        /* Info */
        CommandRegistry.registerCommand("nowplaying", new NowplayingCommand(), "np");
        CommandRegistry.registerCommand("list", new ListCommand(), "queue", "q", "l");
        CommandRegistry.registerCommand("export", new ExportCommand(), "ex");
        CommandRegistry.registerCommand("gr", new GensokyoRadioCommand(), "gensokyo", "gensokyoradio");
        CommandRegistry.registerCommand("muserinfo", new UserInfoCommand());

        /* Seeking */
        CommandRegistry.registerCommand("seek", new SeekCommand());
        CommandRegistry.registerCommand("forward", new ForwardCommand(), "fwd");
        CommandRegistry.registerCommand("rewind", new RewindCommand(), "rew");
        CommandRegistry.registerCommand("restart", new RestartCommand());
        
        /* Bot Maintenance Commands */
        CommandRegistry.registerCommand("mgitinfo", new GitInfoCommand(), "mgit");
        CommandRegistry.registerCommand("munblacklist", new UnblacklistCommand(), "munlimit");
        CommandRegistry.registerCommand("mexit", new ExitCommand());
        CommandRegistry.registerCommand("mbotrestart", new BotRestartCommand());
        CommandRegistry.registerCommand("mstats", new StatsCommand());
        CommandRegistry.registerCommand("meval", new EvalCommand());
        CommandRegistry.registerCommand("mupdate", new UpdateCommand());
        CommandRegistry.registerCommand("mcompile", new CompileCommand());
        CommandRegistry.registerCommand("mmvntest", new MavenTestCommand());
        CommandRegistry.registerCommand("getid", new GetIdCommand());
        CommandRegistry.registerCommand("playerdebug", new PlayerDebugCommand());
        CommandRegistry.registerCommand("nodes", new NodesCommand());
        CommandRegistry.registerCommand("mshards", new ShardsCommand());
        CommandRegistry.registerCommand("mrevive", new ReviveCommand());
        CommandRegistry.registerCommand("adebug", new AudioDebugCommand());
        CommandRegistry.registerCommand("announce", new AnnounceCommand());
        CommandRegistry.registerCommand("mping", new PingCommand());
        CommandRegistry.registerCommand("node", new NodeAdminCommand());
        
        /* Bot configuration */
        CommandRegistry.registerCommand("config", new ConfigCommand(), "cfg");
        CommandRegistry.registerCommand("lang", new LanguageCommand(), "language");
        
        /* Perms */
        CommandRegistry.registerCommand("admin", new PermissionsCommand(PermissionLevel.ADMIN));
        CommandRegistry.registerCommand("dj", new PermissionsCommand(PermissionLevel.DJ));
        CommandRegistry.registerCommand("user", new PermissionsCommand(PermissionLevel.USER));

        // The null check is to ensure we can run this in a test run
        if (Config.CONFIG == null || Config.CONFIG.getDistribution() != DistributionEnum.PATRON) {
            new VoiceChannelCleanupAgent().start();
        } else {
            log.info("Skipped setting up the VoiceChannelCleanupAgent since we are running as PATRON distribution.");
        }
    }

}
