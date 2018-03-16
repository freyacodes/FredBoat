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

package fredboat.event;

import fredboat.audio.player.AudioConnectionFacade;
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.PlayerRegistry;
import fredboat.config.property.Credentials;
import fredboat.util.DiscordUtil;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by napster on 24.02.18.
 */
@Component
public class ShardReviveHandler extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(ShardReviveHandler.class);

    private final Map<Integer, List<Long>> channelsToRejoin = new ConcurrentHashMap<>();

    private final PlayerRegistry playerRegistry;
    private final Credentials credentials;
    private final AudioConnectionFacade audioConnectionFacade;

    public ShardReviveHandler(PlayerRegistry playerRegistry, Credentials credentials, AudioConnectionFacade audioConnectionFacade) {
        this.playerRegistry = playerRegistry;
        this.credentials = credentials;
        this.audioConnectionFacade = audioConnectionFacade;
    }

    @Override
    public void onReady(ReadyEvent event) {
        //Rejoin old channels if revived
        List<Long> channels = channelsToRejoin.computeIfAbsent(event.getJDA().getShardInfo().getShardId(), ArrayList::new);
        List<Long> toRejoin = new ArrayList<>(channels);
        channels.clear();//avoid situations where this method is called twice with the same channels

        toRejoin.forEach(vcid -> {
            VoiceChannel channel = event.getJDA().getVoiceChannelById(vcid);
            if (channel == null) return;
            GuildPlayer player = playerRegistry.getOrCreate(channel.getGuild());
            audioConnectionFacade.openConnection(channel, player);
        });
    }

    @Override
    public void onShutdown(ShutdownEvent event) {
        try {
            List<Long> channels = new ArrayList<>();
            int shardId = event.getJDA().getShardInfo().getShardId();
            playerRegistry.getPlayingPlayers().stream()
                    .filter(guildPlayer -> DiscordUtil.getShardId(guildPlayer.getGuildId(), credentials) == shardId)
                    .forEach(guildPlayer -> {
                        VoiceChannel channel = guildPlayer.getCurrentVoiceChannel();
                        if (channel != null) channels.add(channel.getIdLong());
                    });
            channelsToRejoin.put(shardId, channels);
        } catch (Exception ex) {
            log.error("Caught exception while saving channels to revive shard {}", event.getJDA().getShardInfo(), ex);
        }
    }
}
