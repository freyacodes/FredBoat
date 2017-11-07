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

import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import fredboat.util.log.SLF4JInputStreamErrorLogger;
import fredboat.util.log.SLF4JInputStreamLogger;
import net.dv8tion.jda.core.entities.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Deprecated //we dont use maven anymore. todo decide if can be repurposed for gradle
public class CompileCommand extends Command implements ICommandRestricted {

    private static final Logger log = LoggerFactory.getLogger(CompileCommand.class);

    public CompileCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        context.reply("*Now updating...*\n\nRunning `git clone`... ",
                status -> cloneAndCompile(context, status),
                throwable -> {
                    throw new RuntimeException(throwable);
                }
        );
    }

    private void cloneAndCompile(CommandContext context, Message status) {
        try {
            Runtime rt = Runtime.getRuntime();

            String branch = "master";
            if (context.hasArguments()) {
                branch = context.args[0];
            }
            String githubUser = "Frederikam";
            if (context.args.length > 1) {
                githubUser = context.args[1];
            }

            //Clear any old update folder if it is still present
            try {
                Process rm = rt.exec("rm -rf update");
                rm.waitFor(5, TimeUnit.SECONDS);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            Process gitClone = rt.exec("git clone https://github.com/" + githubUser + "/FredBoat.git --branch " + branch + " --recursive --single-branch update");
            new SLF4JInputStreamLogger(log, gitClone.getInputStream()).start();
            new SLF4JInputStreamErrorLogger(log, gitClone.getInputStream()).start();

            if (!gitClone.waitFor(120, TimeUnit.SECONDS)) {
                CentralMessaging.editMessage(status, status.getRawContent() + "[:anger: timed out]\n\n");
                throw new RuntimeException("Operation timed out: git clone");
            } else if (gitClone.exitValue() != 0) {
                CentralMessaging.editMessage(status, status.getRawContent() + "[:anger: returned code " + gitClone.exitValue() + "]\n\n");
                throw new RuntimeException("Bad response code");
            }
            try {
                CentralMessaging.editMessage(status, status.getRawContent() + "👌🏽\n\nRunning `mvn package shade:shade`... ")
                        .getWithDefaultTimeout();
            } catch (TimeoutException | ExecutionException ignored) {
            }

            File updateDir = new File("update/FredBoat");

            Process mvnBuild = rt.exec("mvn -f " + updateDir.getAbsolutePath() + "/pom.xml package shade:shade");
            new SLF4JInputStreamLogger(log, mvnBuild.getInputStream()).start();
            new SLF4JInputStreamErrorLogger(log, mvnBuild.getInputStream()).start();

            if (!mvnBuild.waitFor(600, TimeUnit.SECONDS)) {
                CentralMessaging.editMessage(status, status.getRawContent() + "[:anger: timed out]\n\n");
                throw new RuntimeException("Operation timed out: mvn package shade:shade");
            } else if (mvnBuild.exitValue() != 0) {
                CentralMessaging.editMessage(status,
                        status.getRawContent() + "[:anger: returned code " + mvnBuild.exitValue() + "]\n\n");
                throw new RuntimeException("Bad response code");
            }

            CentralMessaging.editMessage(status, status.getRawContent() + "👌🏽");

            if (!new File("./update/FredBoat/target/FredBoat-1.0.jar").renameTo(new File(System.getProperty("user.home") + "/FredBoat-1.0.jar"))) {
                throw new RuntimeException("Failed to move jar to home");
            }
        } catch (InterruptedException | IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} [branch [repo]]\n#Update the bot by checking out the provided branch from the provided github repo and compiling it. Default github repo is Frederikam, default branch is master. Does not restart the bot.";
    }

    @Nonnull
    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.BOT_OWNER;
    }
}
