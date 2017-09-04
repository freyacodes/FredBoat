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

import com.mashape.unirest.http.exceptions.UnirestException;
import fredboat.Config;
import fredboat.FredBoat;
import fredboat.util.DiscordUtil;
import lavalink.client.io.Lavalink;
import lavalink.client.player.IPlayer;
import lavalink.client.player.LavaplayerPlayerWrapper;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;

import java.util.List;

public class LavalinkManager {

    public static final LavalinkManager ins = new LavalinkManager();

    private LavalinkManager() {
    }

    private Lavalink lavalink = null;

    public void start() {
        if (!isEnabled()) return;

        String userId;
        try {
            userId = DiscordUtil.getUserId(Config.CONFIG.getBotToken());
        } catch (UnirestException e) {
            throw new RuntimeException(e);
        }

        lavalink = new Lavalink(
                userId,
                Config.CONFIG.getNumShards(),
                shardId -> FredBoat.getInstance(shardId).getJda()
        );

        List<Config.LavalinkHost> hosts = Config.CONFIG.getLavalinkHosts();
        hosts.forEach(lavalinkHost -> lavalink.addNode(lavalinkHost.getUri(),
                lavalinkHost.getPassword()));
    }

    public boolean isEnabled() {
        return !Config.CONFIG.getLavalinkHosts().isEmpty();
    }

    IPlayer createPlayer(String guildId) {
        return isEnabled()
                ? lavalink.getPlayer(guildId)
                : new LavaplayerPlayerWrapper(AbstractPlayer.getPlayerManager().createPlayer());
    }

    public void openConnection(VoiceChannel channel) {
        if (isEnabled()) {
            lavalink.openVoiceConnection(channel);
        } else {
            channel.getGuild().getAudioManager().openAudioConnection(channel);
        }
    }

    public void closeConnection(Guild guild) {
        if (isEnabled()) {
            lavalink.closeVoiceConnection(guild);
        } else {
            guild.getAudioManager().closeAudioConnection();
        }
    }

    public VoiceChannel getConnectedChannel(Guild guild) {
        if (isEnabled()) {
            return lavalink.getConnectedChannel(guild);
        } else {
            return guild.getAudioManager().getConnectedChannel();
        }
    }

    public Lavalink getLavalink() {
        return lavalink;
    }
}
