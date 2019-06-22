package fredboat.db.mongo

import fredboat.db.api.BaseDefaultedRepository
import fredboat.db.transfer.MongoEntity
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono
import java.io.Serializable

abstract class BaseDefaultedRepositoryImpl<ID : Serializable, T : MongoEntity<ID>>(
        repo: ReactiveCrudRepository<T, ID>,
        cacheMetrics: CacheMetricsCollector,
        cacheName: String
) : BaseRepositoryImpl<ID, T>(repo, cacheMetrics, cacheName), BaseDefaultedRepository<ID, T> {

    override fun fetch(id: ID): Mono<T> {
        return super.fetch(id).defaultIfEmpty(default(id)).doOnSuccess { super.cache.synchronous().put(it.id, it) }
    }
}