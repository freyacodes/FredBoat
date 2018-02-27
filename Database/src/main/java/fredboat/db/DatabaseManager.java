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

package fredboat.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory;
import io.prometheus.client.hibernate.HibernateStatisticsCollector;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.sqlsauce.DatabaseConnection;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.sqlsauce.DatabaseWrapper;
import space.npstr.sqlsauce.ssh.SshTunnel;

import javax.annotation.Nullable;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    private static final String MAIN_PERSISTENCE_UNIT_NAME = "fredboat.main";
    private static final String CACHE_PERSISTENCE_UNIT_NAME = "fredboat.cache";

    @Nullable
    private final HibernateStatisticsCollector hibernateStats;
    @Nullable
    private final PrometheusMetricsTrackerFactory hikariStats;
    private final int poolsize;
    private final String appName;
    private final boolean migrateAndValidate;
    private final String mainJdbc;
    @Nullable
    private final SshTunnel.SshDetails mainTunnel;
    @Nullable
    private final String cacheJdbc;
    @Nullable
    private final SshTunnel.SshDetails cacheTunnel;
    @Nullable
    private final DatabaseConnection.EntityManagerFactoryBuilder entityManagerFactoryBuilder;

    @Nullable
    private DatabaseConnection mainDbConn;
    private final Object mainDbConnInitLock = new Object();
    private boolean mainConnBuilt = false;
    @Nullable
    private volatile DatabaseWrapper mainDbWrapper;
    private final Object mainDbWrapperInitLock = new Object();

    @Nullable
    private DatabaseConnection cacheDbConn;
    private final Object cacheDbConnInitLock = new Object();
    private boolean cacheConnBuilt = false;
    @Nullable
    private volatile DatabaseWrapper cacheDbWrapper;
    private final Object cacheDbWrapperInitLock = new Object();

    public DatabaseManager(@Nullable HibernateStatisticsCollector hibernateStats,
                           @Nullable PrometheusMetricsTrackerFactory hikariStats,
                           int poolsize,
                           String appName,
                           boolean migrateAndValidate,
                           String mainJdbc,
                           @Nullable SshTunnel.SshDetails mainTunnel,
                           @Nullable String cacheJdbc,
                           @Nullable SshTunnel.SshDetails cacheTunnel,
                           @Nullable DatabaseConnection.EntityManagerFactoryBuilder entityManagerFactoryBuilder) {
        this.hibernateStats = hibernateStats;
        this.hikariStats = hikariStats;
        this.poolsize = poolsize;
        this.appName = appName;
        this.migrateAndValidate = migrateAndValidate;
        this.mainJdbc = mainJdbc;
        this.mainTunnel = mainTunnel;
        this.cacheJdbc = cacheJdbc;
        this.cacheTunnel = cacheTunnel;
        this.entityManagerFactoryBuilder = entityManagerFactoryBuilder;

        if (mainJdbc.isEmpty()) {
            log.error("Main jdbc url is empty - database creation will fail");
        }
        if (cacheJdbc != null && cacheJdbc.isEmpty()) {
            log.error("Cache jdbc url is empty - database creation will fail");
        }
    }

    public DatabaseConnection getMainDbConn() {
        DatabaseConnection singleton = mainDbConn;
        if (singleton == null) {
            synchronized (mainDbConnInitLock) {
                singleton = mainDbConn;
                if (singleton == null) {
                    mainDbConn = singleton = initMainDbConn();
                    mainConnBuilt = true;
                }
            }
        }
        return singleton;
    }

    public DatabaseWrapper getMainDbWrapper() {
        DatabaseWrapper singleton = mainDbWrapper;
        if (singleton == null) {
            synchronized (mainDbWrapperInitLock) {
                singleton = mainDbWrapper;
                if (singleton == null) {
                    mainDbWrapper = singleton = new DatabaseWrapper(getMainDbConn());
                }
            }
        }
        return singleton;
    }


    @Nullable //may return null if no cache db is configured
    public DatabaseConnection getCacheDbConn() {
        if (cacheJdbc == null || cacheJdbc.isEmpty()) {
            return null;
        }
        DatabaseConnection singleton = cacheDbConn;
        if (singleton == null) {
            synchronized (cacheDbConnInitLock) {
                singleton = cacheDbConn;
                if (singleton == null) {
                    cacheDbConn = singleton = initCacheConn(cacheJdbc);
                    cacheConnBuilt = true;
                }
            }
        }
        return singleton;
    }

    @Nullable //may return null if no cache db is configured
    public DatabaseWrapper getCacheDbWrapper() {
        if (cacheJdbc == null) {
            return null;
        }
        DatabaseWrapper singleton = cacheDbWrapper;
        if (singleton == null) {
            synchronized (cacheDbWrapperInitLock) {
                singleton = cacheDbWrapper;
                if (singleton == null) {
                    DatabaseConnection cacheDbConn = getCacheDbConn();
                    if (cacheDbConn != null) {
                        cacheDbWrapper = singleton = new DatabaseWrapper(cacheDbConn);
                    }
                }
            }
        }
        return singleton;
    }

    public boolean isMainConnBuilt() {
        return mainConnBuilt;
    }

    public boolean isCacheConnBuilt() {
        return cacheConnBuilt;
    }

    private DatabaseConnection initMainDbConn() throws DatabaseException {

        Flyway flyway = null;
        if (migrateAndValidate) {
            flyway = buildFlyway("classpath:fredboat/db/migrations/main");
        }

        DatabaseConnection databaseConnection = getBasicConnectionBuilder(MAIN_PERSISTENCE_UNIT_NAME, mainJdbc)
                .setHibernateProps(buildHibernateProps("ehcache_main.xml"))
                .addEntityPackage("fredboat.db.entity.main")
                .setSshDetails(mainTunnel)
                .setFlyway(flyway)
                .build();

        //adjusting the ehcache config
        if (mainTunnel == null && cacheTunnel == null) {
            //local database: turn off overflow to disk of the cache
            turnOffLocalStorageForEhcacheManager("MAIN_CACHEMANAGER");
        }
        log.debug(CacheManager.getCacheManager("MAIN_CACHEMANAGER").getActiveConfigurationText());

        return databaseConnection;
    }


    public DatabaseConnection initCacheConn(String jdbc) throws DatabaseException {

        Flyway flyway = null;
        if (migrateAndValidate) {
            flyway = buildFlyway("classpath:fredboat/db/migrations/cache");
        }

        DatabaseConnection databaseConnection = getBasicConnectionBuilder(CACHE_PERSISTENCE_UNIT_NAME, jdbc)
                .setHibernateProps(buildHibernateProps("ehcache_cache.xml"))
                .addEntityPackage("fredboat.db.entity.cache")
                .setSshDetails(cacheTunnel)
                .setFlyway(flyway)
                .build();

        //adjusting the ehcache config
        if (mainTunnel == null && cacheTunnel == null) {
            //local database: turn off overflow to disk of the cache
            turnOffLocalStorageForEhcacheManager("CACHE_CACHEMANAGER");
        }
        log.debug(CacheManager.getCacheManager("CACHE_CACHEMANAGER").getActiveConfigurationText());

        return databaseConnection;
    }

    private DatabaseConnection.Builder getBasicConnectionBuilder(String connectionName, String jdbcUrl) {
        DatabaseConnection.Builder builder = new DatabaseConnection.Builder(connectionName, jdbcUrl)
                .setHikariConfig(buildHikariConfig())
                .setDialect("org.hibernate.dialect.PostgreSQL95Dialect")
                .setAppName("FredBoat_" + appName)
                .setHikariStats(hikariStats)
                .setHibernateStats(hibernateStats)
                .setProxyDataSourceBuilder(new ProxyDataSourceBuilder()
                        .logSlowQueryBySlf4j(10, TimeUnit.SECONDS, SLF4JLogLevel.WARN, "SlowQueryLog")
                        .multiline()
                );
        if (entityManagerFactoryBuilder != null) {
            builder = builder.setEntityManagerFactoryBuilder(entityManagerFactoryBuilder);
        }
        return builder;
    }

    private Flyway buildFlyway(String locations) {
        Flyway flyway = new Flyway();
        flyway.setBaselineOnMigrate(true);
        flyway.setBaselineVersion(MigrationVersion.fromVersion("0"));
        flyway.setBaselineDescription("Base Migration");
        flyway.setLocations(locations);

        return flyway;
    }

    private HikariConfig buildHikariConfig() {
        HikariConfig hikariConfig = DatabaseConnection.Builder.getDefaultHikariConfig();
        hikariConfig.setMaximumPoolSize(poolsize);
        return hikariConfig;
    }

    private Properties buildHibernateProps(String ehcacheXmlFile) {
        Properties hibernateProps = DatabaseConnection.Builder.getDefaultHibernateProps();
        hibernateProps.put("hibernate.cache.use_second_level_cache", "true");
        hibernateProps.put("hibernate.cache.use_query_cache", "true");
        hibernateProps.put("net.sf.ehcache.configurationResourceName", ehcacheXmlFile);
        hibernateProps.put("hibernate.cache.provider_configuration_file_resource_path", ehcacheXmlFile);
        hibernateProps.put("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory");
        //hide some exception spam on start, as postgres does not support CLOBs
        // https://stackoverflow.com/questions/43905119/postgres-error-method-org-postgresql-jdbc-pgconnection-createclob-is-not-imple
        hibernateProps.put("hibernate.jdbc.lob.non_contextual_creation", "true");

        if (migrateAndValidate) {
            hibernateProps.put("hibernate.hbm2ddl.auto", "validate");
        } else {
            hibernateProps.put("hibernate.hbm2ddl.auto", "none");
        }

        return hibernateProps;
    }

    private void turnOffLocalStorageForEhcacheManager(String cacheManagerName) {
        CacheManager cacheManager = CacheManager.getCacheManager(cacheManagerName);
        for (String cacheName : cacheManager.getCacheNames()) {
            CacheConfiguration cacheConfig = cacheManager.getCache(cacheName).getCacheConfiguration();
            cacheConfig.getPersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.NONE);
        }
    }
}
