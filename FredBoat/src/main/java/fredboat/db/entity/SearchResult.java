/*
 *
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
 */

package fredboat.db.entity;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import fredboat.FredBoat;
import fredboat.db.DatabaseManager;
import fredboat.db.DatabaseNotReadyException;
import fredboat.util.rest.SearchUtil;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PersistenceException;
import javax.persistence.Table;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by napster on 27.08.17.
 * <p>
 * Caches a search result
 */
@Entity
@Table(name = "search_results")
//todo does this need to be added to ehcache.xml?
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "search_results")
//todo after introducing the db refactoring in the persistent tracklists PR:
//- refactor load() and save() as calls to EntityReader and EntityWriter
//- make this class implement IEntity<SearchResultId>
public class SearchResult {

    private static final Logger log = LoggerFactory.getLogger(SearchResult.class);

    @Id
    private SearchResultId searchResultId;

    @Column(name = "timestamp")
    private long timestamp;

    @Lob
    @Column(name = "search_result")
    private SerializableAudioPlaylist encodedSearchResult;

    //for JPA
    public SearchResult() {
    }

    public SearchResult(AudioPlayerManager playerManager, SearchUtil.SearchProvider provider, String searchTerm,
                        AudioPlaylist searchResult) {
        this.searchResultId = new SearchResultId(provider, searchTerm);
        this.timestamp = System.currentTimeMillis();
        this.encodedSearchResult = new SerializableAudioPlaylist(playerManager, searchResult);
    }

    /**
     * @param playerManager the PlayerManager to perform encoding and decoding with
     * @param provider      the search provider that shall be used for this search
     * @param searchTerm    the query to search for
     * @param maxAgeMillis  the maximum age of the cached search result; provide a negative value for eternal cache
     * @return the cached search result; may return null for a non-existing or outdated search
     */
    public static AudioPlaylist load(AudioPlayerManager playerManager, SearchUtil.SearchProvider provider,
                                     String searchTerm, long maxAgeMillis) {
        DatabaseManager dbManager = FredBoat.getDbManager();
        if (!dbManager.isAvailable()) {
            throw new DatabaseNotReadyException();
        }

        EntityManager em = dbManager.getEntityManager();
        SearchResult sr;
        SearchResultId sId = new SearchResultId(provider, searchTerm);
        try {
            sr = em.find(SearchResult.class, sId);
        } catch (PersistenceException e) {
            log.error("Unexpected error while trying to look up a search result for provider {} and search term {}", provider.name(), searchTerm, e);
            throw new DatabaseNotReadyException(e);
        } finally {
            em.close();
        }

        if (sr != null && (maxAgeMillis < 0 || System.currentTimeMillis() < sr.timestamp + maxAgeMillis)) {
            return sr.getSearchResult(playerManager);
        } else {
            return null;
        }
    }

    /**
     * Persist a search in the database.
     *
     * @return the merged SearchResult object
     */
    public SearchResult save() {
        DatabaseManager dbManager = FredBoat.getDbManager();
        if (!dbManager.isAvailable()) {
            throw new DatabaseNotReadyException();
        }

        EntityManager em = dbManager.getEntityManager();
        try {
            em.getTransaction().begin();
            SearchResult managed = em.merge(this);
            em.getTransaction().commit();
            return managed;
        } catch (PersistenceException e) {
            log.error("Unexpected error while saving a search result for provider {} and search term {}",
                    searchResultId.provider, searchResultId.searchTerm, e);
            throw new DatabaseNotReadyException(e);
        } finally {
            em.close();
        }
    }

    public SearchResultId getId() {
        return searchResultId;
    }

    public SearchUtil.SearchProvider getProvider() {
        return searchResultId.getProvider();
    }

    public void setProvider(SearchUtil.SearchProvider provider) {
        searchResultId.provider = provider.name();
    }

