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
import org.hibernate.exception.JDBCConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;

public class EntityWriter {

    private static final Logger log = LoggerFactory.getLogger(EntityWriter.class);

    /**
     * @param entity entity to be merged
     * @param <E>    entity needs to implement IEntity
     * @return the merged entity
     */
    public static <E extends IEntity> E merge(E entity) {
        DatabaseManager dbManager = FredBoat.getDbManager();
        if (!dbManager.isAvailable()) {
            throw new DatabaseNotReadyException();
        }

        EntityManager em = dbManager.getEntityManager();
        try {
            em.getTransaction().begin();
            E mergedEntity = em.merge(entity);
            em.getTransaction().commit();
            return mergedEntity;
        } catch (JDBCConnectionException e) {
            log.error("Failed to merge entity {}", entity, e);
            throw new DatabaseNotReadyException(e);
        } finally {
            em.close();
        }
    }

    /**
     * @param id    primary key of the entity to be deleted
     * @param clazz class of the entity to be deleted
     * @param <E>   entity needs to implement IEntity
     */
    public static <E extends IEntity> void delete(long id, Class<E> clazz) {
        DatabaseManager dbManager = FredBoat.getDbManager();
        if (!dbManager.isAvailable()) {
            throw new DatabaseNotReadyException("The database is not available currently. Please try again later.");
        }

        EntityManager em = dbManager.getEntityManager();
        try {
            E entity = em.find(clazz, id);

            if (entity != null) {
                em.getTransaction().begin();
                em.remove(entity);
                em.getTransaction().commit();
            }
        } finally {
            em.close();
        }
    }
}
