package fredboat.db.transfer

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "Blacklist")
data class BlacklistEntity(
        @Id override val id: Long,
        var level: Int = -1,
        var hitCount: Int = 0,
        var lastHitTime: Long = 0,
        var blacklistTime: Long = 0
) : MongoEntity<Long> {

    fun incLevel() {
        level++
    }

    fun incHitCount() {
        hitCount++
    }
}