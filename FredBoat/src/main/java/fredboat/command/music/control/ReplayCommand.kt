package fredboat.command.music.control

import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.ICommandRestricted
import fredboat.commandmeta.abs.IMusicCommand
import fredboat.definitions.PermissionLevel
import fredboat.main.Launcher
import fredboat.messaging.internal.Context
import fredboat.util.extension.escapeAndDefuse

class ReplayCommand(name: String, vararg aliases: String) : Command(name, *aliases), IMusicCommand, ICommandRestricted {

    override val minimumPerms: PermissionLevel
        get() = PermissionLevel.DJ

    override suspend fun invoke(context: CommandContext) {
        val player = Launcher.botController.playerRegistry.awaitPlayer(context.guild)

        val last = player.historyQueue.lastOrNull()
        if (last == null) {
            context.reply(context.i18n("replayHistoryEmpty"))
            return
        }

        // requeue newest history track as priority track
        last.isPriority = true
        val toQueue = arrayListOf(last) // as list or else we get issues queueing 2 tracks fast one after another

        // if the player is playing requeue the currently playing track as priority track and skip it
        val old = player.internalContext?.makeClone()
        if (old != null) {
            old.isPriority = true
            toQueue.add(old)

            player.skip()
        }

        player.queueAll(toQueue)
        context.reply(context.i18nFormat("replayWillNowReplay", "**${last.effectiveTitle.escapeAndDefuse()}**"))
    }

    override fun help(context: Context): String {
        return "{0}{1}\n# " + context.i18n("helpReplayCommand")
    }
}