/*
 *
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
 */

package fredboat.db.entity.main;

import fredboat.db.EntityIO;
import fredboat.util.DiscordUtil;
import net.dv8tion.jda.core.entities.Guild;
import space.npstr.sqlsauce.entities.GuildBotComposite;
import space.npstr.sqlsauce.entities.SaucedEntity;
import space.npstr.sqlsauce.fp.types.EntityKey;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by napster on 22.12.17.
 * <p>
 * The caching of this entity is not managed by ehcache, instead a guava cache is used,
 * see {@link fredboat.command.config.PrefixCommand}
 */
@Entity
@Table(name = "prefixes")
public class Prefix extends SaucedEntity<GuildBotComposite, Prefix> {

    @EmbeddedId
    private GuildBotComposite id;

    @Nullable
    //may be null to indicate that there is no custom prefix for this guild
    @Column(name = "prefix", nullable = true, columnDefinition = "text")
    private String prefix;

    //for jpa & the database wrapper
    public Prefix() {
    }

    public Prefix(@Nonnull GuildBotComposite id, @Nullable String prefix) {
        this.id = id;
        this.prefix = prefix;
    }

    @Nonnull
    @Override
    public Prefix setId(@Nonnull GuildBotComposite id) {
        this.id = id;
        return this;
    }

    @Nonnull
    @Override
    public GuildBotComposite getId() {
        return this.id;
    }

    @Nonnull
    @Override
    public Class<Prefix> getClazz() {
        return Prefix.class;
    }

    @Nullable
    public String getPrefix() {
        return this.prefix;
    }

    @Nonnull
    @CheckReturnValue
    public Prefix setPrefix(@Nullable String prefix) {
        this.prefix = prefix;
        return this;
    }

    /**
     * Shortcut to creating an entity key for the provided guild and the running bot
     */
    public static EntityKey<GuildBotComposite, Prefix> key(@Nonnull Guild guild) {
        return EntityKey.of(new GuildBotComposite(guild, DiscordUtil.getBotId()), Prefix.class);
    }

    @Nullable
    public static Optional<String> getPrefix(long guildId, long botId) {
        //language=JPAQL
        String query = "SELECT p.prefix FROM Prefix p WHERE p.id = :id";
        Map<String, Object> params = new HashMap<>();
        params.put("id", new GuildBotComposite(guildId, botId));

        List<String> result = EntityIO.doUserFriendly(EntityIO.onMainDb(
                wrapper -> wrapper.selectJpqlQuery(query, params, String.class)
        ));
        if (result.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(result.get(0));
        }
    }
}
