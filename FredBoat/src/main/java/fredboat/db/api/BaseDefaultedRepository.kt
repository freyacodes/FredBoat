package fredboat.db.api

interface BaseDefaultedRepository<ID, T> : BaseRepository<ID, T> {

    fun default(id: ID): T
}