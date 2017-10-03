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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerRegistry {

    private static final Map<String, GuildPlayer> REGISTRY = new ConcurrentHashMap<>();
    public static final float DEFAULT_VOLUME = 1f;

    public static void put(String k, GuildPlayer v) {
        REGISTRY.put(k, v);
    }

    @Nonnull
    public static GuildPlayer get(@Nonnull Guild guild) {
        return get(guild.getJDA(), guild.getId());
    }

    @Nonnull
    public static GuildPlayer get(JDA jda, String k) {
        GuildPlayer player = REGISTRY.get(k);
        if (player == null) {
            player = new GuildPlayer(jda.getGuildById(k));
            player.setVolume(DEFAULT_VOLUME);
            REGISTRY.put(k, player);
        }

        // Attempt to set the player as a sending handler. Important after a shard revive
        if (!LavalinkManager.ins.isEnabled() && jda.getGuildById(k) != null) {
            jda.getGuildById(k).getAudioManager().setSendingHandler(player);
        }

        return player;
    }

    @Nullable
    public static GuildPlayer getExisting(Guild guild) {
        return getExisting(guild.getJDA(), guild.getId());
    }

    @Nullable
    public static GuildPlayer getExisting(JDA jda, String k) {
        if (REGISTRY.containsKey(k)) {
            return get(jda, k);
        }
        return null;
    }

    public static GuildPlayer remove(String k) {
        return REGISTRY.remove(k);
    }

    public static Map<String, GuildPlayer> getRegistry() {
        return REGISTRY;
    }

    public static List<GuildPlayer> getPlayingPlayers() {
        ArrayList<GuildPlayer> plrs = new ArrayList<>();

        for (GuildPlayer plr : REGISTRY.values()) {
            if (plr.isPlaying()) {
                plrs.add(plr);
            }
        }

        return plrs;
    }

    public static void destroyPlayer(Guild g) {
        destroyPlayer(g.getJDA(), g.getId());
    }

    public static void destroyPlayer(JDA jda, String g) {
        GuildPlayer player = getExisting(jda, g);
        if (player != null) {
            player.destroy();
            remove(g);
        }
    }

}
