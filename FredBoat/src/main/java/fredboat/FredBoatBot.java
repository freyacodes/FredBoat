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

package fredboat;

import fredboat.audio.nas.NativeAudioSendFactory;
import fredboat.audio.player.LavalinkManager;
import fredboat.audio.player.PlayerRegistry;
import fredboat.event.EventLogger;
import fredboat.event.ShardWatchdogListener;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

public class FredBoatBot extends FredBoat {

    private static final Logger log = LoggerFactory.getLogger(FredBoatBot.class);
    private final int shardId;
    private final EventListener listener;

    public FredBoatBot(int shardId) {
        this(shardId, null);
    }

    public FredBoatBot(int shardId, EventListener listener) {
        this.shardId = shardId;
        this.listener = listener;
        log.info("Building shard " + shardId);
        jda = buildJDA();
    }

    private JDA buildJDA(boolean... blocking) {
        shardWatchdogListener = new ShardWatchdogListener();

        JDA newJda = null;

        try {
            boolean success = false;
            while (!success) {
                JDABuilder builder = new JDABuilder(AccountType.BOT)
                        .addEventListener(new EventLogger("216689009110417408"))
                        .addEventListener(shardWatchdogListener)
                        .setToken(Config.CONFIG.getBotToken())
                        .setGame(Game.of(Config.CONFIG.getGame()))
                        .setBulkDeleteSplittingEnabled(true)
                        .setEnableShutdownHook(false);

                if(listener != null) {
                    builder.addEventListener(listener);
                } else {
                    log.warn("Starting a shard without an event listener!");
                }

                if (LavalinkManager.ins.isEnabled()) {
                    builder.addEventListener(LavalinkManager.ins.getLavalink());
                }

                if (!System.getProperty("os.arch").equalsIgnoreCase("arm")
                        && !System.getProperty("os.arch").equalsIgnoreCase("arm-linux")
                        && !System.getProperty("os.arch").equalsIgnoreCase("darwin")
                        && !System.getProperty("os.name").equalsIgnoreCase("Mac OS X")) {
                    builder.setAudioSendFactory(new NativeAudioSendFactory());
                }
                if (Config.CONFIG.getNumShards() > 1) {
                    builder.useSharding(shardId, Config.CONFIG.getNumShards());
                }
                try {
                    while (!getShardCoin(shardId)) {
                        //beg aggressively for a coin
                        Thread.sleep(1000);
                    }
                    if (blocking.length > 0 && blocking[0]) {
                        newJda = builder.buildBlocking();
                    } else {
                        newJda = builder.buildAsync();
                    }
                    success = true;
                } catch (RateLimitedException e) {
                    log.error("Got rate limited while building bot JDA instance! Retrying...", e);
                } catch (Exception e) {
                    log.error("Generic exception when building a JDA instance! Retrying...", e);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to start JDA shard " + shardId, e);
        }

        return newJda;
    }

    private volatile Future reviveTask;
    private volatile long reviveTaskStarted;

    @Override
    public synchronized String revive(boolean... force) {

        String info = "";
        //is there an active task doing that already?
        if (reviveTask != null && !reviveTask.isCancelled() && !reviveTask.isDone()) {
            info += String.format("Active task to revive shard %s running for %s detected.",
                    shardId, TextUtils.formatTime(System.currentTimeMillis() - reviveTaskStarted));

            //is the force flag set?
            if (force.length > 0 && force[0]) {
                info += "\nForce option detected: Killing running task, creating a new one.";
                reviveTask.cancel(true);
            } else {
                info += "\nNo action required. Set force flag to force a recreation of the task.";
                log.info(info);
                return info;
            }
        }

        //wrap this into a task to avoid blocking a thread
        reviveTaskStarted = System.currentTimeMillis();
        reviveTask = FredBoat.executor.submit(() -> {
            try {
                log.info("Reviving shard " + shardId);

                try {
                    channelsToRejoin.clear();

                    PlayerRegistry.getPlayingPlayers().stream()
                            .filter(guildPlayer -> guildPlayer.getJda().getShardInfo().getShardId() == shardId)
                            .forEach(guildPlayer -> {
                                VoiceChannel channel = guildPlayer.getChannel();
                                if (channel != null) channelsToRejoin.add(channel.getId());
                            });
                } catch (Exception ex) {
                    log.error("Caught exception while saving channels to revive shard {}", shardId, ex);
                }

                //remove listeners from decommissioned jda for good memory hygiene
                jda.removeEventListener(shardWatchdogListener);
                jda.removeEventListener(listener);

                jda.shutdown();
                //a blocking build makes sure the revive task runs until the shard is connected, otherwise the shard may
                // get revived again accidentally while still connecting
                jda = buildJDA(true);

            } catch (Exception e) {
                log.error("Task to revive shard {} threw an exception after running for {}",
                        shardId, TextUtils.formatTime(System.currentTimeMillis() - reviveTaskStarted), e);
            }
        });
        info += String.format("\nTask to revive shard %s started!", shardId);
        log.info(info);
        return info;
    }

    int getShardId() {
        return shardId;
    }
}
