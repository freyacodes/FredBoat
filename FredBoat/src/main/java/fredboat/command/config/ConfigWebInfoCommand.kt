package fredboat.command.config

import fredboat.command.info.HelpCommand
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.ICommandRestricted
import fredboat.commandmeta.abs.IConfigCommand
import fredboat.config.property.AppConfig
import fredboat.db.api.GuildSettingsRepository
import fredboat.db.transfer.GuildSettings
import fredboat.definitions.PermissionLevel
import fredboat.messaging.internal.Context
import kotlinx.coroutines.reactive.awaitSingle

class ConfigWebInfoCommand(
        name: String,
        vararg aliases: String,
        private val repo: GuildSettingsRepository,
        private val appConfig: AppConfig
) : Command(name, *aliases), IConfigCommand, ICommandRestricted {

    override val minimumPerms = PermissionLevel.ADMIN

    override suspend fun invoke(context: CommandContext) {
        if (appConfig.webInfoBaseUrl.isBlank()) {
            context.reply("This bot isn't configured to support web info")
            return
        }

        val settings = repo.fetch(context.guild.id)
                .defaultIfEmpty(GuildSettings(context.guild.id))
                .awaitSingle()

        when(context.args.getOrNull(0)?.trim()?.toLowerCase()) {
            "allow" -> settings.allowPublicPlayerInfo = true
            "deny" -> settings.allowPublicPlayerInfo = false
            else -> {
                HelpCommand.sendFormattedCommandHelp(context)
                return
            }
        }
        repo.update(settings).subscribe {
            val response = if (it.allowPublicPlayerInfo)
                "Online playing status is now available at ${appConfig.webInfoBaseUrl}${context.guild.id}"
            else "Online playing status has been disabled."
            context.reply(response)
        }
    }

    override fun help(context: Context) = "{0}{1} allow OR {0}{1} deny"

}