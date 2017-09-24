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
import javax.persistence.TypedQuery;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EntityReader {

    private static final Logger log = LoggerFactory.getLogger(EntityReader.class);

    /**
     * @param id    id of the entity to get
     * @param clazz class of the entity to get
     * @param <E>   class needs to implement IEntity
     * @return the entity with the requested id of the requested class
     * @throws DatabaseNotReadyException if the database is not available
     */
    public static <E extends IEntity<I>, I extends Serializable> E getOrCreateEntity(I id, Class<E> clazz) throws DatabaseNotReadyException {
        E entity = getEntity(id, clazz);
        //return a fresh object if we didn't find the one we were looking for
        if (entity == null) entity = newInstance(id, clazz);
        return entity;
    }

    //may return null
    public static <E extends IEntity<I>, I extends Serializable> E getEntity(I id, Class<E> clazz) throws DatabaseNotReadyException {
        DatabaseManager dbManager = FredBoat.obtainAvailableDbManager();
        EntityManager em = dbManager.getEntityManager();
        E result;
        try {
            em.getTransaction().begin();
            result = em.find(clazz, id);
            em.getTransaction().commit();
        } catch (PersistenceException e) {
            log.error("Error while trying to find entity of class {} from DB for id {}", clazz.getName(), id, e);
            throw new DatabaseNotReadyException(e);
        } finally {
            em.close();
        }
        return result;
    }

    private static <E extends IEntity<I>, I extends Serializable> E newInstance(I id, Class<E> clazz) {
        try {
            E entity = clazz.newInstance();
            entity.setId(id);
            return entity;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Could not create an entity of class " + clazz.getName(), e);
        }
    }

    //result will be ordered by the order of the id list, but may contain null for unknown entities
    public static <E extends IEntity<I>, I extends Serializable> List<E> getEntities(List<I> ids, Class<E> clazz)
            throws DatabaseNotReadyException {
        EntityManager em = FredBoat.obtainAvailableDbManager().getEntityManager();
        List<E> results = new ArrayList<>();
        try {
            results = em.unwrap(Session.class).byMultipleIds(clazz).withBatchSize(1000).multiLoad(ids);
        } catch (PersistenceException e) {
            log.error("Error while loading entities of class {} from DB", clazz.getName(), e);
        } finally {
            em.close();
        }
        return results;
    }

    /**
     * @param clazz class of the entities to get
     * @param <E>   class needs to implement IEntity
     * @return a list of all elements of the requested class
     * @throws DatabaseNotReadyException if the database is not available
     */
    public static <E extends IEntity> List<E> loadAll(Class<E> clazz) throws DatabaseNotReadyException {
        DatabaseManager dbManager = FredBoat.obtainAvailableDbManager();
        EntityManager em = dbManager.getEntityManager();
        try {
            em.getTransaction().begin();
            List<E> result = em.createQuery("SELECT c FROM " + clazz.getSimpleName() + " c", clazz).getResultList();
            em.getTransaction().commit();
            return result;
        } catch (PersistenceException e) {
            log.error("Error while trying to load the blacklist", e);
            throw new DatabaseNotReadyException(e);
        } finally {
            em.close();
        }
    }

    /**
     * @param queryString the raw JPQL query string
     * @param parameters  parameters to be set on the query
     * @param resultClass expected class of the results of the query
     * @param offset      set to -1 or lower for no offset
     * @param limit       set to -1 or lower for no limit
     * @throws DatabaseNotReadyException if the database is not available
     */
    //limited and offset results
    public static <T> List<T> selectJPQLQuery(String queryString, Map<String, Object> parameters, Class<T> resultClass, int offset, int limit)
            throws DatabaseNotReadyException {
        DatabaseManager dbManager = FredBoat.obtainAvailableDbManager();
        EntityManager em = dbManager.getEntityManager();
        try {
            TypedQuery<T> q = em.createQuery(queryString, resultClass);
            parameters.forEach(q::setParameter);
            if (offset > -1) q.setFirstResult(offset);
            if (limit > -1) q.setMaxResults(limit);
            em.getTransaction().begin();
            List<T> result = q.getResultList();
            em.getTransaction().commit();
            return result;
        } catch (PersistenceException e) {
            log.error("Failed to select JPQL query {}", queryString, e);
            throw new DatabaseNotReadyException(e);
        } finally {
            em.close();
        }
    }

    //limited results
    public static <T> List<T> selectJPQLQuery(String queryString, Map<String, Object> parameters, Class<T> resultClass, int limit) {
        return selectJPQLQuery(queryString, parameters, resultClass, -1, limit);
    }

    //all results
    public static <T> List<T> selectJPQLQuery(String queryString, Map<String, Object> parameters, Class<T> resultClass) {
        return selectJPQLQuery(queryString, parameters, resultClass, -1);
    }
}
