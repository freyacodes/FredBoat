package fredboat.audio.queue;

/**
 * Created by napster on 16.03.17.
 * <p>
 * Helps us transfer some information about playlists.
 */
public class PlaylistInfo {

    public enum Source {PASTESERVICE, SPOTIFY}

    private int totalTracks;

    private String name;

    private Source source;

    public PlaylistInfo(int totalTracks, String name, Source source) {
        this.totalTracks = totalTracks;
        this.name = name;
        this.source = source;
    }

    public int getTotalTracks() {
        return totalTracks;
    }

    public void setTotalTracks(int totalTracks) {
        this.totalTracks = totalTracks;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }
}