    public String getSearchTerm() {
        return searchResultId.searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchResultId.searchTerm = searchTerm;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public AudioPlaylist getSearchResult(AudioPlayerManager playerManager) {
        return encodedSearchResult.decode(playerManager);
    }

    public void setSearchResult(AudioPlayerManager playerManager, AudioPlaylist searchResult) {
        this.encodedSearchResult = new SerializableAudioPlaylist(playerManager, searchResult);
    }

    /**
     * Composite primary key for SearchResults
     */
    @Embeddable
    static class SearchResultId implements Serializable {

        private static final long serialVersionUID = 8969973651938173208L;

        @Column(name = "provider", nullable = false)
        private String provider;

        @Column(name = "search_term", nullable = false)
        private String searchTerm;

        //for jpa
        public SearchResultId() {
        }

        public SearchResultId(SearchUtil.SearchProvider provider, String searchTerm) {
            this.provider = provider.name();
            this.searchTerm = searchTerm;
        }

        public SearchUtil.SearchProvider getProvider() {
            return SearchUtil.SearchProvider.valueOf(provider);
        }

        public void setProvider(SearchUtil.SearchProvider provider) {
            this.provider = provider.name();
        }

        public String getSearchTerm() {
            return searchTerm;
        }

        public void setSearchTerm(String searchTerm) {
            this.searchTerm = searchTerm;
        }

        @Override
        public int hashCode() {
            return Objects.hash(provider, searchTerm);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SearchResultId)) return false;
            SearchResultId other = (SearchResultId) o;
            return provider.equals(other.provider) && searchTerm.equals(other.searchTerm);
        }
    }


    private static class SerializableAudioPlaylist implements Serializable {
        private static final long serialVersionUID = -6823555858689776338L;

        private String name;
        private byte[][] tracks;
        private byte[] selectedTrack;
        private boolean isSearchResult;

        //required for deserialization
        SerializableAudioPlaylist() {
        }

        public SerializableAudioPlaylist(AudioPlayerManager playerManager, AudioPlaylist audioPlaylist) {
            this.name = audioPlaylist.getName();
            this.tracks = encodeTracks(playerManager, audioPlaylist.getTracks());
            this.selectedTrack = encodeTrack(playerManager, audioPlaylist.getSelectedTrack());
            this.isSearchResult = audioPlaylist.isSearchResult();
        }

        public AudioPlaylist decode(AudioPlayerManager playerManager) {
            return new BasicAudioPlaylist(name,
                    decodeTracks(playerManager, tracks),
                    decodeTrack(playerManager, selectedTrack),
                    isSearchResult);
        }

        private static byte[][] encodeTracks(AudioPlayerManager playerManager, List<AudioTrack> tracks) {
            if (tracks == null) {
                return new byte[0][];
            }

            byte[][] encoded = new byte[tracks.size()][];
            int skipped = 0;
            for (int i = 0; i < tracks.size(); i++) {
                encoded[i] = encodeTrack(playerManager, tracks.get(i));
                if (encoded[i] == null) {
                    skipped++;
                }
            }

            byte[][] result = new byte[tracks.size() - skipped][];
            int i = 0;
            for (byte[] encodedTrack : encoded) {
                if (encodedTrack != null) {
                    result[i] = encodedTrack;
                    i++;
                }
            }

            return result;
        }

        //may return null if the encoding fails or the input is null
        private static byte[] encodeTrack(AudioPlayerManager playerManager, AudioTrack track) {
            if (track == null) {
                return null;
            }
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                playerManager.encodeTrack(new MessageOutput(baos), track);
                return baos.toByteArray();
            } catch (IOException ignored) {
                return null;
            }
        }

        private static List<AudioTrack> decodeTracks(AudioPlayerManager playerManager, byte[][] input) {
            List<AudioTrack> result = new ArrayList<>();
            if (input == null) return result;

            for (byte[] track : input) {
                AudioTrack decoded = decodeTrack(playerManager, track);
                if (decoded != null) {
                    result.add(decoded);
                }
            }
            return result;
        }

        //may return null if the decoding fails or the input is null
        private static AudioTrack decodeTrack(AudioPlayerManager playerManager, byte[] input) {
            if (input == null) return null;
            ByteArrayInputStream bais = new ByteArrayInputStream(input);
            try {
                return playerManager.decodeTrack(new MessageInput(bais)).decodedTrack;
            } catch (IOException e) {
                return null;
            }
        }
    }
}
