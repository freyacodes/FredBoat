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

import fredboat.db.api.*;
import fredboat.db.entity.cache.SearchResult;
import fredboat.db.entity.main.*;
import fredboat.db.repositories.api.*;
import fredboat.db.repositories.impl.*;
import fredboat.util.DiscordUtil;
import fredboat.util.func.NonnullSupplier;
import net.dv8tion.jda.core.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.annotations.FieldsAreNonNullByDefault;
import space.npstr.annotations.ParametersAreNonnullByDefault;
import space.npstr.annotations.ReturnTypesAreNonNullByDefault;
import space.npstr.sqlsauce.DatabaseConnection;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.sqlsauce.DatabaseWrapper;
import space.npstr.sqlsauce.entities.GuildBotComposite;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * This class serves as the glue between FredBoat and repositories of entities. It is home
 * to commonly used methods to read and write entities, as well as transform them.
 */
@SuppressWarnings("UnusedReturnValue")
@FieldsAreNonNullByDefault
@ParametersAreNonnullByDefault
@ReturnTypesAreNonNullByDefault
public class EntityIO implements IBlacklistIO, IGuildConfigIO, IGuildDataIO, IGuildModulesIO, IGuildPermsIO, IPrefixIO,
        ISearchResultIO {

    private static final Logger log = LoggerFactory.getLogger(EntityIO.class);

    private final IGuildConfigRepo guildConfigRepo;
    private final IGuildDataRepo guildDataRepo;
    private final IGuildModulesRepo guildModulesRepo;
    private final IGuildPermsRepo guildPermsRepo;
    private final IPrefixRepo prefixRepo;
    private final IBlacklistRepo blacklistRepo;

    @Nullable
    private final ISearchResultRepo searchResultRepo;

    public EntityIO(DatabaseConnection main, @Nullable DatabaseConnection cache) {
        DatabaseWrapper mainWrapper = new DatabaseWrapper(main);
        DatabaseWrapper cacheWrapper = cache != null ? new DatabaseWrapper(cache) : null;

        guildConfigRepo = new SqlSauceGuildConfigRepo(mainWrapper);
        guildDataRepo = new SqlSauceGuildDataRepo(mainWrapper);
        guildModulesRepo = new SqlSauceGuildModulesRepo(mainWrapper);
        guildPermsRepo = new SqlSauceGuildPermsRepo(mainWrapper);
        prefixRepo = new SqlSaucePrefixRepo(mainWrapper);
        blacklistRepo = new SqlSauceBlacklistRepo(mainWrapper);

        if (cacheWrapper != null) {
            searchResultRepo = new SqlSauceSearchResultRepo(cacheWrapper);
        } else {
            searchResultRepo = null; //todo noop repo for cache entities?
        }
    }

    /**
     * Wrap an operation that throws a database exception so that it gets rethrown as one of our user friendly
     * MessagingExceptions. MessagingExceptions or their causes are currently not expected to be logged further up,
     * that's why we log the cause of it at this place.
     */
    private static <T> T fetchUserFriendly(NonnullSupplier<T> operation) {
        try {
            return operation.get();
        } catch (DatabaseException e) {
            log.error("EntityIO database operation failed", e);
            throw new DatabaseNotReadyException(e);
        }
    }

    /**
     * Same as {@link EntityIO#fetchUserFriendly(NonnullSupplier)}, just with a nullable return.
     */
    @Nullable
    private static <T> T getUserFriendly(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (DatabaseException e) {
            log.error("EntityIO database operation failed", e);
            throw new DatabaseNotReadyException(e);
        }
    }

    /**
     * Same as {@link EntityIO#fetchUserFriendly(NonnullSupplier)}, just without returning anything
     */
    private static void doUserFriendly(Runnable operation) {
        try {
            operation.run();
        } catch (DatabaseException e) {
            log.error("EntityIO database operation failed", e);
            throw new DatabaseNotReadyException(e);
        }
    }


    // Blacklist stuff

    /**
     * @return the whole blacklist aka all entries. Not a lightweight operation, and shouldn't be called outside
     * of initial population of the blacklist (and probably not even then, reworking the ratelimiter is planned).
     */
    @Override
    public List<BlacklistEntry> loadBlacklist() {
        return fetchUserFriendly(blacklistRepo::loadBlacklist);
    }

    @Override
    public BlacklistEntry mergeBlacklistEntry(BlacklistEntry entry) {
        return fetchUserFriendly(() -> blacklistRepo.merge(entry));
    }

    @Override
    public void deleteBlacklistEntry(long id) {
        doUserFriendly(() -> blacklistRepo.delete(id));
    }


    // Guild config stuff

    @Override
    public GuildConfig fetchGuildConfig(Guild guild) {
        return fetchUserFriendly(() -> guildConfigRepo.fetch(guild));
    }


    @Override
    public GuildConfig transformGuildConfig(Guild guild, Function<GuildConfig, GuildConfig> transformation) {
        GuildConfig guildConfig = fetchUserFriendly(() -> guildConfigRepo.fetch(guild));
        return fetchUserFriendly(() -> guildConfigRepo.merge(transformation.apply(guildConfig)));
    }


    // Guild data stuff

    @Override
    public GuildData fetchGuildData(Guild guild) {
        return fetchUserFriendly(() -> guildDataRepo.fetch(guild));
    }

    @Override
    public GuildData transformGuildData(Guild guild, Function<GuildData, GuildData> transformation) {
        GuildData guildData = fetchUserFriendly(() -> guildDataRepo.fetch(guild));
        return fetchUserFriendly(() -> guildDataRepo.merge(transformation.apply(guildData)));
    }


    // Guild modules stuff

    @Override
    public GuildModules fetchGuildModules(Guild guild) {
        return fetchUserFriendly(() -> guildModulesRepo.fetch(guild));
    }

    @Override
    public GuildModules transformGuildModules(Guild guild, Function<GuildModules, GuildModules> transformation) {
        GuildModules guildModules = fetchUserFriendly(() -> guildModulesRepo.fetch(guild));
        return fetchUserFriendly(() -> guildModulesRepo.merge(transformation.apply(guildModules)));
    }


    // Guild permission stuff

    @Override
    public GuildPermissions fetchGuildPermissions(Guild guild) {
        return fetchUserFriendly(() -> guildPermsRepo.fetch(guild));
    }

    @Override
    public GuildPermissions transformGuildPerms(Guild guild, Function<GuildPermissions, GuildPermissions> transformation) {
        GuildPermissions guildPerms = fetchUserFriendly(() -> guildPermsRepo.fetch(guild));
        return fetchUserFriendly(() -> guildPermsRepo.merge(transformation.apply(guildPerms)));
    }


    // Prefix stuff

    @Override
    public Prefix transformPrefix(Guild guild, Function<Prefix, Prefix> transformation) {
        Prefix prefix = fetchUserFriendly(() -> prefixRepo.fetch(new GuildBotComposite(guild, DiscordUtil.getBotId())));
        return fetchUserFriendly(() -> prefixRepo.merge(transformation.apply(prefix)));
    }

    @Override
    public Optional<String> getPrefix(GuildBotComposite id) {
        return fetchUserFriendly(() -> Optional.ofNullable(prefixRepo.getPrefix(id)));
    }


    // Search result stuff

    /**
     * Merge a search result into the database.
     *
     * @return the merged SearchResult object, or null when there is no cache database
     */
    @Override
    @Nullable
    public SearchResult merge(SearchResult searchResult) {
        if (searchResultRepo != null) {
            return searchResultRepo.merge(searchResult);
        } else {
            return null;
        }
    }

    /**
     * @param maxAgeMillis the maximum age of the cached search result; provide a negative value for eternal cache
     * @return the cached search result; may return null for a non-existing or outdated search, or when there is no
     * cache database
     */
    @Override
    @Nullable
    public SearchResult getSearchResult(SearchResult.SearchResultId id, long maxAgeMillis) {
        if (searchResultRepo == null) {
            return null;
        } else {
            return getUserFriendly(() -> searchResultRepo.getMaxAged(id, maxAgeMillis));
        }
    }
}
