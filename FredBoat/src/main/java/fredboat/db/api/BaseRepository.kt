package fredboat.db.api

import reactor.core.publisher.Mono

interface BaseRepository<ID, T> {

    fun fetch(id: ID): Mono<T>

    fun update(mono: Mono<T>): Mono<T>

    fun update(target: T): Mono<T>

    fun remove(id: ID): Mono<Void>
}