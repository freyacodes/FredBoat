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

import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fredboat.Config;
import fredboat.FredBoat;
import fredboat.audio.player.AbstractPlayer;
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.PlayerRegistry;
import fredboat.feature.I18n;
import fredboat.messaging.CentralMessaging;
import fredboat.shared.constant.DistributionEnum;
import fredboat.shared.constant.ExitCodes;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class MusicPersistenceHandler {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(MusicPersistenceHandler.class);

    private MusicPersistenceHandler() {
    }

    public static void handlePreShutdown(int code) {
        File dir = new File("music_persistence");
        if (!dir.exists()) {
            dir.mkdir();
        }
        HashMap<String, GuildPlayer> reg = PlayerRegistry.getRegistry();

        boolean isUpdate = code == ExitCodes.EXIT_CODE_UPDATE;
        boolean isRestart = code == ExitCodes.EXIT_CODE_RESTART;

        for (String gId : reg.keySet()) {
            try {
                GuildPlayer player = reg.get(gId);

                if (!player.isPlaying()) {
                    continue;//Nothing to see here
                }

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

                JSONObject data = new JSONObject();
                data.put("vc", player.getUserCurrentVoiceChannel(player.getGuild().getSelfMember()).getId());
                data.put("tc", activeTextChannel != null ? activeTextChannel.getId() : "");
                data.put("isPaused", player.isPaused());
                data.put("volume", Float.toString(player.getVolume()));
                data.put("repeatMode", player.getRepeatMode());
                data.put("shuffle", player.isShuffle());

                if (player.getPlayingTrack() != null) {
                    data.put("position", player.getPosition());
                }

                ArrayList<JSONObject> identifiers = new ArrayList<>();

                for (AudioTrackContext atc : player.getRemainingTracks()) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    AbstractPlayer.getPlayerManager().encodeTrack(new MessageOutput(baos), atc.getTrack());

                    JSONObject ident = new JSONObject()
                            .put("message", Base64.encodeBase64String(baos.toByteArray()))
                            .put("user", atc.getUserId());

                    if(atc instanceof SplitAudioTrackContext) {
                        JSONObject split = new JSONObject();
                        SplitAudioTrackContext c = (SplitAudioTrackContext) atc;
                        split.put("title", c.getEffectiveTitle())
                                .put("startPos", c.getStartPosition())
                                .put("endPos", c.getStartPosition() + c.getEffectiveDuration());

                        ident.put("split", split);
                    }

                    identifiers.add(ident);
                }

                data.put("sources", identifiers);

                try {
                    FileUtils.writeStringToFile(new File(dir, gId), data.toString(), Charset.forName("UTF-8"));
                } catch (IOException ex) {
                    if (activeTextChannel != null) {
                        CentralMessaging.sendMessage(activeTextChannel,
                                MessageFormat.format(I18n.get(player.getGuild()).getString("shutdownPersistenceFail"),
                                        ex.getMessage()));
                    }
                }
            } catch (Exception ex) {
                log.error("Error when saving persistence file", ex);
            }
        }
    }

    public static void reloadPlaylists() {
        File dir = new File("music_persistence");

        if(Config.CONFIG.getDistribution() == DistributionEnum.MUSIC) {
            log.warn("Music persistence loading is currently disabled!");

            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    boolean deleted = f.delete();
                    log.info(deleted ? "Deleted persistence file: " + f : "Failed to delete persistence file: " + f);
                }

                dir.delete();
            }
            return;
        }

        log.info("Began reloading playlists");
        if (!dir.exists()) {
            return;
        }
        log.info("Found persistence data: " + Arrays.toString(dir.listFiles()));

        for (File file : dir.listFiles()) {
            try {
                String gId = file.getName();
                JSONObject data = new JSONObject(FileUtils.readFileToString(file, Charset.forName("UTF-8")));

                //TODO: Make shard in-specific
                boolean isPaused = data.getBoolean("isPaused");
                final JSONArray sources = data.getJSONArray("sources");
                VoiceChannel vc = FredBoat.getVoiceChannelById(data.getString("vc"));
                TextChannel tc = FredBoat.getTextChannelById(data.getString("tc"));
                float volume = Float.parseFloat(data.getString("volume"));
                RepeatMode repeatMode = data.getEnum(RepeatMode.class, "repeatMode");
                boolean shuffle = data.getBoolean("shuffle");

                GuildPlayer player = PlayerRegistry.get(vc.getJDA(), gId);

                player.joinChannel(vc);
                if (tc != null) {
                    player.setCurrentTC(tc);
                }
                if(Config.CONFIG.getDistribution().volumeSupported()) {
                    player.setVolume(volume);
                }
                player.setRepeatMode(repeatMode);
                player.setShuffle(shuffle);

                final boolean[] isFirst = {true};

                sources.forEach((Object t) -> {
                    JSONObject json = (JSONObject) t;
                    byte[] message = Base64.decodeBase64(json.getString("message"));
                    Member member = vc.getGuild().getMember(vc.getJDA().getUserById(json.getString("user")));
                    if (member == null)
                        member = vc.getGuild().getSelfMember(); //member left the guild meanwhile, set ourselves as the one who added the song

                    AudioTrack at;
                    try {
                        ByteArrayInputStream bais = new ByteArrayInputStream(message);
                        at = AbstractPlayer.getPlayerManager().decodeTrack(new MessageInput(bais)).decodedTrack;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    if (at == null) {
                        log.error("Loaded track that was null! Skipping...");
                        return;
                    }

                    // Handle split tracks
                    AudioTrackContext atc;
                    JSONObject split = json.optJSONObject("split");
                    if(split != null) {
                        atc = new SplitAudioTrackContext(at, member,
                                split.getLong("startPos"),
                                split.getLong("endPos"),
                                split.getString("title")
                        );
                        at.setPosition(split.getLong("startPos"));

                        if (isFirst[0]) {
                            isFirst[0] = false;
                            if (data.has("position")) {
                                at.setPosition(split.getLong("startPos") + data.getLong("position"));
                            }
                        }
                    } else {
                        atc = new AudioTrackContext(at, member);

                        if (isFirst[0]) {
                            isFirst[0] = false;
                            if (data.has("position")) {
                                at.setPosition(data.getLong("position"));
                            }
                        }
                    }

                    player.queue(atc);
                });

                player.setPause(isPaused);
                CentralMessaging.sendMessage(tc, MessageFormat.format(I18n.get(player.getGuild()).getString("reloadSuccess"), sources.length()));
            } catch (Exception ex) {
                log.error("Error when loading persistence file", ex);
            }
        }

        for (File f : dir.listFiles()) {
            boolean deleted = f.delete();
            log.info(deleted ? "Deleted persistence file: " + f : "Failed to delete persistence file: " + f);
        }

        dir.delete();
    }

}
