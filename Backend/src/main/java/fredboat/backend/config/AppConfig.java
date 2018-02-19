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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import space.npstr.sqlsauce.DatabaseWrapper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by napster on 16.02.18.
 *
 * Mirrors the fredboat tree of the application.yaml
 */
@ConfigurationProperties(prefix = "fredboat")
@Configuration
@EnableConfigurationProperties
public class AppConfig {

    @Bean("databaseManager")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public DatabaseManager getDatabaseManager(Db dbConfig) {
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

    private final Db db = new Db();

    @Bean
    public Db getDb() {
        return db;
    }

    public static class Db {

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

    private final Security security = new Security();

    @Bean
    public Security getSecurity() {
        return security;
    }

    public static class Security {

        private List<Admin> admins = new ArrayList<>();

        public List<Admin> getAdmins() {
            return admins;
        }

        public void setAdmins(List<Admin> admins) {
            this.admins = admins;
        }

        public static class Admin {
            private String name = "";
            private String pass = "";

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getPass() {
                return pass;
            }

            public void setPass(String pass) {
                this.pass = pass;
            }
        }
    }
}
