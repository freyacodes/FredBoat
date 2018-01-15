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

import fredboat.db.entity.main.BlacklistEntry;
import fredboat.db.entity.main.GuildConfig;
import fredboat.db.entity.main.GuildModules;
import fredboat.db.entity.main.GuildPermissions;
import fredboat.main.BotController;
import fredboat.util.func.NonnullFunction;
import fredboat.util.func.NonnullSupplier;
import net.dv8tion.jda.core.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.sqlsauce.DatabaseWrapper;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * This class serves two main use cases - it allows wrapping database calls in a convenient and safe way through methods
 * like {@link EntityIO#doUserFriendly(NonnullSupplier)} and {@link EntityIO#onMainDb(NonnullFunction)}, and it is home
 * to commonly used methods to read entities without writing anything back, and writing entities without reading them
 * (in other words, operations not requiring transformations) that wrap these into the aforementioned convenience
 * methods, reducing the boilerplate code at all places which just need to read data from an entity, or just save data
 * into an entity.
 * The amount of functional things seen in here are higher then usual, therefore a look at exising usage is highly
 * recommended if finding oneself stuck at some point trying to figure out how to compose these methods, alternatively
 * you are invited to shout at Napster who wrote this code.
 * <p>
 * <p>
 * How to use our database:
 * Our use cases can be roughly ordered into three categories:
 * 1. Read some data to act on it
 * Example: A track has started, we look up in the GuildConfig whether the guild wishes the bot to announce it
 * 2. Write some data
 * Example: A user has been autoblacklisted, we need to save his BlacklistEntry back to the database (which is otherwise
 * held in memory for performance reasons, so no need to read it first)
 * Another example: The prefix has been set, we need to save the new one to the database.
 * 3. Read some data, and depending on its contents write one thing, or another back, or maybe nothing at all
 * Example: Users setting up our permission system, where we want to give them feedback based on existing set up
 * permissions and maybe run a few checks, before adjusting the entity and saving it.
 * It is a lot better to do these lightweight (!) operations inside of an ongoing persistence context than
 * loading -> detaching -> saving, but managing the persistence context for each even so tiny query makes code both
 * verbose and error prone.
 * <p>
 * Use cases 1 & 2, especially when the entity is used across the bot, warrant a convenience getter method in this class.
 * Example: loading the GuildConfig, loading GuildPermissions
 * Use cases of type 3 usually have some unique logic to them, which is tied to the context they are called from. It is
 * probably better to keep that logic in the place wherever it is needed, and not bring it to this class.
 * <p>
 * If your use case is not covered by the above / Where the heck is the EntityManager / my query is too complicated for
 * this framework / I need to manage the persistence context for my query / I need to set weird JDBC/Hibernate
 * properties on the connection for my query:
 * The EntityManager can be grabbed and used this way:
 * <pre>
 *  doUserFriendly(onMainDb(
 *      wrapper -> {
 *          EntityManager em = wrapper.unwrap().getEntityManager();
 *          try {
 *              //your code here, returning something
 *          } catch (NastySqlExceptions and what not) {
 *              //probably rethrow as DatabaseException to be caught by doUserFriendly()
 *          } finally {
 *              em.close();
 *          }
 *      }
 *  ));
 * </pre>
 * Make sure to have a look at {@link space.npstr.sqlsauce.DatabaseWrapper} for more usage examples of the EntityManager.
 */
public class EntityIO {

    @Nonnull
    private static final Logger log = LoggerFactory.getLogger(EntityIO.class);

    /**
     * Wrap an operation that throws a database exception so that it gets rethrown as one of our user friendly
     * MessagingExceptions. MessagingExceptions or their causes are currently not expected to be logged further up,
     * that's why we log the cause of it at this place.
     */
    @Nonnull
    public static <T> T doUserFriendly(@Nonnull NonnullSupplier<T> operation) {
        try {
            return operation.get();
        } catch (DatabaseException e) {
            log.error("EntityIO database operation failed", e);
            throw new DatabaseNotReadyException(e);
        }
    }

    /**
     * Provide the main database connection to a function describing a database operation, taking a database wrapper and
     * returning a nonnull value of class T.
     *
     * @return a supplier of the nonnull return value of the provided function. The operation will be executed
     * as soon as {@link NonnullSupplier#get()} on the returned value is called.
     */
    @Nonnull
    public static <T> NonnullSupplier<T> onMainDb(@Nonnull NonnullFunction<DatabaseWrapper, T> operation) {
        return () -> operation.apply(BotController.INS.getMainDbWrapper());
    }

    /**
     * Similar to {@link EntityIO#onMainDb(NonnullFunction)}, just for the cache database, with measures to handle
     * the optional nature of it.
     * Will return an empty optional noop if there is no cache db.
     */
    @Nonnull
    public static <T> NonnullSupplier<Optional<T>> onCacheDb(@Nonnull Function<DatabaseWrapper, T> operation) {
        DatabaseWrapper cacheDbWrapper = BotController.INS.getCacheDbWrapper();
        if (cacheDbWrapper == null) {
            //noinspection Convert2MethodRef
            return () -> Optional.empty();
        } else {
            return () -> Optional.of(operation.apply(cacheDbWrapper));
        }
    }


    // Loading of Entities

    @Nonnull
    public static GuildConfig getGuildConfig(@Nonnull Guild guild) {
        return doUserFriendly(onMainDb(wrapper -> wrapper.getOrCreate(GuildConfig.key(guild))));
    }

    @Nonnull
    public static GuildPermissions getGuildPermissions(@Nonnull Guild guild) {
        return doUserFriendly(onMainDb(wrapper -> wrapper.getOrCreate(GuildPermissions.key(guild))));
    }

    @Nonnull
    public static GuildModules getGuildModules(@Nonnull Guild guild) {
        return doUserFriendly(onMainDb(wrapper -> wrapper.getOrCreate(GuildModules.key(guild))));
    }


    // Blacklist stuff

    @Nonnull
    public static List<BlacklistEntry> loadBlacklist() {
        return doUserFriendly(onMainDb(wrapper -> wrapper.loadAll(BlacklistEntry.class)));
    }

    @Nonnull
    public static BlacklistEntry mergeBlacklistEntry(@Nonnull BlacklistEntry ble) {
        return doUserFriendly(onMainDb(wrapper -> wrapper.merge(ble)));
    }

    public static void deleteBlacklistEntry(long id) {
        //language=SQL
        String query = "DELETE FROM blacklist WHERE id = :id";
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);

        doUserFriendly(onMainDb(wrapper -> wrapper.executeSqlQuery(query, params)));
    }
}
