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

package fredboat.commandmeta

import fredboat.audio.player.PlayerLimiter
import fredboat.audio.player.PlayerRegistry
import fredboat.audio.player.VideoSelectionCache
import fredboat.command.`fun`.*
import fredboat.command.`fun`.img.*
import fredboat.command.admin.*
import fredboat.command.config.*
import fredboat.command.info.*
import fredboat.command.moderation.HardbanCommand
import fredboat.command.moderation.KickCommand
import fredboat.command.moderation.SoftbanCommand
import fredboat.command.moderation.UnbanCommand
import fredboat.command.music.control.*
import fredboat.command.music.info.*
import fredboat.command.music.seeking.ForwardCommand
import fredboat.command.music.seeking.RestartCommand
import fredboat.command.music.seeking.RewindCommand
import fredboat.command.music.seeking.SeekCommand
import fredboat.command.util.*
import fredboat.config.SentryConfiguration
import fredboat.config.property.AppConfig
import fredboat.db.api.GuildSettingsRepository
import fredboat.definitions.Module
import fredboat.definitions.PermissionLevel
import fredboat.definitions.SearchProvider
import fredboat.sentinel.Sentinel
import fredboat.shared.constant.BotConstants
import fredboat.util.AsciiArtConstant
import fredboat.util.rest.TrackSearcher
import fredboat.util.rest.Weather
import fredboat.util.rest.YoutubeAPI
import io.prometheus.client.guava.cache.CacheMetricsCollector
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.util.*
import java.util.function.Supplier

