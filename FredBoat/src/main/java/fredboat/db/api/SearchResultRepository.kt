package fredboat.db.api

import fredboat.db.transfer.SearchResult
import fredboat.db.transfer.SearchResultId

interface SearchResultRepository : BaseRepository<SearchResultId, SearchResult>