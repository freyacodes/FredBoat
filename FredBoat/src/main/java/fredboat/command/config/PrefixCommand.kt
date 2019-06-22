/*
 *
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

package fredboat.command.config

import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.IConfigCommand
import fredboat.db.api.GuildSettingsRepository
import fredboat.definitions.PermissionLevel
import fredboat.main.Launcher
import fredboat.messaging.internal.Context
import fredboat.perms.PermsUtil
import fredboat.sentinel.Guild
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * Created by napster on 19.10.17.
 */
class PrefixCommand(private val repo: GuildSettingsRepository,
                    name: String, vararg aliases: String)
    : Command(name, *aliases), IConfigCommand {

    init {
        staticRepo = repo
    }

    // TODO: Remove blocking versions and static abuse
    companion object {
        lateinit var staticRepo: GuildSettingsRepository
        val defaultPrefix: String get() = Launcher.botController.appConfig.prefix

        fun getPrefix(guildId: Long): Mono<String> = staticRepo.fetch(guildId)
                .map { it.prefix ?: defaultPrefix }
                .defaultIfEmpty(defaultPrefix)
        fun getPrefix(guild: Guild) = getPrefix(guild.id)

        fun giefPrefix(guild: Guild) = getPrefix(guild).block(Duration.ofSeconds(10))!!

        fun showPrefix(context: Context, prefix: String) {
            val p = if (prefix.isEmpty()) "No Prefix" else prefix
            context.reply(context.i18nFormat("prefixGuild", "``$p``")
                    + "\n" + context.i18n("prefixShowAgain"))
        }
    }

    override suspend fun invoke(context: CommandContext) {

        if (context.rawArgs.isEmpty()) {
            showPrefix(context, context.prefix)
            return
        }

        if (!PermsUtil.checkPermsWithFeedback(PermissionLevel.ADMIN, context)) {
            return
        }

        val newPrefix: String?
        if (context.rawArgs.equals("no_prefix", ignoreCase = true)) {
            newPrefix = "" //allow users to set an empty prefix with a special keyword
        } else if (context.rawArgs.equals("reset", ignoreCase = true)) {
            newPrefix = null
        } else {
            //considering this is an admin level command, we can allow users to do whatever they want with their guild
            // prefix, so no checks are necessary here
            newPrefix = context.rawArgs
        }

        repo.fetch(context.guild.id)
                .doOnSuccess { it.prefix = newPrefix }
                .let { repo.update(it) }
                .subscribe()

        showPrefix(context, giefPrefix(context.guild))
    }

    override fun help(context: Context): String {
        return "{0}{1} <prefix> OR {0}{1} no_prefix OR {0}{1} reset\n#" + context.i18n("helpPrefixCommand")
    }
}
