package fredboat.db.mongo

import fredboat.db.api.BlacklistRepository
import fredboat.db.transfer.BlacklistEntity
import io.prometheus.client.cache.caffeine.CacheMetricsCollector

class BlacklistRepositoryImpl(
        repo: InternalBlacklistRepository,
        cacheMetrics: CacheMetricsCollector,
        cacheName: String
) : BaseDefaultedRepositoryImpl<Long, BlacklistEntity>(repo, cacheMetrics, cacheName), BlacklistRepository {

    override fun default(id: Long): BlacklistEntity {
        return BlacklistEntity(id)
    }
}