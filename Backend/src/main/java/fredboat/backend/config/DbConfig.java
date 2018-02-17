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

package fredboat.backend.config;

import fredboat.db.DatabaseManager;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.stereotype.Component;
import space.npstr.sqlsauce.DatabaseWrapper;

import javax.annotation.Nullable;

/**
 * Created by napster on 16.02.18.
 */
@ConfigurationProperties(prefix = "fredboat.db")
@Component
public class DbConfig {

    @Bean("databaseManager")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public DatabaseManager getDatabaseManager(DbConfig dbConfig) {
        //todo improve these parameters
        return new DatabaseManager(null, null,
                4, "Backend", true,
                dbConfig.getMain().getJdbcUrl(), null,
                dbConfig.getCache().getJdbcUrl(), null,
                (puName, dataSource, properties, entityPackages) -> {
                    LocalContainerEntityManagerFactoryBean emfb = new LocalContainerEntityManagerFactoryBean();
                    emfb.setDataSource(dataSource);
                    emfb.setPackagesToScan(entityPackages.toArray(new String[entityPackages.size()]));

                    JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
                    emfb.setJpaVendorAdapter(vendorAdapter);
                    emfb.setJpaProperties(properties);

                    emfb.afterPropertiesSet(); //initiate creation of the native emf
                    return emfb.getNativeEntityManagerFactory();
                });
    }

    @Primary
    @Bean("mainDbWrapper")
    public DatabaseWrapper getMainDbWrapper(DatabaseManager databaseManager) {
        return databaseManager.getMainDbWrapper();
    }

    @Nullable
    @Bean("cacheDbWrapper")
    public DatabaseWrapper getCacheDbWrapper(DatabaseManager databaseManager) {
        return databaseManager.getCacheDbWrapper();
    }

    private final Main main = new Main();

    public Main getMain() {
        return main;
    }

    private final Cache cache = new Cache();

    public Cache getCache() {
        return cache;
    }

    public static class Main {
        private String jdbcUrl = "";

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        public void setJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }
    }

    public static class Cache {
        private String jdbcUrl = "";

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        public void setJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }
    }
}
