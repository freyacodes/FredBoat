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

import fredboat.FredBoat;
import fredboat.db.entity.IEntity;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.util.Collection;
import java.util.Map;

public class EntityWriter {

    private static final Logger log = LoggerFactory.getLogger(EntityWriter.class);

    /**
     * @param entity entity to be merged
     * @param <E>    entity needs to implement IEntity
     * @return the merged entity
     */
    public static <E extends IEntity> E merge(E entity) throws DatabaseNotReadyException {
        DatabaseManager dbManager = FredBoat.obtainAvailableDbManager();
        EntityManager em = dbManager.getEntityManager();
        try {
            em.getTransaction().begin();
            E mergedEntity = em.merge(entity);
            em.getTransaction().commit();
            return mergedEntity;
        } catch (PersistenceException e) {
            log.error("Failed to merge entity {} of class {}", entity.getId(), entity.getClass().getSimpleName(), e);
            throw new DatabaseNotReadyException(e);
        } finally {
            em.close();
        }
    }


    /**
     * @param primaryKey key of the object to the deleted
     * @param clazz      class of the object to be deleted
     * @param <T>        class of the object to be deleted
     * @return true if such an object existed in the database, false if not
     */
    public static <T> boolean deleteObject(Object primaryKey, Class<T> clazz) throws DatabaseNotReadyException {
        DatabaseManager dbManager = FredBoat.obtainAvailableDbManager();

        EntityManager em = dbManager.getEntityManager();
        try {
            em.getTransaction().begin();
            T object = em.find(clazz, primaryKey);
            em.getTransaction().commit();
            if (object != null) {
                em.getTransaction().begin();
                em.remove(object);
                em.getTransaction().commit();
                return true;
            }
            return false;
        } catch (PersistenceException e) {
            log.error("Failed to delete object with key {} of class {}", primaryKey.toString(), clazz.getSimpleName(), e);
            throw new DatabaseNotReadyException(e);
        } finally {
            em.close();
        }
    }

    public static <T> void deleteAll(Collection<Long> primaryKeys, Class<T> clazz) throws DatabaseNotReadyException {
        DatabaseManager dbManager = FredBoat.obtainAvailableDbManager();
        EntityManager em = dbManager.getEntityManager();
        try {
            em.getTransaction().begin();
            for (Object key : primaryKeys) {
                T object = em.find(clazz, key);
                if (object != null) {
                    em.remove(object);
                }
            }
            em.getTransaction().commit();
        } catch (PersistenceException e) {
            log.error("Failed to delete {} objects of class {}", primaryKeys.size(), clazz.getSimpleName(), e);
            throw new DatabaseNotReadyException(e);
        } finally {
            em.close();
        }
    }

    public static <E extends IEntity> boolean deleteEntity(E entity) throws DatabaseNotReadyException {
        return deleteObject(entity.getId(), entity.getClass());
    }

    public static void mergeAll(Collection<? extends IEntity> entities) throws DatabaseNotReadyException {
        DatabaseManager dbManager = FredBoat.obtainAvailableDbManager();
        EntityManager em = dbManager.getEntityManager();
        try {
            em.getTransaction().begin();
            for (IEntity entity : entities) {
                em.merge(entity);
            }
            em.getTransaction().commit();
        } catch (PersistenceException e) {
            log.error("Failed to merge entities", e);
            throw new DatabaseNotReadyException(e);
        } finally {
            em.close();
        }
    }

    public static void persistAll(Collection<? extends IEntity> entities) throws DatabaseNotReadyException {
        DatabaseManager dbManager = FredBoat.obtainAvailableDbManager();
        EntityManager em = dbManager.getEntityManager();
        em.unwrap(Session.class).setJdbcBatchSize(1000); //secret sauce to make it fast af over network
        try {
            em.getTransaction().begin();
            for (IEntity entity : entities) {
                em.persist(entity);
            }
            em.getTransaction().commit();
        } catch (PersistenceException e) {
            log.error("Failed to merge entities", e);
            throw new DatabaseNotReadyException(e);
        } finally {
            em.close();
        }
    }

    public static int executeJPQLQuery(String queryString, Map<String, Object> parameters) throws
            DatabaseNotReadyException {
        DatabaseManager dbManager = FredBoat.obtainAvailableDbManager();
        EntityManager em = dbManager.getEntityManager();
        try {
            Query query = em.createQuery(queryString);
            parameters.forEach(query::setParameter);
            em.getTransaction().begin();
            int updatedOrDeleted = query.executeUpdate();
            em.getTransaction().commit();
            return updatedOrDeleted;
        } catch (PersistenceException e) {
            log.error("Failed to execute JPQL query {}", queryString, e);
            throw new DatabaseNotReadyException(e);
        } finally {
            em.close();
        }
    }
}
