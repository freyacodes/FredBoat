package fredboat.db.mongo

import fredboat.db.api.GuildSettingsRepository
import fredboat.db.transfer.GuildSettings
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import java.util.*

class GuildSettingsRepositoryImpl(
        repo: InternalGuildSettingsRepository,
        cacheMetrics: CacheMetricsCollector,
        cacheName: String
) : BaseDefaultedRepositoryImpl<Long, GuildSettings>(repo, cacheMetrics, cacheName), GuildSettingsRepository {

    override fun default(id: Long): GuildSettings {
        return GuildSettings(id)
    }

}