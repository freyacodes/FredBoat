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
 *
 */

package fredboat.command.admin

import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.ICommandRestricted
import fredboat.definitions.PermissionLevel
import fredboat.main.Launcher
import fredboat.messaging.internal.Context
import fredboat.util.TextUtils
import lavalink.client.player.LavalinkPlayer
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.ScriptException

class EvalCommand(
        private val springContext: () -> ApplicationContext,
        name: String,
        vararg aliases: String
) : Command(name, *aliases), ICommandRestricted {

    companion object {
        private val log = LoggerFactory.getLogger(EvalCommand::class.java)
    }

    private var lastTask: Future<*>? = null

    //Thanks Dinos!
    private val engine: ScriptEngine = ScriptEngineManager().getEngineByName("nashorn")

    override val minimumPerms: PermissionLevel
        get() = PermissionLevel.BOT_OWNER

    init {
        try {
            engine.eval("var imports = new JavaImporter(java.io, java.lang, java.util);")

        } catch (ex: ScriptException) {
            log.error("Failed to init eval command", ex)
        }

    }

    override suspend fun invoke(context: CommandContext) {
        val started = System.currentTimeMillis()

        var source = context.rawArgs

        if (context.hasArguments() && (context.args[0] == "-k" || context.args[0] == "kill")) {
            if (this.lastTask != null) {
                if (this.lastTask!!.isDone || this.lastTask!!.isCancelled) {
                    context.reply("Task isn't running.")
                } else {
                    this.lastTask!!.cancel(true)
                    context.reply("Task killed.")
                }
            } else {
                context.reply("No task found to kill.")
            }
            return
        }

        context.sendTyping()

        val timeOut: Int
        if (context.args.size > 1 && (context.args[0] == "-t" || context.args[0] == "timeout")) {
            timeOut = Integer.parseInt(context.args[1])
            source = source.replaceFirst(context.args[0].toRegex(), "")
            source = source.replaceFirst(context.args[1].toRegex(), "")
        } else
            timeOut = -1

        val finalSource = source.trim { it <= ' ' }

        context.apply {
            engine.put("sentinel", guild.sentinel)
            engine.put("channel", textChannel)
            engine.put("tc", textChannel)
            val player = Launcher.botController.playerRegistry.getOrCreate(guild)
            engine.put("player", player)
            engine.put("link", (player.player as LavalinkPlayer?)?.link)
            engine.put("lavalink", player.lavalink)
            engine.put("vc", player.currentVoiceChannel)
            engine.put("author", member)
            engine.put("invoker", member)
            engine.put("bot", guild.selfMember)
            engine.put("member", guild.selfMember)
            engine.put("message", msg)
            engine.put("guild", guild)
            engine.put("player", player)
            engine.put("pm", Launcher.botController.audioPlayerManager)
            engine.put("context", context)
            engine.put("spring", springContext.invoke())
        }

        val service = Executors.newScheduledThreadPool(1) { r -> Thread(r, "Eval comm execution") }

        val future = service.submit {
            val out: Any?
            try {
                out = engine.eval(
                        "(function() {"
                                + "with (imports) {\n" + finalSource + "\n}"
                                + "})();")

            } catch (ex: Exception) {
                context.reply(String.format("`%s`\n\n`%sms`",
                        ex.message, System.currentTimeMillis() - started))
                log.info("Error occurred in eval", ex)
                return@submit
            }

            val outputS: String
            outputS = when {
                out == null -> ":ok_hand::skin-tone-3:"
                out.toString().contains("\n") -> "\nEval: " + TextUtils.asCodeBlock(out.toString())
                else -> "\nEval: `" + out.toString() + "`"
            }
            context.reply(String.format("```java\n%s```\n%s\n`%sms`",
                    finalSource, outputS, System.currentTimeMillis() - started))

        }
        this.lastTask = future

        val script = object : Thread("Eval comm waiter") {
            override fun run() {
                try {
                    if (timeOut > -1) {
                        future.get(timeOut.toLong(), TimeUnit.SECONDS)
                    }
                } catch (ex: TimeoutException) {
                    future.cancel(true)
                    context.reply("Task exceeded time limit of $timeOut seconds.")
                } catch (ex: Exception) {
                    context.reply(String.format("`%s`\n\n`%sms`",
                            ex.message, System.currentTimeMillis() - started))
                }

            }
        }
        script.start()
    }

    override fun help(context: Context): String {
        return ("{0}{1} [-t seconds | -k] <javascript-code>\n#Run the provided javascript code with the Nashorn engine."
                + " By default no timeout is set for the task, set a timeout by passing `-t` as the first argument and"
                + " the amount of seconds to wait for the task to finish as the second argument."
                + " Run with `-k` or `kill` as first argument to stop the last submitted eval task if it's still ongoing.")
    }
}
