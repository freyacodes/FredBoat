package fredboat.commandmeta.init;

import fredboat.agent.VoiceChannelCleanupAgent;
import fredboat.command.admin.BotRestartCommand;
import fredboat.command.admin.CompileCommand;
import fredboat.command.admin.EvalCommand;
import fredboat.command.admin.ExitCommand;
import fredboat.command.admin.MavenTestCommand;
import fredboat.command.admin.PlayerDebugCommand;
import fredboat.command.admin.ReviveCommand;
import fredboat.command.admin.UpdateCommand;
import fredboat.command.maintenance.AudioDebugCommand;
import fredboat.command.maintenance.GetIdCommand;
import fredboat.command.maintenance.NodesCommand;
import fredboat.command.maintenance.ShardsCommand;
import fredboat.command.maintenance.StatsCommand;
import fredboat.command.moderation.ConfigCommand;
import fredboat.command.moderation.LanguageCommand;
import fredboat.command.music.control.JoinCommand;
import fredboat.command.music.control.LeaveCommand;
import fredboat.command.music.control.PauseCommand;
import fredboat.command.music.control.PlayCommand;
import fredboat.command.music.control.PlaySplitCommand;
import fredboat.command.music.control.RepeatCommand;
import fredboat.command.music.control.ReshuffleCommand;
import fredboat.command.music.control.SelectCommand;
import fredboat.command.music.control.ShuffleCommand;
import fredboat.command.music.control.SkipCommand;
import fredboat.command.music.control.StopCommand;
import fredboat.command.music.control.UnpauseCommand;
import fredboat.command.music.control.VolumeCommand;
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
import fredboat.util.SearchUtil;

public class MusicCommandInitializer {

    public static void initCommands() {
        CommandRegistry.registerCommand("help", new HelpCommand());
        CommandRegistry.registerAlias("help", "info");

        CommandRegistry.registerCommand("mexit", new ExitCommand());
        CommandRegistry.registerCommand("mbotrestart", new BotRestartCommand());
        CommandRegistry.registerCommand("mstats", new StatsCommand());
        CommandRegistry.registerCommand("play", new PlayCommand(SearchUtil.SearchProvider.YOUTUBE));
        CommandRegistry.registerAlias("play", "yt");
        CommandRegistry.registerCommand("sc", new PlayCommand(SearchUtil.SearchProvider.SOUNDCLOUD));
        CommandRegistry.registerAlias("sc", "soundcloud");
        CommandRegistry.registerCommand("meval", new EvalCommand());
        CommandRegistry.registerCommand("skip", new SkipCommand());
        CommandRegistry.registerCommand("join", new JoinCommand());
        CommandRegistry.registerAlias("join", "summon");
        CommandRegistry.registerCommand("nowplaying", new NowplayingCommand());
        CommandRegistry.registerAlias("nowplaying", "np");
        CommandRegistry.registerCommand("leave", new LeaveCommand());
        CommandRegistry.registerCommand("list", new ListCommand());
        CommandRegistry.registerAlias("list", "queue");
        CommandRegistry.registerCommand("mupdate", new UpdateCommand());
        CommandRegistry.registerCommand("mcompile", new CompileCommand());
        CommandRegistry.registerCommand("mmvntest", new MavenTestCommand());
        CommandRegistry.registerCommand("select", new SelectCommand());
        CommandRegistry.registerCommand("stop", new StopCommand());
        CommandRegistry.registerCommand("pause", new PauseCommand());
        CommandRegistry.registerCommand("unpause", new UnpauseCommand());
        CommandRegistry.registerCommand("getid", new GetIdCommand());
        CommandRegistry.registerCommand("shuffle", new ShuffleCommand());
        CommandRegistry.registerCommand("reshuffle", new ReshuffleCommand());
        CommandRegistry.registerCommand("repeat", new RepeatCommand());
        CommandRegistry.registerCommand("volume", new VolumeCommand());
        CommandRegistry.registerAlias("volume", "vol");
        CommandRegistry.registerCommand("restart", new RestartCommand());
        CommandRegistry.registerCommand("export", new ExportCommand());
        CommandRegistry.registerCommand("playerdebug", new PlayerDebugCommand());
        CommandRegistry.registerCommand("music", new MusicHelpCommand());
        CommandRegistry.registerAlias("music", "musichelp");
        CommandRegistry.registerCommand("commands", new CommandsCommand());
        CommandRegistry.registerAlias("commands", "comms");
        CommandRegistry.registerCommand("nodes", new NodesCommand());
        CommandRegistry.registerCommand("gr", new GensokyoRadioCommand());
        CommandRegistry.registerAlias("gr", "gensokyo");
        CommandRegistry.registerAlias("gr", "gensokyoradio");
        CommandRegistry.registerCommand("mshards", new ShardsCommand());
        CommandRegistry.registerCommand("split", new PlaySplitCommand());
        CommandRegistry.registerCommand("config", new ConfigCommand());
        CommandRegistry.registerCommand("lang", new LanguageCommand());
        CommandRegistry.registerCommand("mrevive", new ReviveCommand());
        CommandRegistry.registerCommand("adebug", new AudioDebugCommand());

        CommandRegistry.registerCommand("seek", new SeekCommand());
        CommandRegistry.registerCommand("forward", new ForwardCommand());
        CommandRegistry.registerAlias("forward", "fwd");
        CommandRegistry.registerCommand("rewind", new RewindCommand());
        CommandRegistry.registerAlias("rewind", "rew");

        new VoiceChannelCleanupAgent().start();
    }

}
