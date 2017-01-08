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

package fredboat.audio.source;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;
import fredboat.audio.AbstractPlayer;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaylistImportSourceManager implements AudioSourceManager {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(PlaylistImportSourceManager.class);

    private static final Pattern PLAYLIST_PATTERN = Pattern.compile("^https?:\\/\\/hastebin\\.com\\/(?:raw\\/)?(\\w+)(?:\\..+)?$");
    private static final AudioPlayerManager PRIVATE_MANAGER = AbstractPlayer.registerSourceManagers(new DefaultAudioPlayerManager());

    @Override
    public String getSourceName() {
        return "playlist_import";
    }

    @Override
    public AudioItem loadItem(DefaultAudioPlayerManager manager, AudioReference ar) {
        Matcher m = PLAYLIST_PATTERN.matcher(ar.identifier);

        if (!m.find()) {
            return null;
        }

        String hasteId = m.group(1);
        String response;
        try {
            response = Unirest.get("http://hastebin.com/raw/" + hasteId).asString().getBody();
        } catch (UnirestException ex) {
            throw new FriendlyException("Couldn't load playlist. Either Hastebin is down or the playlist does not exist.", FriendlyException.Severity.FAULT, ex);
        }

        String[] unfiltered = response.split("\\s");
        ArrayList<String> filtered = new ArrayList<>();
        for (String str : unfiltered) {
            if (!str.equals("")) {
                filtered.add(str);
            }
        }

        HastebinAudioResultHandler handler = new HastebinAudioResultHandler();
        Future<Void> lastFuture = null;
        for (String id : filtered) {
            lastFuture = PRIVATE_MANAGER.loadItemOrdered(handler, id, handler);
        }
        
        if(lastFuture == null){
            return null;
        }

        try {
            lastFuture.get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new FriendlyException("Failed loading playlist item", FriendlyException.Severity.FAULT, ex);
        }

        return new BasicAudioPlaylist(hasteId, handler.getLoadedTracks(), null, false);
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return false;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
        throw new UnsupportedOperationException("This source manager is only for loading playlists");
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        throw new UnsupportedOperationException("This source manager is only for loading playlists");
    }

    @Override
    public void shutdown() {
    }

    private class HastebinAudioResultHandler implements AudioLoadResultHandler {

        private final List<AudioTrack> loadedTracks;

        private HastebinAudioResultHandler() {
            this.loadedTracks = new ArrayList<>();
        }

        @Override
        public void trackLoaded(AudioTrack track) {
            loadedTracks.add(track);
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist) {
            log.info("Attempt to load a playlist recursively, skipping");
        }

        @Override
        public void noMatches() {
            //ignore
        }

        @Override
        public void loadFailed(FriendlyException exception) {
            log.debug("Failed loading track provided via hastebin ", exception);
        }

        public List<AudioTrack> getLoadedTracks() {
            return loadedTracks;
        }

    }

}
