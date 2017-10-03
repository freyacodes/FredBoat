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
import fredboat.audio.player.PlayerRegistry;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author frederik
 */
public class AnnounceCommand extends Command implements ICommandRestricted {

    private static final Logger log = LoggerFactory.getLogger(AnnounceCommand.class);

    private static final String HEAD = "__**[BROADCASTED MESSAGE]**__\n";

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        List<GuildPlayer> players = PlayerRegistry.getPlayingPlayers();

        if (players.isEmpty()) {
            return;
        }
        String input = context.msg.getRawContent().substring(context.args[0].length() + 1);
        String msg = HEAD + input;

        context.reply(String.format("[0/%d]", players.size()),
                //success handler
                status -> new Thread(() -> {
                    Phaser phaser = new Phaser(players.size());

                    for (GuildPlayer player : players) {
                        TextChannel activeTextChannel = player.getActiveTextChannel();
                        if (activeTextChannel != null) {
                            CentralMessaging.sendMessage(activeTextChannel, msg,
                                    __ -> phaser.arrive(),
                                    __ -> phaser.arriveAndDeregister());
                        } else {
                            phaser.arriveAndDeregister();
                        }
                    }

                    new Thread(() -> {
                        try {
                            do {
                                try {
                                    phaser.awaitAdvanceInterruptibly(0, 5, TimeUnit.SECONDS);
                                    // Now all the parties have arrived, we can break out of the loop
                                    break;
                                } catch (TimeoutException ex) {
                                    // This is fine, this means that the required parties haven't arrived
                                }
                                printProgress(status,
                                        phaser.getArrivedParties(),
                                        players.size(),
                                        players.size() - phaser.getRegisteredParties());
                            } while (true);
                            printDone(status,
                                    phaser.getRegisteredParties(), //phaser wraps back to 0 on phase increment
                                    players.size() - phaser.getRegisteredParties());
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt(); // restore interrupt flag
                            log.error("interrupted", ex);
                            throw new RuntimeException(ex);
                        }
                    }).start();
                }).start(),

                //failure handler
                throwable -> {
                    log.error("Announcement failed!", throwable);
                    TextUtils.handleException(throwable, context);
                    throw new RuntimeException(throwable);
                }
        );
    }

    private static void printProgress(Message message, int done, int total, int error) {
        CentralMessaging.editMessage(message, MessageFormat.format(
                            "[{0}/{1}]{2,choice,0#|0< {2} failed}",
                            done, total, error)
        );
    }
    private static void printDone(Message message, int completed, int failed) {
        CentralMessaging.editMessage(message, MessageFormat.format(
                            "{0} completed, {1} failed",
                            completed, failed)
        );
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1}\n#Broadcasts an announcement to GuildPlayer TextChannels.";
    }

    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.BOT_ADMIN;
    }
}
