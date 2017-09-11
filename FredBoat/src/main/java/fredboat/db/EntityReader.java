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
import fredboat.db.entity.BlacklistEntry;
import fredboat.db.entity.GuildConfig;
import fredboat.db.entity.GuildPermissions;
import fredboat.db.entity.IEntity;
import fredboat.db.entity.UConfig;
import net.dv8tion.jda.core.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import java.util.List;

public class EntityReader {

    private static final Logger log = LoggerFactory.getLogger(EntityReader.class);

    public static UConfig getUConfig(String id) {
        return getEntity(id, UConfig.class);
    }

    public static GuildConfig getGuildConfig(String id) {
        return getEntity(id, GuildConfig.class);
    }

    public static GuildPermissions getGuildPermissions(Guild guild) {
        return getEntity(guild.getId(), GuildPermissions.class);
    }

    private static <E extends IEntity> E getEntity(String id, Class<E> clazz) throws DatabaseNotReadyException {
        DatabaseManager dbManager = FredBoat.getDbManager();
        if (!dbManager.isAvailable()) {
            throw new DatabaseNotReadyException();
        }

        EntityManager em = dbManager.getEntityManager();
        E config = null;
        try {
            em.getTransaction().begin();
            config = em.find(clazz, id);
            em.getTransaction().commit();
        } catch (PersistenceException e) {
            log.error("Error while trying to find entity of class {} from DB for id {}", clazz.getName(), id, e);
            throw new DatabaseNotReadyException(e);
        } finally {
            em.close();
        }
        //return a fresh object if we didn't find the one we were looking for
        if (config == null) config = newInstance(id, clazz);
        return config;
    }

    private static <E extends IEntity> E newInstance(String id, Class<E> clazz) {
        try {
            E entity = clazz.newInstance();
            entity.setId(id);
            return entity;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Could not create an entity of class " + clazz.getName(), e);
        }
    }

    public static List<BlacklistEntry> loadBlacklist() {
        DatabaseManager dbManager = FredBoat.getDbManager();
        if (!dbManager.isAvailable()) {
            throw new DatabaseNotReadyException("The database is not available currently. Please try again later.");
        }
        EntityManager em = dbManager.getEntityManager();
        List<BlacklistEntry> result;
        try {
            em.getTransaction().begin();
            result = em.createQuery("SELECT b FROM BlacklistEntry b", BlacklistEntry.class).getResultList();
            em.getTransaction().commit();
        } finally {
            em.close();
        }
        return result;
    }
}
