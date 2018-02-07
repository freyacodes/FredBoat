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

import fredboat.db.entity.IEntity;
import fredboat.perms.PermissionLevel;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "guild_permissions")
@Cacheable
@Cache(usage= CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region="guild_permissions")
public class GuildPermissions implements IEntity, Serializable {

    private static final long serialVersionUID = 72988747242640626L;

    // Guild ID
    @Id
    @Column(name = "id")
    private String id;

    public GuildPermissions() {}

    @Override
    public void setId(String id) {
        this.id = id;

        // Set up default permissions. Note that the @everyone role of a guild is of the same snowflake as the guild
        this.djList = id;
        this.userList = id;
    }

    @Column(name = "list_admin", nullable = false, columnDefinition = "text")
    private String adminList = "";

    @Column(name = "list_dj", nullable = false, columnDefinition = "text")
    private String djList = "";

    @Column(name = "list_user", nullable = false, columnDefinition = "text")
    private String userList = "";

    public List<String> getAdminList() {
        if (adminList == null) return new ArrayList<>();

        return Arrays.asList(adminList.split(" "));
    }

    public void setAdminList(List<String> list) {
        StringBuilder str = new StringBuilder();
        for (String item : list) {
            str.append(item).append(" ");
        }

        adminList = str.toString().trim();
    }

    public List<String> getDjList() {
        if (djList == null) return new ArrayList<>();

        return Arrays.asList(djList.split(" "));
    }

    public void setDjList(List<String> list) {
        StringBuilder str = new StringBuilder();
        for (String item : list) {
            str.append(item).append(" ");
        }

        djList = str.toString().trim();
    }

    public List<String> getUserList() {
        if (userList == null) return new ArrayList<>();

        return Arrays.asList(userList.split(" "));
    }

    public void setUserList(List<String> list) {
        StringBuilder str = new StringBuilder();
        for (String item : list) {
            str.append(item).append(" ");
        }

        userList = str.toString().trim();
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

    public void setFromEnum(PermissionLevel level, List<String> list) {
        switch (level) {
            case ADMIN:
                setAdminList(list);
                break;
            case DJ:
                setDjList(list);
                break;
            case USER:
                setUserList(list);
                break;
            default:
                throw new IllegalArgumentException("Unexpected enum " + level);
        }
    }

}
