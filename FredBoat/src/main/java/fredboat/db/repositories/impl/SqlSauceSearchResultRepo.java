/*
 *
 * MIT License
 *
 * Copyright (c) 2017-2018 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package fredboat.db.repositories.impl;

import fredboat.db.entity.cache.SearchResult;
import fredboat.db.repositories.api.ISearchResultRepo;
import space.npstr.sqlsauce.DatabaseWrapper;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by napster on 05.02.18.
 */
public class SqlSauceSearchResultRepo extends SqlSauceRepo<SearchResult.SearchResultId, SearchResult>
        implements ISearchResultRepo {

    public SqlSauceSearchResultRepo(DatabaseWrapper dbWrapper) {
        super(dbWrapper, SearchResult.class);
    }

    @Nullable
    @Override
    public SearchResult getMaxAged(SearchResult.SearchResultId id, long maxAgeMillis) {
        //language=JPAQL
        String query = "SELECT sr FROM SearchResult sr WHERE sr.searchResultId = :id AND sr.timestamp > :oldest";
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("oldest", maxAgeMillis < 0 ? 0 : System.currentTimeMillis() - maxAgeMillis);

        List<SearchResult> queryResult = dbWrapper.selectJpqlQuery(query, params, SearchResult.class, 1);

        if (queryResult.isEmpty()) {
            return null;
        } else {
            return queryResult.get(0);
        }
    }
}
