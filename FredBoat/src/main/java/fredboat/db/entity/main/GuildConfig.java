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

import fredboat.commandmeta.CommandRegistry;
import fredboat.db.entity.IEntity;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ColumnDefault;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "guild_config")
@Cacheable
@Cache(usage= CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region="guild_config")
public class GuildConfig implements IEntity, Serializable {

    private static final long serialVersionUID = 5055243002380106205L;

    @Id
    @Column(name = "guildid", nullable = false)
    private String guildId;

    @Column(name = "track_announce", nullable = false)
    private boolean trackAnnounce = false;

    @Column(name = "auto_resume", nullable = false)
    private boolean autoResume = false;

    @Column(name = "lang", nullable = false)
    private String lang = "en_US";

    @Column(name = "enabled_module_bits", nullable = false)
    @ColumnDefault(value = "15") //see default modules
    private long enabledModuleBits = CommandRegistry.Module.DEFAULT_MODULES;

    public GuildConfig(String id) {
        this.guildId = id;
    }

    @Override
    public void setId(String id) {
        this.guildId = id;
    }

    public GuildConfig() {
    }

    public String getGuildId() {
        return guildId;
    }

    public boolean isTrackAnnounce() {
        return trackAnnounce;
    }

    public void setTrackAnnounce(boolean trackAnnounce) {
        this.trackAnnounce = trackAnnounce;
    }

    public boolean isAutoResume() {
        return autoResume;
    }

    public void setAutoResume(boolean autoplay) {
        this.autoResume = autoplay;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public long getEnabledModuleBits() {
        return enabledModuleBits;
    }

    public void enableModule(@Nonnull CommandRegistry.Module module) {
        enabledModuleBits |= module.bits;
    }

    public void disableModule(@Nonnull CommandRegistry.Module module) {
        enabledModuleBits &= ~module.bits;
    }

    @Nonnull
    public List<CommandRegistry.Module> getEnabledModules() {
        List<CommandRegistry.Module> enabledModules = new ArrayList<>();
        for (CommandRegistry.Module module : CommandRegistry.Module.values()) {
            if (isModuleEnabled(module)) {
                enabledModules.add(module);
            }
        }
        return enabledModules;
    }

    //check whether the bits of the module are present in the enabled bits
    public boolean isModuleEnabled(@Nonnull CommandRegistry.Module module) {
        return (module.bits & enabledModuleBits) == module.bits;
    }
}
