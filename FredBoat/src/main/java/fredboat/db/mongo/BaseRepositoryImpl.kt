package fredboat.db.mongo

import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import com.github.benmanes.caffeine.cache.Caffeine
import fredboat.db.api.BaseRepository
import fredboat.db.transfer.MongoEntity
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.io.Serializable
import java.util.concurrent.TimeUnit

abstract class BaseRepositoryImpl<ID : Serializable, T : MongoEntity<ID>>(
        private val repo: ReactiveCrudRepository<T, ID>,
        cacheMetrics: CacheMetricsCollector,
        cacheName: String
) : ReactiveCrudRepository<T, ID> by repo, BaseRepository<ID, T> {

    protected val cache: AsyncLoadingCache<ID, T> = Caffeine.newBuilder()
            .expireAfterAccess(60, TimeUnit.SECONDS)
            .expireAfterWrite(120, TimeUnit.SECONDS)
            .recordStats()
            .buildAsync<ID, T> { key, _ -> repo.findById(key).toFuture() }

    init {
        cacheMetrics.addCache(cacheName, cache)
    }

    override fun fetch(id: ID): Mono<T> {
        return cache[id].toMono()
    }

    override fun update(mono: Mono<T>): Mono<T> {
        return mono.flatMap { cache.synchronous().put(it.id, it); save(it) }
    }

    override fun update(target: T): Mono<T> {
        return repo.save(target).doOnSuccess { cache.synchronous().put(target.id, target) }
    }

    override fun remove(id: ID): Mono<Void> {
        return repo.deleteById(id).doOnSuccess { cache.synchronous().invalidate(id) }
    }
}