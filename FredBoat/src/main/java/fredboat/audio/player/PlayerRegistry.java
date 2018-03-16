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

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import fredboat.db.api.GuildConfigService;
import fredboat.jda.JdaEntityProvider;
import fredboat.util.ratelimit.Ratelimiter;
import fredboat.util.rest.YoutubeAPI;
import net.dv8tion.jda.core.entities.Guild;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class PlayerRegistry {

    public static final float DEFAULT_VOLUME = 1f;

    private final Map<Long, GuildPlayer> registry = new ConcurrentHashMap<>();
    private final JdaEntityProvider jdaEntityProvider;
    private final AudioConnectionFacade audioConnectionFacade;
    private final GuildConfigService guildConfigService;
    private final AudioPlayerManager audioPlayerManager;
    private final Ratelimiter ratelimiter;
    private final YoutubeAPI youtubeAPI;
    private final MusicTextChannelProvider musicTextChannelProvider;

    public PlayerRegistry(MusicTextChannelProvider musicTextChannelProvider, JdaEntityProvider jdaEntityProvider,
                          AudioConnectionFacade audioConnectionFacade, GuildConfigService guildConfigService,
                          @Qualifier("loadAudioPlayerManager") AudioPlayerManager audioPlayerManager,
                          Ratelimiter ratelimiter, YoutubeAPI youtubeAPI) {
        this.musicTextChannelProvider = musicTextChannelProvider;
        this.jdaEntityProvider = jdaEntityProvider;
        this.audioConnectionFacade = audioConnectionFacade;
        this.guildConfigService = guildConfigService;
        this.audioPlayerManager = audioPlayerManager;
        this.ratelimiter = ratelimiter;
        this.youtubeAPI = youtubeAPI;
    }

    @Nonnull
    public GuildPlayer getOrCreate(@Nonnull Guild guild) {
        return registry.computeIfAbsent(
                guild.getIdLong(), guildId -> {
                    GuildPlayer p = new GuildPlayer(guild, musicTextChannelProvider, jdaEntityProvider,
                            audioConnectionFacade, audioPlayerManager, guildConfigService, ratelimiter, youtubeAPI);
                    p.setVolume(DEFAULT_VOLUME);
                    return p;
                });
    }

    @Nullable
    public GuildPlayer getExisting(@Nonnull Guild guild) {
        return getExisting(guild.getIdLong());
    }

    @Nullable
    public GuildPlayer getExisting(long guildId) {
        return registry.get(guildId);
    }

    public Map<Long, GuildPlayer> getRegistry() {
        return registry;

    }

    public List<GuildPlayer> getPlayingPlayers() {
        return registry.values().stream()
                .filter(GuildPlayer::isPlaying)
                .collect(Collectors.toList());
    }

    public void destroyPlayer(Guild g) {
        destroyPlayer(g.getIdLong());
    }

    public void destroyPlayer(long guildId) {
        GuildPlayer player = getExisting(guildId);
        if (player != null) {
            player.destroy();
            registry.remove(guildId);
        }
    }

    public long playingCount() {
        return registry.values().stream()
                .filter(GuildPlayer::isPlaying)
                .count();
    }
}
