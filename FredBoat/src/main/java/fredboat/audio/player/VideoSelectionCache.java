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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fredboat.jda.TextChannelProvider;
import fredboat.messaging.CentralMessaging;
import io.prometheus.client.guava.cache.CacheMetricsCollector;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Cache and provide video selections (aka seach results shown to users, for them to select from)
 */
@Component
public class VideoSelectionCache {

    //the key looks like this: guildId:userId
    private final Cache<String, VideoSelection> videoSelections = CacheBuilder.newBuilder()
            .recordStats()
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .build();
    private final TextChannelProvider textChannelProvider;

    public VideoSelectionCache(TextChannelProvider textChannelProvider, CacheMetricsCollector cacheMetrics) {
        this.textChannelProvider = textChannelProvider;
        cacheMetrics.addCache("videoSelections", videoSelections);
    }

    @Nullable
    public VideoSelection get(@Nonnull Member member) {
        return get(member.getGuild().getIdLong(), member.getUser().getIdLong());
    }

    @Nullable
    public VideoSelection remove(@Nonnull Member member) {
        return remove(member.getGuild().getIdLong(), member.getUser().getIdLong());
    }

    public void put(@Nonnull Member member, List<AudioTrack> choices, Message outMsg) {
        videoSelections.put(asKey(member), new VideoSelection(choices, outMsg, textChannelProvider));
    }

    @Nullable
    private VideoSelection get(long guildId, long userId) {
        return videoSelections.getIfPresent(asKey(guildId, userId));
    }

    @Nullable
    private VideoSelection remove(long guildId, long userId) {
        VideoSelection result = get(guildId, userId);
        videoSelections.invalidate(asKey(guildId, userId));
        return result;
    }


    private static String asKey(@Nonnull Member member) {
        return asKey(member.getGuild().getIdLong(), member.getUser().getIdLong());
    }

    private static String asKey(long guildId, long userId) {
        return guildId + ":" + userId;
    }

    public static class VideoSelection {

        public final List<AudioTrack> choices;
        public final long outMsgId; //our own message
        public final long channelId;
        private final TextChannelProvider textChannelProvider;

        private VideoSelection(List<AudioTrack> choices, Message outMsg, TextChannelProvider textChannelProvider) {
            this.choices = choices;
            this.outMsgId = outMsg.getIdLong();
            this.channelId = outMsg.getChannel().getIdLong();
            this.textChannelProvider = textChannelProvider;
        }

        public void deleteMessage() {
            TextChannel tc = textChannelProvider.getTextChannelById(channelId);
            if (tc != null) {
                //we can call this without an additional permission check, as this should be our own message that we are
                //deleting
                CentralMessaging.deleteMessageById(tc, outMsgId);
            }
        }
    }
}
