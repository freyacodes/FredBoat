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

package fredboat.backend;

import fredboat.db.repositories.api.*;
import fredboat.db.repositories.impl.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import space.npstr.sqlsauce.DatabaseWrapper;

/**
 * Created by napster on 16.02.18.
 */
@SpringBootApplication
@EnableAutoConfiguration(exclude = { //we handle these ourselves via the DatabaseManager
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        FlywayAutoConfiguration.class
})
public class Application {

    public static final String API_VERSION = "v1";

    public static void main(String[] args) {
        System.setProperty("spring.config.name", "backend");
        SpringApplication.run(Application.class, args);
    }


    //main db repos
    @Bean
    @Primary
    public GuildConfigRepo guildConfigRepo(DatabaseWrapper wrapper) {
        return new SqlSauceGuildConfigRepo(wrapper);
    }

    @Bean
    @Primary
    public BlacklistRepo blacklistRepo(DatabaseWrapper wrapper) {
        return new SqlSauceBlacklistRepo(wrapper);
    }

    @Bean
    @Primary
    public GuildDataRepo guildDataRepo(DatabaseWrapper wrapper) {
        return new SqlSauceGuildDataRepo(wrapper);
    }

    @Bean
    @Primary
    public GuildModulesRepo guildModulesRepo(DatabaseWrapper wrapper) {
        return new SqlSauceGuildModulesRepo(wrapper);
    }

    @Bean
    @Primary
    public GuildPermsRepo guildPermsRepo(DatabaseWrapper wrapper) {
        return new SqlSauceGuildPermsRepo(wrapper);
    }

    @Bean
    @Primary
    public PrefixRepo prefixRepo(DatabaseWrapper wrapper) {
        return new SqlSaucePrefixRepo(wrapper);
    }


    //cache db repos
    @Bean
    @Primary
    public SearchResultRepo searchResultRepo(@Qualifier("cacheDbWrapper") DatabaseWrapper wrapper) {
        return new SqlSauceSearchResultRepo(wrapper);
    }
}
