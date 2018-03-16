/*
 * MIT License
 *
 * Copyright (c) 2017-2018 Frederik Ar. Mikkelsen
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
 */

package fredboat.audio.player;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fredboat.config.property.Credentials;
import io.prometheus.client.guava.cache.CacheMetricsCollector;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Created by napster on 24.02.18.
 */
@Component
public class MusicTextChannelProvider {

    private static final Logger log = LoggerFactory.getLogger(MusicTextChannelProvider.class);

    //guild id <-> channel id
    private final Cache<Long, Long> musicTextChannels;

    public MusicTextChannelProvider(Credentials credentials, CacheMetricsCollector cacheMetrics) {
        musicTextChannels = CacheBuilder.newBuilder()
                .recordStats()
                .concurrencyLevel(credentials.getRecommendedShardCount())
                //todo: evict after some time?
                .build();

        cacheMetrics.addCache("musicTextChannels", musicTextChannels);
    }

    public void setMusicChannel(TextChannel textChannel) {
        musicTextChannels.put(textChannel.getGuild().getIdLong(), textChannel.getIdLong());
    }

    /**
     * @return id of the music textchannel, or 0 if none has been recorded
     */
    public long getMusicTextChannelId(long guildId) {
        Long textChannelId = musicTextChannels.getIfPresent(guildId);
        if (textChannelId == null) {
            return 0;
        }
        return textChannelId;
    }

    /**
     * @return may return null if we never saved  left the guild, the channel was deleted, or there is no channel where we can talk
     * in that guild
     */
    @Nullable
    public TextChannel getMusicTextChannel(@Nonnull Guild guild) {
        TextChannel textChannel = guild.getTextChannelById(getMusicTextChannelId(guild.getIdLong()));

        if (textChannel != null) {
            return textChannel;
        } else {
            log.warn("No currentTC in guild {}! Trying to look up a channel where we can talk...", guild);
            for (TextChannel tc : guild.getTextChannels()) {
                if (tc.canTalk()) {
                    return tc;
                }
            }
            return null;
        }
    }
}
