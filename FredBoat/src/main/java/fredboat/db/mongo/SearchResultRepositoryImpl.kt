package fredboat.db.mongo

import fredboat.db.api.SearchResultRepository
import fredboat.db.transfer.SearchResult
import fredboat.db.transfer.SearchResultId
import io.prometheus.client.cache.caffeine.CacheMetricsCollector

class SearchResultRepositoryImpl(
        repo: InternalSearchResultRepository,
        cacheMetrics: CacheMetricsCollector,
        cacheName: String
) : BaseRepositoryImpl<SearchResultId, SearchResult>(repo, cacheMetrics, cacheName), SearchResultRepository