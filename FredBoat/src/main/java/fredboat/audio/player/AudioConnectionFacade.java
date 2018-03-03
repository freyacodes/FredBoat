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
import fredboat.config.property.Credentials;
import fredboat.config.property.LavalinkConfig;
import fredboat.jda.ShardProvider;
import fredboat.util.DiscordUtil;
import lavalink.client.io.Lavalink;
import lavalink.client.io.metrics.LavalinkCollector;
import lavalink.client.player.IPlayer;
import lavalink.client.player.LavaplayerPlayerWrapper;
import net.dv8tion.jda.core.audio.AudioSendHandler;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.managers.AudioManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Delegate audio connections either to the local JDA instances or remote lavalink nodes
 */
@Component
public class AudioConnectionFacade implements EventListener {

    @Nullable
    private final Lavalink lavalink;
    private final DebugConnectionListenerProvider debugConnectionListenerProvider;
    private final AudioPlayerManager audioPlayerManager;

    public AudioConnectionFacade(LavalinkConfig lavalinkConfig, Credentials credentials,
                                 DebugConnectionListenerProvider debugConnectionListenerProvider,
                                 @Qualifier("loadAudioPlayerManager") AudioPlayerManager audioPlayerManager,
                                 ShardProvider shardProvider) {
        this.debugConnectionListenerProvider = debugConnectionListenerProvider;
        this.audioPlayerManager = audioPlayerManager;
        if (lavalinkConfig.getNodes().isEmpty()) {
            lavalink = null; //local playback
            audioPlayerManager.enableGcMonitoring();
            return;
        }

        lavalink = new Lavalink(
                Long.toString(DiscordUtil.getBotId(credentials)),
                credentials.getRecommendedShardCount(),
                shardProvider::getShardById
        );
        Runtime.getRuntime().addShutdownHook(new Thread(lavalink::shutdown, "lavalink-shutdown-hook"));

        List<LavalinkConfig.LavalinkNode> nodes = lavalinkConfig.getNodes();
        nodes.forEach(lavalinkNode -> lavalink.addNode(lavalinkNode.getName(), lavalinkNode.getUri(),
                lavalinkNode.getPassword()));

        new LavalinkCollector(lavalink).register();
    }

    public boolean isLocal() {
        return lavalink == null;
    }

    IPlayer createPlayer(String guildId) {
        return lavalink == null
                ? new LavaplayerPlayerWrapper(audioPlayerManager.createPlayer())
                : lavalink.getLink(guildId).getPlayer();
    }

    /**
     * Open a connection to a channel and set a send handler
     */
    public void openConnection(VoiceChannel channel, AudioSendHandler audioSendHandler) {
        if (lavalink == null) {
            AudioManager audioManager = channel.getGuild().getAudioManager();
            audioManager.openAudioConnection(channel);
            audioManager.setSendingHandler(audioSendHandler);
            audioManager.setConnectionListener(debugConnectionListenerProvider.get(channel.getGuild()));
        } else {
            lavalink.getLink(channel.getGuild()).connect(channel);
        }
    }

    public void closeConnection(Guild guild) {
        if (lavalink == null) {
            guild.getAudioManager().closeAudioConnection();
        } else {
            lavalink.getLink(guild).disconnect();
        }
    }

    @Nullable
    public Lavalink getLavalink() {
        return lavalink;
    }

    @Override
    public void onEvent(Event event) {
        if (lavalink != null) {
            lavalink.onEvent(event);
        }
    }
}
