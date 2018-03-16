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

package fredboat.db.migrations.main;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Created by napster on 29.12.17.
 */
public class V3__CommandModules implements JdbcMigration {

    private static final String DROP
            = "DROP TABLE IF EXISTS public.guild_modules;";

    private static final String CREATE
            = "CREATE TABLE public.guild_modules "
            + "( "
            + "    guild_id BIGINT NOT NULL, "
            + "    admin    BOOLEAN, "
            + "    info     BOOLEAN, "
            + "    config   BOOLEAN, "
            + "    music    BOOLEAN, "
            + "    mod      BOOLEAN, "
            + "    util     BOOLEAN, "
            + "    fun      BOOLEAN, "
            + "    CONSTRAINT guild_modules_pkey PRIMARY KEY (guild_id) "
            + ");";

    @Override
    public void migrate(Connection connection) throws Exception {
        try (Statement drop = connection.createStatement()) {
            drop.execute(DROP);
        }
        try (Statement create = connection.createStatement()) {
            create.execute(CREATE);
        }
    }
}
