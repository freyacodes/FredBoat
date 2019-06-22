package fredboat.db.transfer

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist
import fredboat.definitions.SearchProvider
import lavalink.client.LavalinkUtil
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.io.Serializable

@Document(collection = "SearchResult")
data class SearchResult(
        @Id
        override val id: SearchResultId,
        val timestamp: Long,
        val playlist: SerializableAudioPlaylist
) : MongoEntity<SearchResultId> {

    constructor(provider: SearchProvider, query: String, playlist: AudioPlaylist)
            : this(SearchResultId(provider, query), System.currentTimeMillis(), SerializableAudioPlaylist(playlist))

    fun getSearchResult(): AudioPlaylist {
        return BasicAudioPlaylist(
                playlist.name,
                playlist.tracks.map { LavalinkUtil.toAudioTrack(it) },
                if (playlist.selectedTrack != null) LavalinkUtil.toAudioTrack(playlist.selectedTrack) else null,
                playlist.isSearch ?: false)
    }
}

data class SearchResultId(val name: String, val query: String) : Serializable {
    constructor(provider: SearchProvider, query: String) : this(provider.name, query)
}

data class SerializableAudioPlaylist(
        val name: String?,
        val tracks: List<ByteArray>,
        val selectedTrack: ByteArray?,
        val isSearch: Boolean?
) : Serializable {

    constructor(playlist: AudioPlaylist) : this(
            playlist.name,
            playlist.tracks.map { LavalinkUtil.toBinary(it) },
            if (playlist.selectedTrack != null) LavalinkUtil.toBinary(playlist.selectedTrack) else null,
            playlist.isSearchResult
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SerializableAudioPlaylist

        if (name != other.name) return false
        if (tracks != other.tracks) return false
        if (selectedTrack != null) {
            if (other.selectedTrack == null) return false
            if (!selectedTrack.contentEquals(other.selectedTrack)) return false
        } else if (other.selectedTrack != null) return false
        if (isSearch != other.isSearch) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + tracks.hashCode()
        result = 31 * result + (selectedTrack?.contentHashCode() ?: 0)
        result = 31 * result + isSearch.hashCode()
        return result
    }
}