package fredboat.command.music.control

import fredboat.commandmeta.CommandInitializer
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.ICommandRestricted
import fredboat.commandmeta.abs.IMusicCommand
import fredboat.definitions.PermissionLevel
import fredboat.main.getBotController
import fredboat.messaging.internal.Context

class RoundRobbinCommand(name: String, vararg aliases: String) : Command(name, *aliases), IMusicCommand, ICommandRestricted {

    override val minimumPerms: PermissionLevel = PermissionLevel.DJ

    override suspend fun invoke(context: CommandContext) {
        val player = getBotController().playerRegistry.awaitPlayer(context.guild)
        player.isRoundRobin = !player.isRoundRobin

        context.reply(if (player.isRoundRobin)
            context.i18nFormat("roundRobinOn", "`${context.prefix}${CommandInitializer.HELP_COMM_NAME} ${context.trigger}`")
        else
            context.i18n("roundRobinOff"))
    }

    override fun help(context: Context): String {
        return "{0}{1}\n#" + context.i18n("helpRoundRobinCommand")
    }
}