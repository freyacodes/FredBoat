/*
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

package fredboat.commandmeta

import com.fredboat.sentinel.entities.MessageReceivedEvent
import fredboat.command.config.PrefixCommand
import fredboat.commandmeta.abs.CommandContext
import fredboat.config.idString
import fredboat.config.property.AppConfig
import fredboat.feature.metrics.Metrics
import fredboat.sentinel.Message
import fredboat.sentinel.RawUser
import fredboat.sentinel.getGuildMono
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.*
import java.util.regex.Pattern

/**
 * Created by napster on 23.02.18.
 */
@Component
class CommandContextParser(
        private val appConfig: AppConfig,
        @param:Qualifier("selfUser")
        private val selfUser: RawUser
) {

    companion object {
        private val log = LoggerFactory.getLogger(CommandContext::class.java)

        // https://regex101.com/r/ceFMeF/6
        //group 1 is the mention, group 2 is the id of the mention, group 3 is the rest of the input including new lines
        private val MENTION_PREFIX = Pattern.compile("^(<@!?([0-9]+)>)(.*)$", Pattern.DOTALL)
    }

    /**
     * @return The full context for the triggered command, or null if it's not a command that we know.
     */
    suspend fun parse(event: MessageReceivedEvent): CommandContext? {
        val content = event.content
        var input: String
        var isMention = false
        val mentionMatcher = MENTION_PREFIX.matcher(content)
        // either starts with a mention of us
        val botId = selfUser.idString
        if (mentionMatcher.find() && mentionMatcher.group(2) == botId) {
            input = mentionMatcher.group(3).trim { it <= ' ' }
            isMention = true
        } else {
            val prefix = PrefixCommand.getPrefix(event.guild).awaitSingle()
            val defaultPrefix = appConfig.prefix
            if (content.startsWith(prefix)) {
                input = content.substring(prefix.length)
                if (prefix == defaultPrefix) {
                    Metrics.prefixParsed.labels("default").inc()
                } else {
                    Metrics.prefixParsed.labels("custom").inc()
                }
            } else {
                //hardcoded check for the help or prefix command that is always displayed as FredBoat status
                if (content.startsWith(defaultPrefix + CommandInitializer.HELP_COMM_NAME)
                        || content.startsWith(defaultPrefix + CommandInitializer.PREFIX_COMM_NAME)) {
                    Metrics.prefixParsed.labels("default").inc()
                    input = content.substring(defaultPrefix.length)
                } else {
                    //no match neither mention nor custom/default prefix
                    return null
                }
            }
        }// or starts with a custom/default prefix
        input = input.trim { it <= ' ' }// eliminate possible whitespace between the mention/prefix and the rest of the input
        if (input.isEmpty()) {
            if (isMention) { //just a mention and nothing else? trigger the prefix command
                input = "prefix"
            } else {
                return null //no command will be detectable from an empty input
            }
        }

        // the \p{javaSpaceChar} instead of the better known \s is used because it actually includes unicode whitespaces
        val args = input.split("\\p{javaSpaceChar}+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (args.isEmpty()) {
            return null //while this shouldn't technically be possible due to the preprocessing of the input, better be safe than throw exceptions
        }

        val commandTrigger = args[0]

        val command = CommandRegistry.findCommand(commandTrigger.toLowerCase())
        if (command == null) {
            log.info("Unknown command:\t{}", commandTrigger)
            return null
        } else {
            val guild = getGuildMono(event.guild, textChannelInvoked = event.channel).retry(1).awaitFirstOrNull()
                    ?: throw RuntimeException("Guild ${event.guild} doesn't seem to exist")
            val channel = guild.getTextChannel(event.channel) ?: throw RuntimeException("Channel was sent in null channel")
            val member = guild.getMember(event.author) ?: throw RuntimeException("Unknown message author")

            return CommandContext(
                    guild,
                    channel,
                    member,
                    Message(guild, event),
                    isMention,
                    commandTrigger,
                    Arrays.copyOfRange(args, 1, args.size), //exclude args[0] that contains the command trigger
                    input.replaceFirst(commandTrigger.toRegex(), "").trim { it <= ' ' },
                    command)
        }
    }

}
