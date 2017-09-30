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

import fredboat.FredBoat;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import fredboat.shared.constant.ExitCodes;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class UpdateCommand extends Command implements ICommandRestricted {

    private static final Logger log = LoggerFactory.getLogger(UpdateCommand.class);
    private static final CompileCommand COMPILE_COMMAND = new CompileCommand();
    private static final long MAX_JAR_AGE = 10 * 60 * 1000;

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        try {
            File homeJar = new File(System.getProperty("user.home") + "/FredBoat-1.0.jar");

            //Must exist and not be too old
            if(homeJar.exists()
                    && (System.currentTimeMillis() - homeJar.lastModified()) < MAX_JAR_AGE){
                update(context);
                return;
            } else {
                log.info("");
            }

            COMPILE_COMMAND.onInvoke(context);

            update(context);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void update(CommandContext context) throws IOException {
        File homeJar = new File(System.getProperty("user.home") + "/FredBoat-1.0.jar");
        File targetJar = new File("./update/target/FredBoat-1.0.jar");

        targetJar.getParentFile().mkdirs();
        targetJar.delete();
        FileUtils.copyFile(homeJar, targetJar);

        //Shutdown for update
        try {
            context.reply("Now restarting...").getWithDefaultTimeout();
        } catch (TimeoutException | InterruptedException | ExecutionException ignored) {
        }
        FredBoat.shutdown(ExitCodes.EXIT_CODE_UPDATE);
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} [branch [repo]]\n#Update the bot by checking out the provided branch from the provided github repo and compiling it. Default github repo is Frederikam, default branch is master. Restart with the fresh build.";
    }

    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.BOT_OWNER;
    }
}
