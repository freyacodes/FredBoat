/*
 * MIT License
 *
 * Copyright (c) 2016 Frederik Ar. Mikkelsen
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

package fredboat.common.db.entities;

import fredboat.common.db.DatabaseManager;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.TextChannel;

@Entity
@Table(name = "guild_config")
public class GuildConfig {

    @Id
    private long guildId;

    @OneToMany
    @JoinColumn(name = "guildconfig")
    private Set<TCConfig> textChannels;

    public GuildConfig() {
        System.out.println("Instantiated!");
    }

    public GuildConfig(Guild guild) {
        System.out.println("Instantiated!" + guild.getId());
        this.guildId = Long.parseLong(guild.getId());

        textChannels = new CopyOnWriteArraySet<>();

        for (TextChannel tc : guild.getTextChannels()) {
            TCConfig tcc = new TCConfig(this, tc);
            textChannels.add(tcc);
        }

        for (TCConfig tcc : textChannels) {
            DatabaseManager.getEntityManager().persist(tcc);
        }
    }

    public Set<TCConfig> getTextChannels() {
        return textChannels;
    }
    
    public void addTextChannel(TCConfig tcc){
        textChannels.add(tcc);
    }
    
    public void removeTextChannel(TCConfig tcc){
        textChannels.remove(tcc);
    }

    public long getGuildId() {
        return guildId;
    }

}
