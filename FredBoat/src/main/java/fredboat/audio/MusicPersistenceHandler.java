/*
 * MIT License
 *
 * Copyright (c) 2016 Frederik Ar. Mikkelsen
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

package fredboat.audio;

import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fredboat.FredBoat;
import fredboat.audio.queue.AudioTrackContext;
import fredboat.util.ExitCodes;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

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

        String msg;

        if (isUpdate) {
            msg = "FredBoat♪♪ is updating. This should only take a minute and will reload the current playlist.";
        } else if (isRestart) {
            msg = "FredBoat♪♪ is restarting. This should only take a minute and will reload the current playlist.";
        } else {
            msg = "FredBoat♪♪ is shutting down. Once the bot comes back the current playlist will reload.";
        }

        for (String gId : reg.keySet()) {
            try {
                GuildPlayer player = reg.get(gId);

                if (!player.isPlaying()) {
                    continue;//Nothing to see here
                }

                player.getActiveTextChannel().sendMessage(msg).queue();

                JSONObject data = new JSONObject();
                data.put("vc", player.getUserCurrentVoiceChannel(player.getGuild().getSelfMember().getUser()).getId());
                data.put("tc", player.getActiveTextChannel().getId());
                data.put("isPaused", player.isPaused());
                data.put("volume", Float.toString(player.getVolume()));
                data.put("repeat", player.isRepeat());
                data.put("shuffle", player.isShuffle());

                if (player.getPlayingTrack() != null) {
                    data.put("position", player.getPlayingTrack().getTrack().getPosition());
                }

                ArrayList<JSONObject> identifiers = new ArrayList<>();

                for (AudioTrackContext atc : player.getRemainingTracks()) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    AbstractPlayer.getPlayerManager().encodeTrack(new MessageOutput(baos), atc.getTrack());

                    JSONObject ident = new JSONObject()
                            .put("message", Base64.encodeBase64String(baos.toByteArray()))
                            .put("user", atc.getMember().getUser().getId());

                    identifiers.add(ident);
                }

                data.put("sources", identifiers);

                try {
                    FileUtils.writeStringToFile(new File(dir, gId), data.toString(), Charset.forName("UTF-8"));
                } catch (IOException ex) {
                    player.getActiveTextChannel().sendMessage("Error occurred when saving persistence file: " + ex.getMessage()).queue();
                }
            } catch (Exception ex) {
                log.error("Error when saving persistence file", ex);
            }
        }
    }

    public static void reloadPlaylists() {
        log.info("Began reloading playlists");
        File dir = new File("music_persistence");
        if (!dir.exists()) {
            return;
        }
        log.info("Found persistence data: " + Arrays.toString(dir.listFiles()));

        for (File file : dir.listFiles()) {
            InputStream is = null;
            try {
                String gId = file.getName();
                is = new FileInputStream(file);
                Scanner scanner = new Scanner(is);
                JSONObject data = new JSONObject(scanner.useDelimiter("\\A").next());
                scanner.close();

                //TODO: Make shard in-specific
                boolean isPaused = data.getBoolean("isPaused");
                final JSONArray sources = data.getJSONArray("sources");
                VoiceChannel vc = FredBoat.getVoiceChannelById(data.getString("vc"));
                TextChannel tc = FredBoat.getTextChannelById(data.getString("tc"));
                float volume = Float.parseFloat(data.getString("volume"));
                boolean repeat = data.getBoolean("repeat");
                boolean shuffle = data.getBoolean("shuffle");

                GuildPlayer player = PlayerRegistry.get(vc.getJDA(), gId);

                player.joinChannel(vc);
                player.setCurrentTC(tc);
                if(FredBoat.distribution.volumeSupported()) {
                    player.setVolume(volume);
                }
                player.setRepeat(repeat);
                player.setShuffle(shuffle);

                final boolean[] isFirst = {true};

                sources.forEach((Object t) -> {
                    JSONObject json = (JSONObject) t;
                    byte[] message = Base64.decodeBase64(json.getString("message"));
                    Member member = vc.getGuild().getMember(vc.getJDA().getUserById(json.getString("user")));

                    AudioTrack at;
                    try {
                        ByteArrayInputStream bais = new ByteArrayInputStream(message);
                        at = AbstractPlayer.getPlayerManager().decodeTrack(new MessageInput(bais)).decodedTrack;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    if (at == null) {
                        log.error("Loaded track that was null! Skipping...");
                    }

                    if (isFirst[0]) {
                        isFirst[0] = false;
                        if (data.has("position")) {
                            at.setPosition(data.getLong("position"));
                        }
                    }

                    player.queue(new AudioTrackContext(at, member));
                });

                player.setPause(isPaused);
                tc.sendMessage("Reloading playlist. `" + sources.length() + "` tracks found.").queue();
            } catch (Exception ex) {
                log.error("Error when loading persistence file", ex);
            } finally {
                try {
                    is.close();
                } catch (Exception ex) {
                    log.error("Error when closing InputStream after error", ex);
                }
            }
        }

        for (File f : dir.listFiles()) {
            boolean deleted = f.delete();
            log.info(deleted ? "Deleted persistence file: " + f : "Failed to delete persistence file: " + f);
        }

        dir.delete();
    }

}
