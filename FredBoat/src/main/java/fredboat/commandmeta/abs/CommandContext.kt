/*
 *
 * MIT License
 *
 * Copyright (c) 2017-2018 Frederik Ar. Mikkelsen
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

package fredboat.commandmeta.abs

import fredboat.definitions.Module
import fredboat.main.Launcher
import fredboat.messaging.internal.Context
import fredboat.sentinel.*
import kotlinx.coroutines.reactive.awaitFirst

/**
 * Convenience container for values associated with an issued command
 */
class CommandContext(
        override val guild: Guild,
        override val textChannel: TextChannel,
        override val member: Member,
        val msg: Message,
        val isMention: Boolean,               // whether a mention was used to trigger this command
        val trigger: String,                  // the command trigger, e.g. "play", or "p", or "pLaY", whatever the user typed
        val args: Array<String>,              // the arguments split by whitespace, excluding prefix and trigger
        val rawArgs: String,                  // raw arguments excluding prefix and trigger, trimmed
        val command: Command) : Context() {

    /**
     * @return an adjusted list of mentions in case the prefix mention is used to exclude it. This method should always
     * be used over Message#getMentions()
     */
    //remove the first mention
    //FIXME: this will mess with the mentions if the bot was mentioned at a later place in the message a second time,
    // for example @bot hug @bot will not trigger a self hug message
    // low priority, this is mostly a cosmetic issue
    val mentionedMembers: List<Member>
        get() {
            return if (isMention) {
                val mentions = msg.mentionedMembers.toMutableList()
                if (!mentions.isEmpty()) {
                    mentions.removeAt(0)
                }
                mentions
            } else {
                msg.mentionedMembers
            }
        }

    val enabledModules: Collection<Module>
        get() = Launcher.botController.guildSettingsRepository.fetch(guild.id).block()!!
                .modules.filter { it.enabled }
                .map { it.module }

    override val user: User
        get() = member.user

    /**
     * Deletes the users message that triggered this command, if we have the permissions to do so
     */
    fun deleteMessage() {
        msg.delete().subscribe()
    }

    fun hasArguments(): Boolean {
        return args.isNotEmpty() && !rawArgs.isEmpty()
    }
}
