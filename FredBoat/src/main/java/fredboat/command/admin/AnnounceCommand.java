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

import fredboat.audio.GuildPlayer;
import fredboat.audio.PlayerRegistry;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.ICommandAdminRestricted;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author frederik
 */
public class AnnounceCommand extends Command implements ICommandAdminRestricted {

    private static final Logger log = LoggerFactory.getLogger(AnnounceCommand.class);

    private static final String HEAD = "__**[BROADCASTED MESSAGE]**__\n";

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        List<GuildPlayer> players = PlayerRegistry.getPlayingPlayers();

        if (players.isEmpty()) {
            return;
        }
        String input = message.getRawContent().substring(args[0].length() + 1);
        String msg = HEAD + input;

        Message status;
        try {
            status = channel.sendMessage(String.format("[0/%d]", players.size())).complete(true);
        } catch (RateLimitedException e) {
            log.error("annuoncement failed! Rate limits.", e);
            TextUtils.handleException(e, channel, invoker);
            throw new RuntimeException(e);
        }

        new Thread(() -> {
            Phaser phaser = new Phaser(players.size());

            for (GuildPlayer player : players) {
                player.getActiveTextChannel().sendMessage(msg).queue(
                        __ -> phaser.arrive(),
                        __ -> phaser.arriveAndDeregister());
            }

            new Thread(() -> {
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(5));
                    do {
                        try {
                            phaser.awaitAdvanceInterruptibly(0, 5, TimeUnit.SECONDS);
                            // now all the parties have arrived, we can break out of the loop
                            break;
                        } catch (TimeoutException ex) {
                            // this is fine, this means that the required parties haven't arrived
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
        }).start();
    }

    private static void printProgress(Message message, int done, int total, int error) {
                    message.editMessage(MessageFormat.format(
                            "[{0}/{1}]{2,choice,0#|0< {2} failed}",
                            done, total, error)
                    ).queue();
    }
    private static void printDone(Message message, int completed, int failed) {
                    message.editMessage(MessageFormat.format(
                            "{0} completed, {1} failed",
                            completed, failed)
                    ).queue();
    }

    @Override
    public String help(Guild guild) {
        return "{0}{1}\n#Broadcasts an announcement to GuildPlayer TextChannels.";
    }
}
