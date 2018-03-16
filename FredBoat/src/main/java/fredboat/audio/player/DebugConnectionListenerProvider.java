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
import fredboat.util.rest.CacheUtil;
import io.prometheus.client.guava.cache.CacheMetricsCollector;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.audio.hooks.ConnectionListener;
import net.dv8tion.jda.core.audio.hooks.ConnectionStatus;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DebugConnectionListenerProvider {

    private final Cache<Long, DebugConnectionListener> debugConnectionListeners = CacheBuilder.newBuilder()
            .recordStats()
            //todo expire them? they are pretty lightweight
            .build();

    public DebugConnectionListenerProvider(CacheMetricsCollector cacheMetrics) {
        cacheMetrics.addCache("debugConnectionListeners", debugConnectionListeners);
    }

    public DebugConnectionListener get(Guild guild) {
        return CacheUtil.getUncheckedUnwrapped(debugConnectionListeners, guild.getIdLong(),
                () -> new DebugConnectionListener(guild));
    }

    private static class DebugConnectionListener implements ConnectionListener {

        private static final Logger log = LoggerFactory.getLogger(DebugConnectionListener.class);

        private ConnectionStatus oldStatus = null;
        private final long guildId;
        private final JDA.ShardInfo shardInfo;

        DebugConnectionListener(Guild guild) {
            this.guildId = guild.getIdLong();
            this.shardInfo = guild.getJDA().getShardInfo();
        }

        @Override
        public void onPing(long l) {

        }

        @Override
        public void onStatusChange(ConnectionStatus connectionStatus) {
            log.debug(String.format("Status change for audio connection in guild %s in %s: %s => %s",
                    guildId, shardInfo, oldStatus, connectionStatus));

            oldStatus = connectionStatus;
        }

        @Override
        public void onUserSpeaking(User user, boolean b) {

        }
    }
}
