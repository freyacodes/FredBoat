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

package fredboat.db.entity.main;

import net.dv8tion.jda.core.entities.Guild;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import space.npstr.sqlsauce.entities.SaucedEntity;
import space.npstr.sqlsauce.fp.types.EntityKey;

import javax.annotation.Nonnull;
import javax.persistence.*;

@Entity
@Table(name = "guild_config")
@Cacheable
@Cache(usage= CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region="guild_config")
public class GuildConfig extends SaucedEntity<String, GuildConfig> {

    @Id
    @Column(name = "guildid", nullable = false)
    private String guildId;

    @Column(name = "track_announce", nullable = false)
    private boolean trackAnnounce = false;

    @Column(name = "auto_resume", nullable = false)
    private boolean autoResume = false;

    @Column(name = "lang", nullable = false)
    private String lang = "en_US";

    //for jpa / db wrapper
    public GuildConfig() {
    }

    @Nonnull
    public static EntityKey<String, GuildConfig> key(@Nonnull String guildId) {
        return EntityKey.of(guildId, GuildConfig.class);
    }

    @Nonnull
    public static EntityKey<String, GuildConfig> key(long guildId) {
        return key(Long.toString(guildId));
    }

    @Nonnull
    public static EntityKey<String, GuildConfig> key(@Nonnull Guild guild) {
        return key(guild.getId());
    }

    @Nonnull
    @Override
    public GuildConfig setId(@Nonnull String id) {
        this.guildId = id;
        return this;
    }

    @Nonnull
    @Override
    public String getId() {
        return this.guildId;
    }

    public boolean isTrackAnnounce() {
        return trackAnnounce;
    }

    @Nonnull
    public GuildConfig setTrackAnnounce(boolean trackAnnounce) {
        this.trackAnnounce = trackAnnounce;
        return this;
    }

    public boolean isAutoResume() {
        return autoResume;
    }

    @Nonnull
    public GuildConfig setAutoResume(boolean autoplay) {
        this.autoResume = autoplay;
        return this;
    }

    public String getLang() {
        return lang;
    }

    @Nonnull
    public GuildConfig setLang(String lang) {
        this.lang = lang;
        return this;
    }

}
