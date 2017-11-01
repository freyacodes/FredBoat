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

package fredboat.audio.player;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PlayerRegistry {

    public static final float DEFAULT_VOLUME = 1f;

    private final Map<Long, GuildPlayer> REGISTRY = new ConcurrentHashMap<>();

    //internal holder pattern
    private static PlayerRegistry instance() {
        return RegistryHolder.INSTANCE;
    }

    private static class RegistryHolder {
        private static final PlayerRegistry INSTANCE = new PlayerRegistry();
    }

    public static void put(long guildId, GuildPlayer guildPlayer) {
        instance().REGISTRY.put(guildId, guildPlayer);
    }

    @Nonnull
    public static GuildPlayer getOrCreate(@Nonnull Guild guild) {
        return getOrCreate(guild.getJDA(), guild.getIdLong());
    }

    @Nonnull
    public static GuildPlayer getOrCreate(JDA jda, long guildId) {
        GuildPlayer player = instance().REGISTRY.get(guildId);
        if (player == null) {
            player = new GuildPlayer(jda.getGuildById(guildId));
            player.setVolume(DEFAULT_VOLUME);
            instance().REGISTRY.put(guildId, player);
        }

        // Attempt to set the player as a sending handler. Important after a shard revive
        if (!LavalinkManager.ins.isEnabled() && jda.getGuildById(guildId) != null) {
            jda.getGuildById(guildId).getAudioManager().setSendingHandler(player);
        }

        return player;
    }

    @Nullable
    public static GuildPlayer getExisting(@Nonnull Guild guild) {
        return getExisting(guild.getJDA(), guild.getIdLong());
    }

    @Nullable
    public static GuildPlayer getExisting(JDA jda, long guildId) {
        if (instance().REGISTRY.containsKey(guildId)) {
            return getOrCreate(jda, guildId);
        }
        return null;
    }

    public static GuildPlayer remove(long guildId) {
        return instance().REGISTRY.remove(guildId);
    }

    public static Map<Long, GuildPlayer> getRegistry() {
        return instance().REGISTRY;

    }

    public static List<GuildPlayer> getPlayingPlayers() {
        return instance().REGISTRY.values().stream()
                .filter(GuildPlayer::isPlaying)
                .collect(Collectors.toList());
    }

    public static void destroyPlayer(Guild g) {
        destroyPlayer(g.getJDA(), g.getIdLong());
    }

    public static void destroyPlayer(JDA jda, long guildId) {
        GuildPlayer player = getExisting(jda, guildId);
        if (player != null) {
            player.destroy();
            remove(guildId);
        }
    }

    public static long playingCount() {
        return instance().REGISTRY.values().stream()
                .filter(GuildPlayer::isPlaying)
                .count();
    }
}
