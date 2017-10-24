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

import fredboat.audio.player.AbstractPlayer;
import fredboat.audio.player.PlayerRegistry;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class EvalCommand extends Command implements ICommandRestricted {

    private static final Logger log = LoggerFactory.getLogger(EvalCommand.class);

    //Thanks Dinos!
    private ScriptEngine engine;

    public EvalCommand(String name, String... aliases) {
        super(name, aliases);
        engine = new ScriptEngineManager().getEngineByName("nashorn");
        try {
            engine.eval("var imports = new JavaImporter(java.io, java.lang, java.util);");

        } catch (ScriptException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        Guild guild = context.guild;
        JDA jda = guild.getJDA();
        context.sendTyping();

        final String source = context.rawArgs;

        engine.put("jda", jda);
        engine.put("api", jda);
        engine.put("channel", context.channel);
        engine.put("vc", PlayerRegistry.getExisting(guild) != null ? PlayerRegistry.getExisting(guild).getCurrentVoiceChannel() : null);
        engine.put("author", context.invoker);
        engine.put("bot", jda.getSelfUser());
        engine.put("member", guild.getSelfMember());
        engine.put("message", context.msg);
        engine.put("guild", guild);
        engine.put("player", PlayerRegistry.getExisting(guild));
        engine.put("pm", AbstractPlayer.getPlayerManager());
        engine.put("context", context);

        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
        ScheduledFuture<?> future = service.schedule(() -> {

            Object out;
            try {
                out = engine.eval(
                        "(function() {"
                        + "with (imports) {\n" + source + "\n}"
                        + "})();");

            } catch (Exception ex) {
                context.reply("`" + ex.getMessage() + "`");
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

            context.reply(TextUtils.asCodeBlock(source, "java") + "\n" + outputS);

        }, 0, TimeUnit.MILLISECONDS);

        Thread script = new Thread("Eval") {
            @Override
            public void run() {
                try {
                    future.get(10, TimeUnit.SECONDS);

                } catch (TimeoutException ex) {
                    future.cancel(true);
                    context.reply("Task exceeded time limit.");
                } catch (Exception ex) {
                    context.reply("`" + ex.getMessage() + "`");
                }
            }
        };
        script.start();
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} <Java-code>\\n#Run the provided Java code.";
    }

    @Nonnull
    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.BOT_OWNER;
    }
}
