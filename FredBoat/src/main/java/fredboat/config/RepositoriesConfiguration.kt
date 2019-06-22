package fredboat.config

import fredboat.db.api.BlacklistRepository
import fredboat.db.api.GuildSettingsRepository
import fredboat.db.api.SearchResultRepository
import fredboat.db.mongo.*
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RepositoriesConfiguration(private val cacheMetrics: CacheMetricsCollector) {

    @Bean
    fun guildSettingsRepository(repo: InternalGuildSettingsRepository): GuildSettingsRepository {
        return GuildSettingsRepositoryImpl(repo, cacheMetrics, GuildSettingsRepository::class.java.simpleName)
    }

    @Bean
    fun blacklistRepository(repo: InternalBlacklistRepository): BlacklistRepository {
        return BlacklistRepositoryImpl(repo, cacheMetrics, BlacklistRepository::class.java.simpleName)
    }

    @Bean
    fun searchResultRepository(repo: InternalSearchResultRepository): SearchResultRepository {
        return SearchResultRepositoryImpl(repo, cacheMetrics, SearchResultRepository::class.java.simpleName)
    }
}