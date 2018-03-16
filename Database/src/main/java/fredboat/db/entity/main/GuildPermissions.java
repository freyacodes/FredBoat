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

import fredboat.definitions.PermissionLevel;
import net.dv8tion.jda.core.entities.Guild;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import space.npstr.sqlsauce.entities.SaucedEntity;
import space.npstr.sqlsauce.fp.types.EntityKey;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "guild_permissions")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "guild_permissions")
public class GuildPermissions extends SaucedEntity<String, GuildPermissions> {

    // Guild ID
    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "list_admin", nullable = false, columnDefinition = "text")
    private String adminList = "";

    @Column(name = "list_dj", nullable = false, columnDefinition = "text")
    private String djList = "";

    @Column(name = "list_user", nullable = false, columnDefinition = "text")
    private String userList = "";

    //for jpa / db wrapper
    GuildPermissions() {
    }

    @Nonnull
    public static EntityKey<String, GuildPermissions> key(@Nonnull Guild guild) {
        return EntityKey.of(guild.getId(), GuildPermissions.class);
    }

    @Nonnull
    @Override
    public GuildPermissions setId(@Nonnull String id) {
        this.id = id;

        //Set up default permissions. Note that the @everyone role of a guild is of the same snowflake as the guild
        // This code works because setId() is only ever called when creating a new instance of this entity after failing
        // not finding it in the database, and never else. There is no need to ever set the id on this object outside of
        // that case.
        this.djList = id;
        this.userList = id;

        return this;
    }

    @Nonnull
    @Override
    public String getId() {
        return id;
    }

    public List<String> getAdminList() {
        if (adminList == null) return new ArrayList<>();

        return Arrays.asList(adminList.split(" "));
    }

    @Nonnull
    public GuildPermissions setAdminList(List<String> list) {
        StringBuilder str = new StringBuilder();
        for (String item : list) {
            str.append(item).append(" ");
        }

        adminList = str.toString().trim();
        return this;
    }

    public List<String> getDjList() {
        if (djList == null) return new ArrayList<>();

        return Arrays.asList(djList.split(" "));
    }

    @Nonnull
    public GuildPermissions setDjList(List<String> list) {
        StringBuilder str = new StringBuilder();
        for (String item : list) {
            str.append(item).append(" ");
        }

        djList = str.toString().trim();
        return this;
    }

    public List<String> getUserList() {
        if (userList == null) return new ArrayList<>();

        return Arrays.asList(userList.split(" "));
    }

    @Nonnull
    public GuildPermissions setUserList(List<String> list) {
        StringBuilder str = new StringBuilder();
        for (String item : list) {
            str.append(item).append(" ");
        }

        userList = str.toString().trim();
        return this;
    }

    public List<String> getFromEnum(PermissionLevel level) {
        switch (level) {
            case ADMIN:
                return getAdminList();
            case DJ:
                return getDjList();
            case USER:
                return getUserList();
            default:
                throw new IllegalArgumentException("Unexpected enum " + level);
        }
    }

    @Nonnull
    public GuildPermissions setFromEnum(PermissionLevel level, List<String> list) {
        switch (level) {
            case ADMIN:
                return setAdminList(list);
            case DJ:
                return setDjList(list);
            case USER:
                return setUserList(list);
            default:
                throw new IllegalArgumentException("Unexpected enum " + level);
        }
    }

}
