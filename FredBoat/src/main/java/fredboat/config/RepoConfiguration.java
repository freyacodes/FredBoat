/*
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

package fredboat.config;

import com.google.gson.Gson;
import fredboat.config.property.BackendConfig;
import fredboat.db.DatabaseManager;
import fredboat.db.repositories.api.*;
import fredboat.db.repositories.impl.*;
import fredboat.db.repositories.impl.rest.*;
import fredboat.main.BotController;
import fredboat.util.rest.Http;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import space.npstr.sqlsauce.DatabaseWrapper;

import javax.annotation.Nullable;

/**
 * Created by napster on 24.02.18.
 * <p>
 * Create our entity repositories
 */
@Configuration
public class RepoConfiguration {

    private final BackendConfig backendConfig;
    private final DatabaseManager databaseManager;
    private final Gson gson = new Gson();
    private final Http http = BotController.HTTP; //todo replace

    public RepoConfiguration(BackendConfig backendConfig, DatabaseManager databaseManager) {
        this.backendConfig = backendConfig;
        this.databaseManager = databaseManager;
    }

    @Bean
    public BlacklistRepo blacklistRepo() {
        String backendUrl = backendConfig.getHost();
        if (backendUrl.isEmpty()) {
            return new SqlSauceBlacklistRepo(databaseManager.getMainDbWrapper());
        } else {
            return new RestBlacklistRepo(backendUrl, http, gson, backendConfig.getBasicAuth());
        }
    }

    @Bean
    public GuildConfigRepo guildConfigRepo() {
        String backendUrl = backendConfig.getHost();
        if (backendUrl.isEmpty()) {
            return new SqlSauceGuildConfigRepo(databaseManager.getMainDbWrapper());
        } else {
            return new RestGuildConfigRepo(backendUrl, http, gson, backendConfig.getBasicAuth());
        }
    }

    @Bean
    public GuildDataRepo guildDataRepo() {
        String backendUrl = backendConfig.getHost();
        if (backendUrl.isEmpty()) {
            return new SqlSauceGuildDataRepo(databaseManager.getMainDbWrapper());
        } else {
            return new RestGuildDataRepo(backendUrl, http, gson, backendConfig.getBasicAuth());
        }
    }

    @Bean
    public GuildModulesRepo guildModulesRepo() {
        String backendUrl = backendConfig.getHost();
        if (backendUrl.isEmpty()) {
            return new SqlSauceGuildModulesRepo(databaseManager.getMainDbWrapper());
        } else {
            return new RestGuildModulesRepo(backendUrl, http, gson, backendConfig.getBasicAuth());
        }
    }

    @Bean
    public GuildPermsRepo guildPermsRepo() {
        String backendUrl = backendConfig.getHost();
        if (backendUrl.isEmpty()) {
            return new SqlSauceGuildPermsRepo(databaseManager.getMainDbWrapper());
        } else {
            return new RestGuildPermsRepo(backendUrl, http, gson, backendConfig.getBasicAuth());
        }
    }

    @Bean
    public PrefixRepo prefixRepo() {
        String backendUrl = backendConfig.getHost();
        if (backendUrl.isEmpty()) {
            return new SqlSaucePrefixRepo(databaseManager.getMainDbWrapper());
        } else {
            return new RestPrefixRepo(backendUrl, http, gson, backendConfig.getBasicAuth());
        }
    }

    @Nullable
    @Bean
    public SearchResultRepo searchResultRepo() {
        String backendUrl = backendConfig.getHost();
        if (backendUrl.isEmpty()) {
            DatabaseWrapper cacheDbWrapper = databaseManager.getCacheDbWrapper();
            return cacheDbWrapper == null ? null : new SqlSauceSearchResultRepo(cacheDbWrapper); //todo noop repo for cache entities?
        } else {
            return new RestSearchResultRepo(backendUrl, http, gson, backendConfig.getBasicAuth());
        }
    }
}
