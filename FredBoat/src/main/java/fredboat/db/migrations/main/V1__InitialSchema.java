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

package fredboat.db.migrations.main;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Created by napster on 23.12.17.
 * <p>
 * This initializes our schema at the point where we turned off hibernate-auto-ddl
 */
public class V1__InitialSchema implements JdbcMigration {

    @Override
    public void migrate(Connection connection) throws Exception {

        //UConfig
        //drop UConfig if exists; it was never used anyways, and can be readded later if we actually use it
        String dropUConfigSql = "DROP TABLE IF EXISTS public.user_config;";
        try (Statement dropUConfig = connection.createStatement()) {
            dropUConfig.execute(dropUConfigSql);
        }

        //BlacklistEntry
        String createBlacklistSql
                = "CREATE TABLE IF NOT EXISTS public.blacklist "
                + "( "
                + "    id                    BIGINT NOT NULL, "
                + "    level                 INTEGER NOT NULL, "
                + "    rate_limit_reached    INTEGER NOT NULL, "
                + "    rate_limit_timestamp  BIGINT NOT NULL, "
                + "    blacklisted_timestamp BIGINT NOT NULL, "
                + "    CONSTRAINT blacklist_pkey PRIMARY KEY (id) "
                + ");";
        try (Statement createBlacklist = connection.createStatement()) {
            createBlacklist.execute(createBlacklistSql);
        }

        //GuildConfig
        String createGuildConfigSql
                = "CREATE TABLE IF NOT EXISTS public.guild_config "
                + "( "
                + "    guildid        CHARACTER VARYING(255) COLLATE pg_catalog.\"default\" NOT NULL, "
                + "    track_announce BOOLEAN NOT NULL, "
                + "    auto_resume    BOOLEAN NOT NULL, "
                + "    lang           CHARACTER VARYING(255) COLLATE pg_catalog.\"default\" NOT NULL, "
                + "    prefix         TEXT COLLATE pg_catalog.\"default\", "
                + "    CONSTRAINT guild_config_pkey PRIMARY KEY(guildid) "
                + ");";
        try (Statement createGuildConfig = connection.createStatement()) {
            createGuildConfig.execute(createGuildConfigSql);
        }

        //GuildPermissions
        String createGuildPermissionsSql
                = "CREATE TABLE IF NOT EXISTS public.guild_permissions "
                + "( "
                + "    id         CHARACTER VARYING(255) COLLATE pg_catalog.\"default\" NOT NULL, "
                + "    list_admin TEXT COLLATE pg_catalog.\"default\" NOT NULL, "
                + "    list_dj    TEXT COLLATE pg_catalog.\"default\" NOT NULL, "
                + "    list_user  TEXT COLLATE pg_catalog.\"default\" NOT NULL, "
                + "    CONSTRAINT guild_permissions_pkey PRIMARY KEY (id) "
                + ");";
        try (Statement createGuildPermissions = connection.createStatement()) {
            createGuildPermissions.execute(createGuildPermissionsSql);
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
