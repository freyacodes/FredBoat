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

package fredboat.command.fun;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IFunCommand;
import fredboat.event.EventListenerBoat;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import net.dv8tion.jda.core.entities.Guild;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class DanceCommand extends Command implements IFunCommand {

    private final Function<Guild, ReentrantLock> locks = CacheBuilder.newBuilder()
            .maximumSize(128) //any value will do, but not too big
            .build(CacheLoader.from(() -> new ReentrantLock()))
            .compose(Guild::getId); //mapping guild id to a lock

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        //locking by use of java.util.concurrent.locks

        //in most cases we would need to use a fair lock, but since
        // any one lock is only set-up by one thread we can get away with a naive isLocked check
        ReentrantLock lock = locks.apply(context.getGuild());
        if (lock.isLocked()) {
            return; //already in progress
        }
        Runnable func = new Runnable() {
            @Override
            public void run() {
                context.reply('\u200b' + "\\o\\", msg -> {
                    try {
                        lock.lock();
                        EventListenerBoat.messagesToDeleteIfIdDeleted.put(context.msg.getIdLong(), msg.getIdLong());
                        long start = System.currentTimeMillis();
                        while (start + 60000 > System.currentTimeMillis()) {
                            Thread.sleep(1000);
                            msg = CentralMessaging.editMessage(msg, "/o/").getWithDefaultTimeout();
                            Thread.sleep(1000);
                            msg = CentralMessaging.editMessage(msg, "\\o\\").getWithDefaultTimeout();
                        }
                    } catch (TimeoutException | ExecutionException | InterruptedException ignored) {
                    } finally {
                        lock.unlock();
                    }
                });
            }
        };

        Thread thread = new Thread(func, DanceCommand.class.getSimpleName() + " dance");
        thread.start();
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1}\n#Dance for a minute.";
    }
}
