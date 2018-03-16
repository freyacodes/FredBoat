/*
 *
 * MIT License
 *
 * Copyright (c) 2017-2018 Frederik Ar. Mikkelsen
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

import net.dv8tion.jda.core.entities.Guild;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ColumnDefault;
import space.npstr.sqlsauce.entities.SaucedEntity;
import space.npstr.sqlsauce.fp.types.EntityKey;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.persistence.*;

/**
 * Created by napster on 23.01.18.
 * <p>
 * FredBoat internal info kept on guilds
 */
@Entity
@Table(name = "guild_data")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "guild_data")
public class GuildData extends SaucedEntity<Long, GuildData> {

    @Id
    @Column(name = "guild_id", nullable = false)
    private long guildId;

    @Column(name = "ts_hello_sent",
            nullable = false)
    @ColumnDefault(value = "0")
    private long timestampHelloSent;


    //for jpa / db wrapper
    GuildData() {
    }

    @Nonnull
    public static EntityKey<Long, GuildData> key(@Nonnull Guild guild) {
        return EntityKey.of(guild.getIdLong(), GuildData.class);
    }

    @Nonnull
    @Override
    public GuildData setId(@Nonnull Long guildId) {
        this.guildId = guildId;
        return this;
    }

    @Nonnull
    @Override
    public Long getId() {
        return guildId;
    }

    public long getTimestampHelloSent() {
        return timestampHelloSent;
    }

    @Nonnull
    @CheckReturnValue
    public GuildData helloSent() {
        this.timestampHelloSent = System.currentTimeMillis();
        return this;
    }
}
