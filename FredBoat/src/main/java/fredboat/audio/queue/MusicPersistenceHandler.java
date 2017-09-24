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

package fredboat.audio.queue;

import fredboat.FredBoat;
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.PlayerRegistry;
import fredboat.db.EntityReader;
import fredboat.feature.I18n;
import fredboat.messaging.CentralMessaging;
import fredboat.shared.constant.ExitCodes;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MusicPersistenceHandler {

    private static final Logger log = LoggerFactory.getLogger(MusicPersistenceHandler.class);

    private MusicPersistenceHandler() {
    }

    public static void handlePreShutdown(int code) {
        //make a copy of the reg list to avoid concurrent modification if players get added/deleted while this runs
        List<GuildPlayer> players = new ArrayList<>(PlayerRegistry.getRegistry().values());

        boolean isUpdate = code == ExitCodes.EXIT_CODE_UPDATE;
        boolean isRestart = code == ExitCodes.EXIT_CODE_RESTART;

        long started = System.currentTimeMillis();
        int playersSaved = 0;

        log.info("Saving {} guild players", players.size());
        for (GuildPlayer player : players) {
            playersSaved++;
            //if the player is being used make an announcement to the users
            if (player.isPlaying()) {
                String msg;
                if (isUpdate) {
                    msg = I18n.get(player.getGuild()).getString("shutdownUpdating");
                } else if (isRestart) {
                    msg = I18n.get(player.getGuild()).getString("shutdownRestarting");
                } else {
                    msg = I18n.get(player.getGuild()).getString("shutdownIndef");
                }

                TextChannel activeTextChannel = player.getActiveTextChannel();
                if (activeTextChannel != null) {
                    CentralMessaging.sendMessage(activeTextChannel, msg);
                }
            }
            try {
                player.save(true);
            } catch (Exception e) {
                log.error("Error when saving guild player for guild {}", player.getGuild().getId(), e);
            }
        }
        log.info("Saved {} guild players in {} ms", playersSaved, System.currentTimeMillis() - started);
    }

    /**
     * Reloads unpaused guild players for the given shard
     */
    public static void restoreGuildPlayers(FredBoat shard) {
        log.info("Began restoring guild players for shard {} with {} guilds",
                shard.getShardInfo().getShardId(), shard.getGuildCount());
        long started = System.currentTimeMillis();

        JDA jda = shard.getJda();

        Set<Long> guildIds = jda.getGuilds().stream().map(Guild::getIdLong).collect(Collectors.toSet());

        String query = "SELECT a.guildId FROM GuildPlayerData a WHERE a.isPaused = false";
        List<Long> activePlayers = EntityReader.selectJPQLQuery(query, Collections.emptyMap(), Long.class);

        activePlayers.retainAll(guildIds);

        int playersRestoredCount = 0;
        for (long guildId : activePlayers) {
            try {
                PlayerRegistry.get(jda, Long.toString(guildId));
                playersRestoredCount++;
            } catch (Exception e) {
                log.error("Exception when restoring guild player for guild {}", guildId, e);
            }
        }

        log.info("Restored {} guild players for shard {} in {}ms",
                playersRestoredCount, shard.getShardInfo().getShardId(), System.currentTimeMillis() - started);
    }
}
