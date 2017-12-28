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
import fredboat.db.entity.main.BlacklistEntry;
import fredboat.db.entity.main.GuildConfig;
import fredboat.db.entity.main.GuildPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.sqlsauce.DatabaseException;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

public class EntityWriter {

    private static final Logger log = LoggerFactory.getLogger(EntityWriter.class);

    public static void mergeGuildConfig(GuildConfig config) {
        merge(config);
    }

    public static void mergeBlacklistEntry(BlacklistEntry ble) {
        merge(ble);
    }

    public static void mergeGuildPermissions(GuildPermissions guildPermissions) {
        merge(guildPermissions);
    }

    private static void merge(IEntity entity) {
        EntityManager em = null;
        try {
            em = FredBoat.getMainDbConnection().getEntityManager();
            em.getTransaction().begin();
            em.merge(entity);
            em.getTransaction().commit();
        } catch (PersistenceException | DatabaseException e) {
            log.error("Failed to merge entity {}", entity, e);
            throw new DatabaseNotReadyException(e);
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public static void deleteBlacklistEntry(long id) {
        EntityManager em = null;
        try {
            em = FredBoat.getMainDbConnection().getEntityManager();
            em.getTransaction().begin();
            BlacklistEntry ble = em.find(BlacklistEntry.class, id);
            em.getTransaction().commit();

            if (ble != null) {
                em.getTransaction().begin();
                em.remove(ble);
                em.getTransaction().commit();
            }
        } catch (DatabaseException | PersistenceException e) {
            log.error("Db blew up while deleting black list entry for id {}", id, e);
            throw new DatabaseNotReadyException("The database is not available currently. Please try again later.");
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }
}
