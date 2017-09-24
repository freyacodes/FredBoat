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

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import fredboat.Config;
import fredboat.FredBoat;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.Properties;

public class DatabaseManager {

    public static final String SQLITE_DIALECT = "org.hibernate.dialect.SQLiteDialect";

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    private EntityManagerFactory emf;
    private Session sshTunnel;
    private DatabaseState state = DatabaseState.UNINITIALIZED;

    //local port, if using SSH tunnel point your jdbc to this, e.g. jdbc:postgresql://localhost:9333/...
    private static final int SSH_TUNNEL_PORT = 9333;

    private String jdbcUrl;
    private String dialect;
    private int poolSize;

    /**
     * @param jdbcUrl  connection to the database
     * @param dialect  set to null or empty String to have it auto detected by Hibernate, chosen jdbc driver must support that
     * @param poolSize max size of the connection pool
     */
    public DatabaseManager(String jdbcUrl, String dialect, int poolSize) {
        this.jdbcUrl = jdbcUrl;
        this.dialect = dialect;
        this.poolSize = poolSize;
    }

    /**
     * Starts the database connection.
     *
     * @throws IllegalStateException if trying to start a database that is READY or INITIALIZING
     */
    public synchronized void startup() {
        if (state == DatabaseState.READY || state == DatabaseState.INITIALIZING) {
            throw new IllegalStateException("Can't start the database, when it's current state is " + state);
        }

        state = DatabaseState.INITIALIZING;

        try {
            if (Config.CONFIG.isUseSshTunnel()) {
                //don't connect again if it's already connected
                if (sshTunnel == null || !sshTunnel.isConnected()) {
                    connectSSH();
                }
            }

            //These are now located in the resources directory as XML
            Properties properties = new Properties();
            properties.put("configLocation", "hibernate.cfg.xml");

            properties.put("hibernate.connection.provider_class", "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");
            properties.put("hibernate.connection.url", jdbcUrl);
            if (dialect != null && !"".equals(dialect)) properties.put("hibernate.dialect", dialect);
            properties.put("hibernate.cache.use_second_level_cache", "true");
            properties.put("hibernate.cache.provider_configuration_file_resource_path", "ehcache.xml");
            properties.put("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory");

            //this does a lot of logs
            //properties.put("hibernate.show_sql", "true");

            //automatically update the tables we need
            //caution: only add new columns, don't remove or alter old ones, otherwise manual db table migration needed
            properties.put("hibernate.hbm2ddl.auto", "update");

            //disable autocommit, it is not recommended for our usecases, and interferes with some of them
            // see https://vladmihalcea.com/2017/05/17/why-you-should-always-use-hibernate-connection-provider_disables_autocommit-for-resource-local-jpa-transactions/
            // this also means all EntityManager interactions need to be wrapped into em.getTransaction.begin() and
            // em.getTransaction.commit() to prevent a rollback spam at the database
            properties.put("hibernate.connection.autocommit", "true");
            properties.put("hibernate.connection.provider_disables_autocommit", "false");

            properties.put("hibernate.hikari.maximumPoolSize", Integer.toString(poolSize));

            //how long to wait for a connection becoming available, also the timeout when a DB fails
            if (isSQLiteDB()) {
                properties.put("hibernate.hikari.connectionTimeout", Integer.toString(Config.SQLITE_TIMEOUT_MILLISECONDS));
            } else {
                properties.put("hibernate.hikari.connectionTimeout", Integer.toString(Config.HIKARI_TIMEOUT_MILLISECONDS));
            }
            //this helps with sorting out connections in pgAdmin
            properties.put("hibernate.hikari.dataSource.ApplicationName", "FredBoat_" + Config.CONFIG.getDistribution());

            //timeout the validation query (will be done automatically through Connection.isValid())
            properties.put("hibernate.hikari.validationTimeout", "1000");


            LocalContainerEntityManagerFactoryBean emfb = new LocalContainerEntityManagerFactoryBean();
            if (isSQLiteDB()) {
                emfb.setPackagesToScan("fredboat.db.entity.common", "fredboat.db.entity.sqlite");
            } else {
                emfb.setPackagesToScan("fredboat.db.entity.common", "fredboat.db.entity.postgres");
            }
            emfb.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            emfb.setJpaProperties(properties);
            emfb.setPersistenceUnitName("fredboat.test");
            emfb.setPersistenceProviderClass(HibernatePersistenceProvider.class);
            emfb.afterPropertiesSet();

            //leak prevention, close existing factory if possible
            closeEntityManagerFactory();

            emf = emfb.getObject();

            //adjusting the ehcache config
            if (!Config.CONFIG.isUseSshTunnel()) {
                //local database: turn off overflow to disk of the cache
                for (CacheManager cacheManager : CacheManager.ALL_CACHE_MANAGERS) {
                    for (String cacheName : cacheManager.getCacheNames()) {
                        CacheConfiguration cacheConfig = cacheManager.getCache(cacheName).getCacheConfiguration();
                        cacheConfig.getPersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.NONE);
                    }
                }
            }
            for (CacheManager cacheManager : CacheManager.ALL_CACHE_MANAGERS) {
                log.debug(cacheManager.getActiveConfigurationText());
            }

            log.info("Started Hibernate");
            state = DatabaseState.READY;
        } catch (Exception ex) {
            state = DatabaseState.FAILED;
            throw new RuntimeException("Failed starting database connection", ex);
        }
    }

    public void reconnectSSH() {
        connectSSH();
        //try a test query and if successful set state to ready
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.createNativeQuery("SELECT 1;").getResultList();
            em.getTransaction().commit();
            state = DatabaseState.READY;
        } finally {
            em.close();
        }
    }

    private synchronized void connectSSH() {
        if (!Config.CONFIG.isUseSshTunnel()) {
            log.warn("Cannot connect ssh tunnel as it is not specified in the config");
            return;
        }
        if (sshTunnel != null && sshTunnel.isConnected()) {
            log.info("Tunnel is already connected, disconnect first before reconnecting");
            return;
        }
        try {
            //establish the tunnel
            log.info("Starting SSH tunnel");

            java.util.Properties config = new java.util.Properties();
            JSch jsch = new JSch();
            JSch.setLogger(new JSchLogger());

            //Parse host:port
            String sshHost = Config.CONFIG.getSshHost().split(":")[0];
            int sshPort = Integer.parseInt(Config.CONFIG.getSshHost().split(":")[1]);

            Session session = jsch.getSession(Config.CONFIG.getSshUser(),
                    sshHost,
                    sshPort
            );
            jsch.addIdentity(Config.CONFIG.getSshPrivateKeyFile());
            config.put("StrictHostKeyChecking", "no");
            config.put("ConnectionAttempts", "3");
            session.setConfig(config);
            session.setServerAliveInterval(500);//milliseconds
            session.connect();

            log.info("SSH Connected");

            //forward the port
            int assignedPort = session.setPortForwardingL(
                    SSH_TUNNEL_PORT,
                    "localhost",
                    Config.CONFIG.getForwardToPort()
            );

            sshTunnel = session;

            log.info("localhost:" + assignedPort + " -> " + sshHost + ":" + Config.CONFIG.getForwardToPort());
            log.info("Port Forwarded");
        } catch (Exception e) {
            throw new RuntimeException("Failed to start SSH tunnel", e);
        }
    }

    /**
     * Please call close() on the EntityManager object you receive after you are done to let the pool recycle the
     * connection and save the nature from environmental toxins like open database connections.
     */
    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    /**
     * Performs health checks on the ssh tunnel and database
     *
     * @return true if the database is operational, false if not
     */
    public boolean isAvailable() {
        if (state != DatabaseState.READY) {
            return false;
        }

        //is the ssh connection still alive?
        if (sshTunnel != null && !sshTunnel.isConnected()) {
            log.error("SSH tunnel lost connection.");
            state = DatabaseState.FAILED;
            //immediately try to reconnect the tunnel
            //DBConnectionWatchdogAgent should take further care of this
            FredBoat.executor.submit(this::reconnectSSH);
            return false;
        }

        return state == DatabaseState.READY;
    }

    /**
     * Avoid multiple threads calling a close on the factory by wrapping it into this synchronized method
     */
    private synchronized void closeEntityManagerFactory() {
        if (emf != null && emf.isOpen()) {
            try {
                emf.close();
            } catch (IllegalStateException ignored) {
                //it has already been closed, nothing to catch here
            }
        }
    }

    public DatabaseState getState() {
        return state;
    }

    public enum DatabaseState {
        UNINITIALIZED,
        INITIALIZING,
        FAILED,
        READY,
        SHUTDOWN
    }

    /**
     * Shutdown, close, stop, halt, burn down all resources this object has been using
     */
    public void shutdown() {
        log.info("DatabaseManager shutdown call received, shutting down");
        state = DatabaseState.SHUTDOWN;
        closeEntityManagerFactory();

        if (sshTunnel != null)
            sshTunnel.disconnect();
    }

    public boolean isSQLiteDB() {
        return SQLITE_DIALECT.equalsIgnoreCase(dialect);
    }

    private static class JSchLogger implements com.jcraft.jsch.Logger {

        private static final Logger logger = LoggerFactory.getLogger("JSch");

        @Override
        public boolean isEnabled(int level) {
            return true;
        }

        @Override
        public void log(int level, String message) {
            switch (level) {
                case com.jcraft.jsch.Logger.DEBUG:
                    logger.debug(message);
                    break;
                case com.jcraft.jsch.Logger.INFO:
                    logger.info(message);
                    break;
                case com.jcraft.jsch.Logger.WARN:
                    logger.warn(message);
                    break;
                case com.jcraft.jsch.Logger.ERROR:
                case com.jcraft.jsch.Logger.FATAL:
                    logger.error(message);
                    break;
                default:
                    throw new RuntimeException("Invalid log level");
            }
        }
    }

}
