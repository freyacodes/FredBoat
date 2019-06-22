package fredboat.db.transfer

import java.io.Serializable

interface MongoEntity<ID : Serializable> {
    val id: ID
}