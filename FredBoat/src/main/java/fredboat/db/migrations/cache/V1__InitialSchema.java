/*
 *
 * MIT License
 *
 * Copyright (c) 2017 Frederik Ar. Mikkelsen
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

package fredboat.db.migrations.cache;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Created by napster on 23.12.17.
 */
public class V1__InitialSchema implements JdbcMigration {
    @Override
    public void migrate(Connection connection) throws Exception {

        //SearchResult
        String createSearchResultsSql
                = "CREATE TABLE IF NOT EXISTS public.search_results "
                + "( "
                + "    provider      CHARACTER VARYING(255) COLLATE pg_catalog.\"default\" NOT NULL, "
                + "    search_term   TEXT COLLATE pg_catalog.\"default\" NOT NULL, "
                + "    search_result OID, "
                + "    \"timestamp\" BIGINT, "
                + "    CONSTRAINT search_results_pkey PRIMARY KEY (provider, search_term) "
                + ");";
        try (Statement createSearchResults = connection.createStatement()) {
            createSearchResults.execute(createSearchResultsSql);
        }

        //HStorex (from sqlsauce, requires hstore extension to be enabled)
        String createHstorexSql
                = "CREATE TABLE IF NOT EXISTS public.hstorex "
                + "( "
                + "    name    TEXT COLLATE pg_catalog.\"default\" NOT NULL, "
                + "    hstorex HSTORE, "
                + "    CONSTRAINT hstorex_pkey PRIMARY KEY (name) "
                + ")";
        try (Statement createHstorex = connection.createStatement()) {
            createHstorex.execute(createHstorexSql);
        }
    }
}
