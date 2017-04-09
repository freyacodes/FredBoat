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

import fredboat.Config;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.Properties;

public class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);
    
    private static EntityManagerFactory emf;
    public static DatabaseState state = DatabaseState.UNINITIALIZED;

    public static void startup(String jdbcUrl) {
        state = DatabaseState.INITIALIZING;

        try {

            //These are now located in the resources directory as XML
            Properties properties = new Properties();
            properties.put("configLocation", "hibernate.cfg.xml");

            properties.put("hibernate.connection.provider_class", "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");
            properties.put("hibernate.connection.url", jdbcUrl);
            properties.put("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory");

            properties.put("hibernate.show_sql", "true");

            //automatically create the tables we need
            properties.put("hibernate.hbm2ddl.auto", "update");

            properties.put("hibernate.hikari.maximumPoolSize", Integer.toString(Config.CONFIG.getHikariPoolSize()));
            properties.put("hibernate.hikari.idleTimeout", Integer.toString(Config.HIKARI_TIMEOUT_MILLISECONDS));


            LocalContainerEntityManagerFactoryBean emfb = new LocalContainerEntityManagerFactoryBean();
            emfb.setPackagesToScan("fredboat.db.entity");
            emfb.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            emfb.setJpaProperties(properties);
            emfb.setPersistenceUnitName("fredboat.test");
            emfb.setPersistenceProviderClass(HibernatePersistenceProvider.class);
            emfb.afterPropertiesSet();
            emf = emfb.getObject();

            log.info("Started Hibernate");
            state = DatabaseState.READY;
        } catch (Exception ex) {
            state = DatabaseState.FAILED;
            throw new RuntimeException("Failed starting database connection", ex);
        }
    }

    /**
     * Please call close() on the em you receive after you are done to let the pool recycle the connection and save the
     * nature from environmental toxins like open database connections.
     */
    public static EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    static boolean isDisabled() {
        return state == DatabaseState.DISABLED || state == DatabaseState.FAILED;
    }

    public enum DatabaseState {
        DISABLED, //When no JDBC URL is given
        UNINITIALIZED,
        INITIALIZING,
        FAILED,
        READY
    }

}