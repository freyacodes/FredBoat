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

import fredboat.FredBoat;
import fredboat.db.entity.GuildBotId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.sqlsauce.entities.SaucedEntity;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by napster on 22.12.17.
 * <p>
 * The caching of this entity is not managed by ehcache, instead a guava cache is used,
 * see {@link fredboat.command.moderation.PrefixCommand}
 */
@Entity
@Table(name = "prefixes")
public class Prefix extends SaucedEntity<GuildBotId, Prefix> {

    private static final Logger log = LoggerFactory.getLogger(Prefix.class);

    @Id
    private GuildBotId prefixId;

    @Nullable
    //may be null to indicate that there is no custom prefix for this guild
    @Column(name = "prefix", nullable = true, columnDefinition = "text")
    private String prefix;

    //for jpa & the database wrapper
    public Prefix() {
    }

    public Prefix(long guildId, long botId, @Nullable String prefix) {
        this.prefixId = new GuildBotId(guildId, botId);
        this.prefix = prefix;
    }

    @Nonnull
    @Override
    public Prefix setId(@Nonnull GuildBotId id) {
        this.prefixId = id;
        return this;
    }

    @Nonnull
    @Override
    public GuildBotId getId() {
        return this.prefixId;
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

    @Nullable
    public static Optional<String> getPrefix(long guildId, long botId) {
        //language=JPAQL
        String query = "SELECT p.prefix FROM Prefix p WHERE p.prefixId = :prefixId";
        Map<String, Object> params = new HashMap<>();
        params.put("prefixId", new GuildBotId(guildId, botId));

        List<String> result = FredBoat.getMainDbWrapper().selectJpqlQuery(query, params, String.class);
        if (result.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(result.get(0));
        }
    }
}
