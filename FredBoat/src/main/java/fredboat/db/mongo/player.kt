package fredboat.db.mongo

import fredboat.db.transfer.MongoEntity
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "MongoPlayer")
class MongoPlayer(
        @Id
        override val id: Long,
        /** If the player was playing at the time of saving,
         * it may automatically be reloaded when we start the bot */
        val playing: Boolean,
        val paused: Boolean,
        val shuffled: Boolean,
        val roundRobin: Boolean,
        /** [fredboat.definitions.RepeatMode] ordinal */
        val repeat: Byte,
        /** 0-1 */
        val volume: Float,
        /** Time of the playing track at the time of saving in milliseconds */
        val position: Long?,
        /** Voice channel we were playing in if interrupted */
        val voiceChannel: Long?,
        /** Channel to send event messages in */
        val textChannel: Long?,
        val queue: List<MongoTrack>
) : MongoEntity<Long>

class MongoTrack(
        val id: ObjectId,
        val blob: ByteArray,
        val requester: Long,
        /** Used by split tracks */
        val startTime: Long?,
        /** Used by split tracks */
        val endTime: Long?,
        val title: String
)