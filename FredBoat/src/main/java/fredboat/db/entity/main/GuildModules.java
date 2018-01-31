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

import fredboat.commandmeta.CommandRegistry;
import net.dv8tion.jda.core.entities.Guild;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import space.npstr.sqlsauce.entities.SaucedEntity;
import space.npstr.sqlsauce.fp.types.EntityKey;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by napster on 29.12.17.
 * <p>
 * There are many more concise ways to persist the module configuration of a guild, here is are reasons for the chosen design:
 * <p>
 * - An entity of it's own:
 * To keep the guild config small, and be more flexible. Maybe we want one of these things to be bot specific in the future?
 * Having different ehcache regions for both is a bonus.
 * <p>
 * - Each module in a column:
 * Alternatives like discord permission style bits in a single long value, or collections/lists of enabled permissions
 * lack option to switch between opt-in and opt-out patterns for modules, and having to do any changes or migrations to
 * those is more complicated. The possible performance/ressource usage improvement is probably not worth it.
 * <p>
 * <p>
 * - Nullables Columns
 * A null value signals us that a guild has not expressed any preference for a module in any way, or better the other way
 * round: Explicitly enabling/disabling a module tells us something about the preferences of our users. If we were to preset
 * these values, we would lose this information, and could not switch enabled/disabled modules by default reliably for existing guilds.
 */
@Entity
@Table(name = "guild_modules")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "guild_modules")
public class GuildModules extends SaucedEntity<Long, GuildModules> {

    @Id
    @Column(name = "guild_id")
    private long guildId;

    @Nullable
    @Column(name = "admin", nullable = true)
    private Boolean adminModule; //corresponding to CommandRegistry.Module.ADMIN

    @Nullable
    @Column(name = "info", nullable = true)
    private Boolean infoModule; //corresponding to CommandRegistry.Module.INFO

    @Nullable
    @Column(name = "config", nullable = true)
    private Boolean configModule; //corresponding to CommandRegistry.Module.CONFIG

    @Nullable
    @Column(name = "music", nullable = true)
    private Boolean musicModule; //corresponding to CommandRegistry.Module.MUSIC

    @Nullable
    @Column(name = "mod", nullable = true)
    private Boolean modModule; //corresponding to CommandRegistry.Module.MOD

    @Nullable
    @Column(name = "util", nullable = true)
    private Boolean utilModule; //corresponding to CommandRegistry.Module.UTIL

    @Nullable
    @Column(name = "fun", nullable = true)
    private Boolean funModule; //corresponding to CommandRegistry.Module.FUN


    //for jpa / database wrapper
    public GuildModules() {
    }

    @Nonnull
    public static EntityKey<Long, GuildModules> key(@Nonnull Guild guild) {
        return EntityKey.of(guild.getIdLong(), GuildModules.class);
    }

    @Nonnull
    @Override
    public GuildModules setId(@Nonnull Long guildId) {
        this.guildId = guildId;
        return this;
    }

    @Nonnull
    @Override
    public Long getId() {
        return guildId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.guildId);
    }

    @Override
    public boolean equals(final Object obj) {
        return (obj instanceof GuildModules) && ((GuildModules) obj).guildId == this.guildId;
    }

    @Nonnull
    @CheckReturnValue
    public GuildModules enableModule(@Nonnull CommandRegistry.Module module) {
        return setModule(module, true);
    }

    @Nonnull
    @CheckReturnValue
    public GuildModules disableModule(@Nonnull CommandRegistry.Module module) {
        return setModule(module, false);
    }

    @Nonnull
    @CheckReturnValue
    public GuildModules resetModule(@Nonnull CommandRegistry.Module module) {
        return setModule(module, null);
    }

    @Nonnull
    @CheckReturnValue
    private GuildModules setModule(@Nonnull CommandRegistry.Module module, @Nullable Boolean enabled) {
        switch (module) {
            case ADMIN:
                adminModule = enabled;
                break;
            case INFO:
                infoModule = enabled;
                break;
            case CONFIG:
                configModule = enabled;
                break;
            case MUSIC:
                musicModule = enabled;
                break;
            case MOD:
                modModule = enabled;
                break;
            case UTIL:
                utilModule = enabled;
                break;
            case FUN:
                funModule = enabled;
                break;
            default:
                throw new RuntimeException("Unknown Module " + module.name());
        }
        return this;
    }

    /**
     * @return true if the provided module is enabled, false if not, or null if no preference has been set.
     */
    @Nullable
    public Boolean isModuleEnabled(@Nonnull CommandRegistry.Module module) {
        switch (module) {
            case ADMIN:
                return adminModule;
            case INFO:
                return infoModule;
            case CONFIG:
                return configModule;
            case MUSIC:
                return musicModule;
            case MOD:
                return modModule;
            case UTIL:
                return utilModule;
            case FUN:
                return funModule;
            default:
                throw new RuntimeException("Unknown Module " + module.name());
        }
    }

    /**
     * @return true if the provided module is enabled, false if not. If no value has been specified, return the provide
     * default value.
     */
    public boolean isModuleEnabled(@Nonnull CommandRegistry.Module module, boolean def) {
        Boolean enabled = isModuleEnabled(module);
        if (enabled != null) {
            return enabled;
        } else {
            return def;
        }
    }

    @Nonnull
    public List<CommandRegistry.Module> getEnabledModules() {
        List<CommandRegistry.Module> enabledModules = new ArrayList<>();
        for (CommandRegistry.Module module : CommandRegistry.Module.values()) {
            if (isModuleEnabled(module, module.enabledByDefault)) {
                enabledModules.add(module);
            }
        }
        return enabledModules;
    }
}
