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

package fredboat.command.admin;

import fredboat.audio.player.GuildPlayer;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.definitions.PermissionLevel;
import fredboat.main.Launcher;
import fredboat.messaging.internal.Context;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.concurrent.*;

public class EvalCommand extends Command implements ICommandRestricted {

    private static final Logger log = LoggerFactory.getLogger(EvalCommand.class);

    @Nullable
    private Future lastTask;

    //Thanks Dinos!
    private ScriptEngine engine;

    public EvalCommand(String name, String... aliases) {
        super(name, aliases);
        engine = new ScriptEngineManager().getEngineByName("nashorn");
        try {
            engine.eval("var imports = new JavaImporter(java.io, java.lang, java.util);");

        } catch (ScriptException ex) {
            log.error("Failed to init eval command", ex);
        }
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        final long started = System.currentTimeMillis();

        String source = context.rawArgs;

        if (context.hasArguments() && (context.args[0].equals("-k") || context.args[0].equals("kill"))) {
            if (this.lastTask != null) {
                if (this.lastTask.isDone() || this.lastTask.isCancelled()) {
                    context.reply("Task isn't running.");
                } else {
                    this.lastTask.cancel(true);
                    context.reply("Task killed.");
                }
            } else {
                context.reply("No task found to kill.");
            }
            return;
        }

        context.sendTyping();

        final int timeOut;
        if (context.args.length > 1 && (context.args[0].equals("-t") || context.args[0].equals("timeout"))) {
            timeOut = Integer.parseInt(context.args[1]);
            source = source.replaceFirst(context.args[0], "");
            source = source.replaceFirst(context.args[1], "");
        } else timeOut = -1;

        final String finalSource = source.trim();

        Guild guild = context.guild;
        JDA jda = guild.getJDA();

        engine.put("jda", jda);
        engine.put("api", jda);
        engine.put("channel", context.channel);
        GuildPlayer player = Launcher.getBotController().getPlayerRegistry().getExisting(guild);
        engine.put("vc", player != null ? player.getCurrentVoiceChannel() : null);
        engine.put("author", context.msg.getAuthor());
        engine.put("invoker", context.invoker);
        engine.put("bot", jda.getSelfUser());
        engine.put("member", guild.getSelfMember());
        engine.put("message", context.msg);
        engine.put("guild", guild);
        engine.put("player", player);
        engine.put("pm", Launcher.getBotController().getAudioPlayerManager());
        engine.put("context", context);

        ScheduledExecutorService service = Executors.newScheduledThreadPool(1, r -> new Thread(r, "Eval comm execution"));

        Future<?> future = service.submit(() -> {
            Object out;
            try {
                out = engine.eval(
                        "(function() {"
                                + "with (imports) {\n" + finalSource + "\n}"
                                + "})();");

            } catch (Exception ex) {
                context.reply(String.format("`%s`\n\n`%sms`",
                        ex.getMessage(), System.currentTimeMillis() - started));
                log.info("Error occurred in eval", ex);
                return;
            }

            String outputS;
            if (out == null) {
                outputS = ":ok_hand::skin-tone-3:";
            } else if (out.toString().contains("\n")) {
                outputS = "\nEval: " + TextUtils.asCodeBlock(out.toString());
            } else {
                outputS = "\nEval: `" + out.toString() + "`";
            }
            context.reply(String.format("```java\n%s```\n%s\n`%sms`",
                    finalSource, outputS, System.currentTimeMillis() - started));

        });
        this.lastTask = future;

        Thread script = new Thread("Eval comm waiter") {
            @Override
            public void run() {
                try {
                    if (timeOut > -1) {
                        future.get(timeOut, TimeUnit.SECONDS);
                    }
                } catch (final TimeoutException ex) {
                    future.cancel(true);
                    context.reply("Task exceeded time limit of " + timeOut + " seconds.");
                } catch (final Exception ex) {
                    context.reply(String.format("`%s`\n\n`%sms`",
                            ex.getMessage(), System.currentTimeMillis() - started));
                }
            }
        };
        script.start();
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} [-t seconds | -k] <javascript-code>\n#Run the provided javascript code with the Nashorn engine."
                + " By default no timeout is set for the task, set a timeout by passing `-t` as the first argument and"
                + " the amount of seconds to wait for the task to finish as the second argument."
                + " Run with `-k` or `kill` as first argument to stop the last submitted eval task if it's still ongoing.";
    }

    @Nonnull
    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.BOT_OWNER;
    }
}