@Service
class CommandInitializer(
        cacheMetrics: CacheMetricsCollector,
        weather: Weather,
        trackSearcher: TrackSearcher,
        videoSelectionCache: VideoSelectionCache,
        sentryConfiguration: SentryConfiguration,
        playerLimiter: PlayerLimiter,
        youtubeAPI: YoutubeAPI,
        sentinel: Sentinel,
        playerRegistry: PlayerRegistry,
        guildSettingsRepository: GuildSettingsRepository,
        appConfig: AppConfig,
        springContext: Supplier<ApplicationContext>
) {

    companion object {
        /** Used for integration testing  */
        var initialized = false

        //the main alias of some commands are reused across the bot. these constants make sure any changes to them dont break things
        const val MODULES_COMM_NAME = "modules"
        const val HELP_COMM_NAME = "help"
        const val SKIP_COMM_NAME = "skip"
        const val COMMANDS_COMM_NAME = "commands"
        const val MUSICHELP_COMM_NAME = "music"
        const val YOUTUBE_COMM_NAME = "youtube"
        const val SOUNDCLOUD_COMM_NAME = "soundcloud"
        const val PREFIX_COMM_NAME = "prefix"
        const val PLAY_COMM_NAME = "play"
        const val CONFIG_COMM_NAME = "config"
        const val LANGUAGE_COMM_NAME = "language"
    }

    init {
        // Administrative Module - always on (as in, essential commands for BOT_ADMINs and BOT_OWNER)
        val adminModule = CommandRegistry(Module.ADMIN)
        adminModule.registerCommand(AnnounceCommand("announce"))
        adminModule.registerCommand(BotRestartCommand("botrestart"))
        adminModule.registerCommand(DisableCommandsCommand("disable"))
        adminModule.registerCommand(DiscordPermissionCommand("discordpermissions", "disperms"))
        adminModule.registerCommand(EnableCommandsCommand("enable"))
        adminModule.registerCommand(EvalCommand({ springContext.get() }, "eval"))
        adminModule.registerCommand(RemoteEvalCommand("reval"))
        adminModule.registerCommand(ExitCommand("exit"))
        adminModule.registerCommand(GetNodeCommand("getnode"))
        adminModule.registerCommand(LeaveServerCommand("leaveserver"))
        adminModule.registerCommand(NodeAdminCommand("node", "nodes"))
        adminModule.registerCommand(ReviveCommand("revive"))
        adminModule.registerCommand(SentryDsnCommand(sentryConfiguration, "sentrydsn"))
        adminModule.registerCommand(SetAvatarCommand("setavatar"))
        adminModule.registerCommand(UnblacklistCommand("unblacklist", "unlimit"))
        adminModule.registerCommand(GuildDebugCommand("guilddebug", "guilddump", "dumpguild", "gdump"))
        adminModule.registerCommand(UnsubCommand("unsub", playerRegistry = playerRegistry))


        // Informational / Debugging / Maintenance - always on
        val infoModule = CommandRegistry(Module.INFO)
        infoModule.registerCommand(CommandsCommand(COMMANDS_COMM_NAME, "comms", "cmds"))
        infoModule.registerCommand(DebugCommand("debug"))
        infoModule.registerCommand(FuzzyUserSearchCommand("fuzzy"))
        infoModule.registerCommand(GetIdCommand("getid"))
        infoModule.registerCommand(GitInfoCommand("gitinfo", "git"))
        infoModule.registerCommand(HelloCommand("hello", "about"))
        infoModule.registerCommand(HelpCommand(HELP_COMM_NAME, "info"))
        infoModule.registerCommand(InviteCommand("invite"))
        infoModule.registerCommand(MusicHelpCommand(MUSICHELP_COMM_NAME, "musichelp"))
        infoModule.registerCommand(PingCommand("ping"))
        infoModule.registerCommand(ShardsCommand("shards"))
        infoModule.registerCommand(StatsCommand("stats", sentinel.selfUser))
        infoModule.registerCommand(TextCommand("https://github.com/Frederikam", "github"))
        infoModule.registerCommand(TextCommand(BotConstants.GITHUB_URL, "repo"))
        infoModule.registerCommand(SentinelsCommand("sentinels", "sentisneks", "slist"))
        infoModule.registerCommand(UptimeCommand("uptime"))


        // Configurational stuff - always on
        val configModule = CommandRegistry(Module.CONFIG)
        configModule.registerCommand(ConfigCommand(CONFIG_COMM_NAME, guildSettingsRepository, "cfg"))
        configModule.registerCommand(LanguageCommand(LANGUAGE_COMM_NAME, "lang"))
        configModule.registerCommand(ModulesCommand("modules", guildSettingsRepository, "module", "mods"))
        configModule.registerCommand(PrefixCommand(guildSettingsRepository, PREFIX_COMM_NAME, "pre"))
        configModule.registerCommand(ConfigWebInfoCommand("webinfo", repo = guildSettingsRepository, appConfig = appConfig))
        /* Perms */
        configModule.registerCommand(PermissionsCommand(PermissionLevel.ADMIN, guildSettingsRepository, "admin", "admins"))
        configModule.registerCommand(PermissionsCommand(PermissionLevel.DJ, guildSettingsRepository, "dj", "djs"))
        configModule.registerCommand(PermissionsCommand(PermissionLevel.USER, guildSettingsRepository, "user", "users"))


        // Moderation Module - Anything related to managing Discord guilds
        val moderationModule = CommandRegistry(Module.MOD)
        moderationModule.registerCommand(HardbanCommand("hardban", "ban", "hb"))
        moderationModule.registerCommand(UnbanCommand("unban"))
        moderationModule.registerCommand(KickCommand("kick"))
        moderationModule.registerCommand(SoftbanCommand("softban", "sb"))


        // Utility Module - Like Fun commands but without the fun ¯\_(ツ)_/¯
        val utilityModule = CommandRegistry(Module.UTIL)
        utilityModule.registerCommand(AvatarCommand("avatar", "ava"))
        utilityModule.registerCommand(BrainfuckCommand("brainfuck"))
        utilityModule.registerCommand(MathCommand("math"))
        utilityModule.registerCommand(RoleInfoCommand("roleinfo"))
        utilityModule.registerCommand(ServerInfoCommand("serverinfo", "guildinfo"))
        utilityModule.registerCommand(UserInfoCommand("userinfo", "memberinfo"))
        utilityModule.registerCommand(WeatherCommand(weather, "weather"))
        utilityModule.registerCommand(CalcShardCommand("calcshard", "cash", "cs"))


        // Fun Module - mostly ascii, memes, pictures, games
        val funModule = CommandRegistry(Module.FUN)
        funModule.registerCommand(TextCommand("Ahoy, the Akinator command has been discontinued on FredBoat, but you can give the Aki bot a try: https://akibot.xyz", "akinator", "aki"))
        funModule.registerCommand(DanceCommand(cacheMetrics, "dance"))
        funModule.registerCommand(JokeCommand("joke", "jk"))
        funModule.registerCommand(RiotCommand("riot"))
        funModule.registerCommand(SayCommand("say"))

        /* Other Anime Discord, Sergi memes or any other memes
           saved in this album https://imgur.com/a/wYvDu        */

        /* antique FredBoat staff insiders */
        funModule.registerCommand(RestrictedRemoteFileCommand(PermissionLevel.BOT_ADMIN, "http://i.imgur.com/j8VvjOT.png", "rude"))
        funModule.registerCommand(RestrictedRemoteFileCommand(PermissionLevel.BOT_ADMIN, "http://i.imgur.com/oJL7m7m.png", "fuck"))
        funModule.registerCommand(RestrictedRemoteFileCommand(PermissionLevel.BOT_ADMIN, "http://i.imgur.com/BrCCbfx.png", "idc"))
        funModule.registerCommand(RestrictedRemoteFileCommand(PermissionLevel.BOT_ADMIN, "http://i.imgur.com/jjoz783.png", "beingraped"))
        funModule.registerCommand(RestrictedRemoteFileCommand(PermissionLevel.BOT_ADMIN, "http://i.imgur.com/w7x1885.png", "wow"))
        funModule.registerCommand(RestrictedRemoteFileCommand(PermissionLevel.BOT_ADMIN, "http://i.imgur.com/GNsAxkh.png", "what"))
        funModule.registerCommand(RestrictedRemoteFileCommand(PermissionLevel.BOT_ADMIN, "http://i.imgur.com/sBfq3wM.png", "pun"))
        funModule.registerCommand(RestrictedRemoteFileCommand(PermissionLevel.BOT_ADMIN, "http://i.imgur.com/pQiT26t.jpg", "cancer"))
        funModule.registerCommand(RestrictedRemoteFileCommand(PermissionLevel.BOT_ADMIN, "http://i.imgur.com/YT1Bkhj.png", "stupidbot"))
        funModule.registerCommand(RestrictedRemoteFileCommand(PermissionLevel.BOT_ADMIN, "http://i.imgur.com/QmI469j.png", "escape"))
        funModule.registerCommand(RestrictedRemoteFileCommand(PermissionLevel.BOT_ADMIN, "http://i.imgur.com/mKdTGlg.png", "noods"))
        funModule.registerCommand(RestrictedRemoteFileCommand(PermissionLevel.BOT_ADMIN, "http://i.imgur.com/i65ss6p.png", "powerpoint"))

        funModule.registerCommand(RemoteFileCommand("http://i.imgur.com/DYToB2e.jpg", "ram"))
        funModule.registerCommand(RemoteFileCommand("http://i.imgur.com/utPRe0e.gif", "welcome", "whalecum"))
        funModule.registerCommand(RemoteFileCommand("http://i.imgur.com/93VahIh.png", "anime"))
        funModule.registerCommand(RemoteFileCommand("http://i.imgur.com/qz6g1vj.gif", "explosion"))
        funModule.registerCommand(RemoteFileCommand("http://i.imgur.com/84nbpQe.png", "internetspeed"))

        /* Text Faces & Unicode 'Art' & ASCII 'Art' and Stuff */
        funModule.registerCommand(TextCommand("¯\\_(ツ)_/¯", "shrug", "shr"))
        funModule.registerCommand(TextCommand("ಠ_ಠ", "faceofdisapproval", "fod", "disapproving"))
        funModule.registerCommand(TextCommand("༼ つ ◕_◕ ༽つ", "sendenergy"))
        funModule.registerCommand(TextCommand("(•\\_•) ( •\\_•)>⌐■-■ (⌐■_■)", "dealwithit", "dwi"))
        funModule.registerCommand(TextCommand("(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧ ✧ﾟ･: *ヽ(◕ヮ◕ヽ)", "channelingenergy"))
        funModule.registerCommand(TextCommand("Ƹ̵̡Ӝ̵̨̄Ʒ", "butterfly"))
        funModule.registerCommand(TextCommand("(ノಠ益ಠ)ノ彡┻━┻", "angrytableflip", "tableflipbutangry", "atp"))
        funModule.registerCommand(TextCommand(AsciiArtConstant.DOG, "dog", "cooldog", "dogmeme"))
        funModule.registerCommand(TextCommand("T-that's l-lewd, baka!!!", "lewd", "lood", "l00d"))
        funModule.registerCommand(TextCommand("This command is useless.", "useless"))
        funModule.registerCommand(TextCommand("¯\\\\(°_o)/¯", "shrugwtf", "swtf"))
        funModule.registerCommand(TextCommand("ヽ(^o^)ノ", "hurray", "yay", "woot"))
        /* Lennies */
        funModule.registerCommand(TextCommand("/╲/╭( ͡° ͡° ͜ʖ ͡° ͡°)╮/╱\\", "spiderlenny"))
        funModule.registerCommand(TextCommand("( ͡° ͜ʖ ͡°)", "lenny"))
        funModule.registerCommand(TextCommand("┬┴┬┴┤ ͜ʖ ͡°) ├┬┴┬┴", "peeking", "peekinglenny", "peek"))
        funModule.registerCommand(TextCommand(AsciiArtConstant.EAGLE_OF_LENNY, "eagleoflenny", "eol", "lennyeagle"))
        funModule.registerCommand(MagicCommand("magic", "magicallenny", "lennymagical"))
        funModule.registerCommand(TextCommand("( ͡°( ͡° ͜ʖ( ͡° ͜ʖ ͡°)ʖ ͡°) ͡°)", "lennygang"))

        /* Random images / image collections */
        funModule.registerCommand(CatgirlCommand("catgirl", "neko", "catgrill"))
        funModule.registerCommand(FacedeskCommand("https://imgur.com/a/I5Q4U", "facedesk"))
        funModule.registerCommand(HugCommand("https://imgur.com/a/jHJOc", "hug"))
        funModule.registerCommand(PatCommand("https://imgur.com/a/WiPTl", "pat"))
        funModule.registerCommand(RollCommand("https://imgur.com/a/lrEwS", "roll"))
        funModule.registerCommand(RandomImageCommand("https://imgur.com/a/mnhzS", "wombat"))
        funModule.registerCommand(RandomImageCommand("https://imgur.com/a/hfL80", "capybara"))
        funModule.registerCommand(RandomImageCommand("https://imgur.com/a/WvjQA", "quokka"))
        funModule.registerCommand(RandomImageCommand("https://imgur.com/a/BL2MW", "otter"))

        // Music Module

        val musicModule = CommandRegistry(Module.MUSIC)
        /* Control */
        musicModule.registerCommand(DestroyCommand("destroy"))
        musicModule.registerCommand(JoinCommand("join", "summon", "jn", "j"))
        musicModule.registerCommand(LeaveCommand("leave", "lv"))
        musicModule.registerCommand(PauseCommand("pause", "pa", "ps"))
        musicModule.registerCommand(PlayCommand(playerLimiter, trackSearcher, videoSelectionCache,
                Arrays.asList(SearchProvider.YOUTUBE, SearchProvider.SOUNDCLOUD),
                PLAY_COMM_NAME, "p"))
        musicModule.registerCommand(PlayCommand(playerLimiter, trackSearcher, videoSelectionCache,
                listOf(SearchProvider.YOUTUBE),
                YOUTUBE_COMM_NAME, "yt"))
        musicModule.registerCommand(PlayCommand(playerLimiter, trackSearcher, videoSelectionCache,
                listOf(SearchProvider.SOUNDCLOUD),
                SOUNDCLOUD_COMM_NAME, "sc"))
        musicModule.registerCommand(ReplayCommand("replay", "rp"))
        musicModule.registerCommand(PlayCommand(playerLimiter, trackSearcher, videoSelectionCache,
                listOf(SearchProvider.YOUTUBE, SearchProvider.SOUNDCLOUD),
                "playnext", "playtop", "pn", isPriority = true))
        musicModule.registerCommand(PlaySplitCommand(playerLimiter, "split"))
        musicModule.registerCommand(RepeatCommand("repeat", "rep", "loop"))
        musicModule.registerCommand(ReshuffleCommand("reshuffle", "resh"))
        musicModule.registerCommand(SelectCommand(videoSelectionCache, "select",
                *buildNumericalSelectAliases("sel")))
        musicModule.registerCommand(ShuffleCommand("shuffle", "sh", "random"))
        musicModule.registerCommand(RoundRobbinCommand("roundrobin", "rr"))
        musicModule.registerCommand(SkipCommand(SKIP_COMM_NAME, "sk", "s"))
        musicModule.registerCommand(StopCommand("stop", "st"))
        musicModule.registerCommand(UnpauseCommand("unpause", "unp", "resume"))
        musicModule.registerCommand(VolumeCommand("volume", "vol"))
        musicModule.registerCommand(VoteSkipCommand("voteskip", "vsk", "v"))
        musicModule.registerCommand(VoteSkipCommand("unvoteskip", "unv", isUnvote = true))

        /* Info */
        musicModule.registerCommand(ExportCommand("export", "ex"))
        musicModule.registerCommand(GensokyoRadioCommand("gensokyo", "gr", "gensokyoradio"))
        musicModule.registerCommand(HistoryCommand("history", "hist", "h"))
        musicModule.registerCommand(ListCommand("list", "queue", "q", "l", "ls"))
        musicModule.registerCommand(NowplayingCommand(youtubeAPI, "nowplaying", "np"))

        /* Seeking */
        musicModule.registerCommand(ForwardCommand("forward", "fwd"))
        musicModule.registerCommand(RestartCommand("restart"))
        musicModule.registerCommand(RewindCommand("rewind", "rew"))
        musicModule.registerCommand(SeekCommand("seek"))
        initialized = true
    }


    /**
     * Build a string array that consist of the max number of searches.
     *
     * @param extraAliases Aliases to be appended to the rest of the ones being built.
     * @return String array that contains string representation of numbers with addOnAliases.
     */
    private fun buildNumericalSelectAliases(vararg extraAliases: String): Array<String> {
        val selectTrackAliases = arrayOfNulls<String>(TrackSearcher.MAX_RESULTS + extraAliases.size)
        var i = 0
        while (i < extraAliases.size) {
            selectTrackAliases[i] = extraAliases[i]
            i++
        }
        while (i < TrackSearcher.MAX_RESULTS + extraAliases.size) {
            selectTrackAliases[i] = (i - extraAliases.size + 1).toString()
            i++
        }
        return selectTrackAliases.requireNoNulls()
    }

}
