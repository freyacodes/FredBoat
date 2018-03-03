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

import com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory;
import fredboat.agent.DBConnectionWatchdogAgent;
import fredboat.agent.FredBoatAgent;
import fredboat.config.property.DatabaseConfig;
import fredboat.config.property.ConfigPropertiesProvider;
import fredboat.db.DatabaseManager;
import fredboat.main.ShutdownHandler;
import fredboat.shared.constant.BotConstants;
import fredboat.shared.constant.ExitCodes;
import fredboat.util.DiscordUtil;
import io.prometheus.client.hibernate.HibernateStatisticsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import space.npstr.sqlsauce.DatabaseConnection;
import space.npstr.sqlsauce.DatabaseWrapper;

import javax.annotation.Nullable;

/**
 * Created by napster on 23.02.18.
 * <p>
 * Provides database related beans
 */
@Configuration
public class DatabaseConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfiguration.class);

    @Primary
    @Bean
    public DatabaseWrapper mainDbWrapper(DatabaseConnection mainDbConn) {
        return new DatabaseWrapper(mainDbConn);
    }

    @Bean
    @Nullable
    public DatabaseWrapper cacheDbWrapper(@Nullable @Qualifier("cacheDbConn") DatabaseConnection cacheDbConn) {
        return cacheDbConn == null ? null : new DatabaseWrapper(cacheDbConn);
    }

    @Primary
    @Bean
    public DatabaseConnection mainDbConn(DatabaseManager databaseManager, ShutdownHandler shutdownHandler)
            throws InterruptedException {
        //attempt to connect to the database a few times
        // this is relevant in a dockerized environment because after a reboot there is no guarantee that the db
        // container will be started before the fredboat one
        int dbConnectionAttempts = 0;
        DatabaseConnection mainDbConn = null;
        while ((mainDbConn == null || !mainDbConn.isAvailable()) && dbConnectionAttempts++ < 10) {
            try {
                if (mainDbConn != null) {
                    mainDbConn.shutdown();
                }
                mainDbConn = databaseManager.getMainDbConn();
            } catch (Exception e) {
                log.info("Could not connect to the database. Retrying in a moment...", e);
                Thread.sleep(6000);
            }
        }
        if (mainDbConn == null || !mainDbConn.isAvailable()) {
            String message = "Could not establish database connection. Exiting...";
            log.error(message);
            shutdownHandler.shutdown(ExitCodes.EXIT_CODE_ERROR); //a "hard" shutdown is kinda necessary in docker environments
            throw new RuntimeException(message);
        }

        FredBoatAgent.start(new DBConnectionWatchdogAgent(mainDbConn));
        return mainDbConn;
    }

    @Bean
    @Nullable
    public DatabaseConnection cacheDbConn(DatabaseManager databaseManager, ShutdownHandler shutdownHandler) {
        try {
            return databaseManager.getCacheDbConn();
        } catch (Exception e) {
            String message = "Exception when connecting to cache db";
            log.error(message, e);
            shutdownHandler.shutdown(ExitCodes.EXIT_CODE_ERROR); //a "hard" shutdown is kinda necessary in docker environments
            throw new RuntimeException(message);
        }
    }

    @Bean
    public DatabaseManager databaseManager(ConfigPropertiesProvider configProvider, HibernateStatisticsCollector hibernateStats,
                                           PrometheusMetricsTrackerFactory hikariStats) {
        //run migrations except when its the patron boat
        boolean migrateAndValidate = DiscordUtil.getBotId(configProvider.getCredentials()) != BotConstants.PATRON_BOT_ID;

        DatabaseConfig dbConf = configProvider.getDatabaseConfig();

        DatabaseManager databaseManager = new DatabaseManager(hibernateStats, hikariStats,
                dbConf.getHikariPoolSize(), configProvider.getAppConfig().getDistribution().name(), migrateAndValidate,
                dbConf.getMainJdbcUrl(), dbConf.getMainSshTunnelConfig(),
                dbConf.getCacheJdbcUrl(), dbConf.getCacheSshTunnelConfig(),
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

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (databaseManager.isCacheConnBuilt()) {
                DatabaseConnection cacheDbConn = databaseManager.getCacheDbConn();
                if (cacheDbConn != null) {
                    cacheDbConn.shutdown();
                }
            }
            if (databaseManager.isMainConnBuilt()) {
                databaseManager.getMainDbConn().shutdown();
            }
        }, "databasemanager-shutdown-hook"));

        return databaseManager;
    }
}
