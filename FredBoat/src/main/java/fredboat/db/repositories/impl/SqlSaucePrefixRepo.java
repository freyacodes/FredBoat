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

import fredboat.db.entity.main.Prefix;
import fredboat.db.repositories.api.IPrefixRepo;
import space.npstr.sqlsauce.DatabaseWrapper;
import space.npstr.sqlsauce.entities.GuildBotComposite;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by napster on 05.02.18.
 */
public class SqlSaucePrefixRepo extends SqlSauceRepo<GuildBotComposite, Prefix> implements IPrefixRepo {

    public SqlSaucePrefixRepo(DatabaseWrapper dbWrapper) {
        super(dbWrapper, Prefix.class);
    }

    @Nullable
    @Override
    public String getPrefix(GuildBotComposite id) {
        //language=JPAQL
        String query = "SELECT p.prefix FROM Prefix p WHERE p.id = :id";
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);

        List<String> result = dbWrapper.selectJpqlQuery(query, params, String.class);

        if (result.isEmpty()) {
            return null;
        } else {
            return result.get(0);
        }
    }
}
