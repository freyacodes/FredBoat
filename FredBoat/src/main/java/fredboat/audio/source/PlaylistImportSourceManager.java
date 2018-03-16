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

package fredboat.audio.source;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;
import fredboat.audio.queue.PlaylistInfo;
import fredboat.main.BotController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;

public class PlaylistImportSourceManager implements AudioSourceManager, PlaylistImporter {

    private static final Logger log = LoggerFactory.getLogger(PlaylistImportSourceManager.class);

    private final AudioPlayerManager audioPlayerManager;

    public PlaylistImportSourceManager(AudioPlayerManager audioPlayerManager) {
        this.audioPlayerManager = audioPlayerManager;
    }

    @Override
    public String getSourceName() {
        return "playlist_import";
    }

    @Override
    public AudioItem loadItem(DefaultAudioPlayerManager manager, AudioReference ar) {

        String[] parsed = parse(ar.identifier);
        if (parsed == null) return null;
        String serviceName = parsed[0];
        String pasteId = parsed[1];


        if (pasteId == null || "".equals(pasteId) || !PasteServiceConstants.PASTE_SERVICE_URLS.containsKey(serviceName)) {
            return null;
        }
        List<String> trackIds = loadAndParseTrackIds(serviceName, pasteId);

        PasteServiceAudioResultHandler handler = new PasteServiceAudioResultHandler();
        Future<Void> lastFuture = null;
        for (String id : trackIds) {
            lastFuture = audioPlayerManager.loadItemOrdered(handler, id, handler);
        }

        if (lastFuture == null) {
            return null;
        }

        try {
            lastFuture.get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new FriendlyException("Failed loading playlist item", FriendlyException.Severity.FAULT, ex);
        }

        return new BasicAudioPlaylist(pasteId, handler.getLoadedTracks(), null, false);
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return false;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) {
        throw new UnsupportedOperationException("This source manager is only for loading playlists");
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) {
        throw new UnsupportedOperationException("This source manager is only for loading playlists");
    }

    @Override
    public void shutdown() {
    }

    /**
     * @return null or a string array containing the service name at [0] and the paste id at [1] of the requested playlist
     */
    private String[] parse(String identifier) {
        String pasteId;
        Matcher m;
        Matcher serviceNameMatcher = PasteServiceConstants.SERVICE_NAME_PATTERN.matcher(identifier);

        if (!serviceNameMatcher.find()) {
            return null;
        }

        String serviceName = serviceNameMatcher.group(1).trim().toLowerCase();

        switch (serviceName) {

            case "hastebin":
                m = PasteServiceConstants.HASTEBIN_PATTERN.matcher(identifier);
                pasteId = m.find() ? m.group(1) : null;
                break;

            case "wastebin":
                m = PasteServiceConstants.WASTEBIN_PATTERN.matcher(identifier);
                pasteId = m.find() ? m.group(1) : null;
                break;

            case "pastebin":
                m = PasteServiceConstants.PASTEBIN_PATTERN.matcher(identifier);
                pasteId = m.find() ? m.group(1) : null;
                break;

            default:
                return null;
        }

        String[] result = new String[2];
        result[0] = serviceName;
        result[1] = pasteId;
        return result;
    }

    private List<String> loadAndParseTrackIds(String serviceName, String pasteId) {
        String response;
        try {
            response = BotController.HTTP.get(PasteServiceConstants.PASTE_SERVICE_URLS.get(serviceName) + pasteId).asString();
        } catch (IOException ex) {
            throw new FriendlyException(
                    "Couldn't load playlist. Either " + serviceName + " is down or the playlist does not exist.",
                    FriendlyException.Severity.FAULT, ex);
        }

        String[] unfiltered = response.split("\\s");
        ArrayList<String> filtered = new ArrayList<>();
        for (String str : unfiltered) {
            if (!str.equals("")) {
                filtered.add(str);
            }
        }
        return filtered;
    }


    @Override
    public PlaylistInfo getPlaylistDataBlocking(String identifier) {

        String[] pasteData = parse(identifier);
        if (pasteData == null) return null;

        String serviceName = pasteData[0];
        String pasteId = pasteData[1];
        if (serviceName == null || "".equals(serviceName) || pasteId == null || "".equals(pasteId)) return null;

        List<String> trackIds = loadAndParseTrackIds(serviceName, pasteId);

        return new PlaylistInfo(trackIds.size(), pasteId, PlaylistInfo.Source.PASTESERVICE);
    }

    private class PasteServiceAudioResultHandler implements AudioLoadResultHandler {

        private final List<AudioTrack> loadedTracks;

        private PasteServiceAudioResultHandler() {
            this.loadedTracks = new ArrayList<>();
        }

        @Override
        public void trackLoaded(AudioTrack track) {
            loadedTracks.add(track);
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist) {
            loadedTracks.addAll(playlist.getTracks());
        }

        @Override
        public void noMatches() {
            // ignore
        }

        @Override
        public void loadFailed(FriendlyException exception) {
            log.debug("Failed loading track provided via the paste service", exception);
        }

        public List<AudioTrack> getLoadedTracks() {
            return loadedTracks;
        }

    }

}
