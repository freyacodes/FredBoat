package fredboat.db.transfer

import fredboat.definitions.Module
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "GuildSettings")
data class GuildSettings(
        @Id
        override val id: Long,
        var helloSent: Boolean = false,
        var trackAnnounce: Boolean = false,
        var autoResume: Boolean = false,
        var allowPublicPlayerInfo: Boolean = false,
        var allowPlaylist: Boolean = true,
        var maxTrackLength: Long? = null,
        var maxTrackCount: Int? = null,
        var userMaxTrackCount: Int? = null,
        var lang: String = "en_US",
        var prefix: String? = null,
        var modules: List<ModuleEntity> = Module.values().toList().map { ModuleEntity(it, true) },
        var permissions: PermissionEntity = PermissionEntity(userList = listOf(id), djList = listOf(id))
) : MongoEntity<Long> {

    fun get(module: Module): ModuleEntity {
        return modules.first { it.module.name == module.name }
    }
}