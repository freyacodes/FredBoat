package fredboat.audio.source;

import fredboat.audio.queue.PlaylistInfo;

public interface PlaylistImporter {

    /**
     * this should try to acquire as much data as possible about a playlist in a lightweight way
     * if necessary, a few http requests are ok
     *
     * @param identifier the same string by which the importer may be asked to load the whole playlist
     * @return information about the playlist or null if it's not a playlist recognized by this importer
     */
    PlaylistInfo getPlaylistDataBlocking(String identifier);
}
