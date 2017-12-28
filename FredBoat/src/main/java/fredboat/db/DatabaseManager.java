/*
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
 *
 */

package fredboat.db;

import com.zaxxer.hikari.HikariConfig;
import fredboat.Config;
import fredboat.feature.metrics.Metrics;
import fredboat.shared.constant.BotConstants;
import fredboat.util.DiscordUtil;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.sqlsauce.DatabaseConnection;
import space.npstr.sqlsauce.DatabaseException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Properties;

public class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    private static final String MAIN_PERSISTENCE_UNIT_NAME = "fredboat.main";
    private static final String CACHE_PERSISTENCE_UNIT_NAME = "fredboat.cache";

    @Nonnull
    public static DatabaseConnection main() throws DatabaseException {
        String jdbc = Config.CONFIG.getMainJdbcUrl();

        Flyway flyway = new Flyway();
        flyway.setBaselineOnMigrate(true);
        flyway.setBaselineVersion(MigrationVersion.fromVersion("0"));
        flyway.setBaselineDescription("Base Migration");
        flyway.setLocations("classpath:fredboat/db/migrations/main");

        HikariConfig hikariConfig = DatabaseConnection.Builder.getDefaultHikariConfig();
        hikariConfig.setMaximumPoolSize(Config.CONFIG.getHikariPoolSize());

        Properties hibernateProps = DatabaseConnection.Builder.getDefaultHibernateProps();
        hibernateProps.put("hibernate.cache.use_second_level_cache", "true");
        hibernateProps.put("hibernate.cache.use_query_cache", "true");
        hibernateProps.put("net.sf.ehcache.configurationResourceName", "/ehcache_main.xml");
        hibernateProps.put("hibernate.cache.provider_configuration_file_resource_path", "ehcache_main.xml");
        hibernateProps.put("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory");
        //we use flyway db now for migrations, hibernate shall only run validations
        hibernateProps.put("hibernate.hbm2ddl.auto", "validate");

        //dont run migrations or validate the db from the patron bot
        if (DiscordUtil.getBotId() == BotConstants.PATRON_BOT_ID) {
            flyway = null;
            hibernateProps.put("hibernate.hbm2ddl.auto", "none");
        }

        DatabaseConnection databaseConnection = new DatabaseConnection.Builder(MAIN_PERSISTENCE_UNIT_NAME, jdbc)
                .setHikariConfig(hikariConfig)
                .setHibernateProps(hibernateProps)
                .setDialect("org.hibernate.dialect.PostgreSQL95Dialect")
                .addEntityPackage("fredboat.db.entity.main")
                .setAppName("FredBoat_" + Config.CONFIG.getDistribution())
                .setSshDetails(Config.CONFIG.getMainSshTunnelConfig())
                .setHikariStats(Metrics.instance().hikariStats)
                .setHibernateStats(Metrics.instance().hibernateStats)
                .setCheckConnection(false)
                .setFlyway(flyway)
                .build();

        //adjusting the ehcache config
        if (Config.CONFIG.getMainSshTunnelConfig() == null) {
            //local database: turn off overflow to disk of the cache
            CacheManager cacheManager = CacheManager.getCacheManager("MAIN_CACHEMANAGER");
            for (String cacheName : cacheManager.getCacheNames()) {
                CacheConfiguration cacheConfig = cacheManager.getCache(cacheName).getCacheConfiguration();
                cacheConfig.getPersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.NONE);
            }
        }
        log.debug(CacheManager.getCacheManager("MAIN_CACHEMANAGER").getActiveConfigurationText());

        return databaseConnection;
    }


    @Nullable //may return null of no cache db has been configured
    public static DatabaseConnection cache() throws DatabaseException {
        String cacheJdbc = Config.CONFIG.getCacheJdbcUrl();
        if (cacheJdbc == null) {
            return null;
        }

        Flyway flyway = new Flyway();
        flyway.setBaselineOnMigrate(true);
        flyway.setBaselineVersion(MigrationVersion.fromVersion("0"));
        flyway.setBaselineDescription("Base Migration");
        flyway.setLocations("classpath:fredboat/db/migrations/cache");

        HikariConfig hikariConfig = DatabaseConnection.Builder.getDefaultHikariConfig();
        hikariConfig.setMaximumPoolSize(Config.CONFIG.getHikariPoolSize());

        Properties hibernateProps = DatabaseConnection.Builder.getDefaultHibernateProps();
        hibernateProps.put("hibernate.cache.use_second_level_cache", "true");
        hibernateProps.put("hibernate.cache.use_query_cache", "true");
        hibernateProps.put("net.sf.ehcache.configurationResourceName", "/ehcache_cache.xml");
        hibernateProps.put("hibernate.cache.provider_configuration_file_resource_path", "ehcache_cache.xml");
        hibernateProps.put("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory");
        //we use flyway db now for migrations, hibernate shall only run validations
        hibernateProps.put("hibernate.hbm2ddl.auto", "validate");

        //dont run migrations or validate the db from the patron bot
        if (DiscordUtil.getBotId() == BotConstants.PATRON_BOT_ID) {
            flyway = null;
            hibernateProps.put("hibernate.hbm2ddl.auto", "none");
        }

        DatabaseConnection databaseConnection = new DatabaseConnection.Builder(CACHE_PERSISTENCE_UNIT_NAME, cacheJdbc)
                .setHikariConfig(hikariConfig)
                .setHibernateProps(hibernateProps)
                .setDialect("org.hibernate.dialect.PostgreSQL95Dialect")
                .addEntityPackage("fredboat.db.entity.cache")
                .setAppName("FredBoat_" + Config.CONFIG.getDistribution())
                .setSshDetails(Config.CONFIG.getCacheSshTunnelConfig())
                .setHikariStats(Metrics.instance().hikariStats)
                .setHibernateStats(Metrics.instance().hibernateStats)
                .setFlyway(flyway)
                .build();

        //adjusting the ehcache config
        if (Config.CONFIG.getMainSshTunnelConfig() == null) {
            //local database: turn off overflow to disk of the cache
            CacheManager cacheManager = CacheManager.getCacheManager("CACHE_CACHEMANAGER");
            for (String cacheName : cacheManager.getCacheNames()) {
                CacheConfiguration cacheConfig = cacheManager.getCache(cacheName).getCacheConfiguration();
                cacheConfig.getPersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.NONE);
            }
        }
        log.debug(CacheManager.getCacheManager("CACHE_CACHEMANAGER").getActiveConfigurationText());

        return databaseConnection;
    }
}
