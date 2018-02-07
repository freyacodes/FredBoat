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

import fredboat.db.entity.main.BlacklistEntry;
import fredboat.db.repositories.api.IBlacklistRepo;
import space.npstr.sqlsauce.DatabaseWrapper;
import space.npstr.sqlsauce.fp.types.EntityKey;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Created by napster on 05.02.18.
 */
public class SqlSauceBlacklistRepo extends SqlSauceRepo implements IBlacklistRepo {

    public SqlSauceBlacklistRepo(DatabaseWrapper dbWrapper) {
        super(dbWrapper);
    }

    @Nullable
    @Override
    public BlacklistEntry get(Long id) {
        return dbWrapper.getEntity(EntityKey.of(id, BlacklistEntry.class));
    }

    @Override
    public void delete(Long id) {
        dbWrapper.deleteEntity(EntityKey.of(id, BlacklistEntry.class));
        // old code, which is slightly more efficient?
//        //language=SQL
//        String query = "DELETE FROM blacklist WHERE id = :id";
//        Map<String, Object> params = new HashMap<>();
//        params.put("id", id);
//
//        doUserFriendly(onMainDb(wrapper -> wrapper.executeSqlQuery(query, params)));
    }

    @Override
    public BlacklistEntry fetch(Long id) {
        return dbWrapper.getOrCreate(EntityKey.of(id, BlacklistEntry.class));
    }

    @Override
    public BlacklistEntry merge(BlacklistEntry entity) {
        return dbWrapper.merge(entity);
    }

    @Override
    public List<BlacklistEntry> loadBlacklist() {
        return dbWrapper.loadAll(BlacklistEntry.class);
    }
}
