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

package fredboat.perms;

import fredboat.Config;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.db.EntityReader;
import fredboat.db.entity.common.GuildPermissions;
import fredboat.feature.togglz.FeatureFlags;
import fredboat.util.DiscordUtil;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.utils.PermissionUtil;

import java.text.MessageFormat;
import java.util.List;

public class PermsUtil {

    public static PermissionLevel getPerms(Member member) {
        if (isUserBotOwner(member.getUser())) {
            return PermissionLevel.BOT_OWNER; // https://fred.moe/Q-EB.png
        } else if (isAdmin(member)) {
            return PermissionLevel.BOT_ADMIN;
        } else if (PermissionUtil.checkPermission(member, Permission.ADMINISTRATOR)) {
            return PermissionLevel.ADMIN;
        }

        if (!FeatureFlags.PERMISSIONS.isActive()) {
            return PermissionUtil.checkPermission(member, Permission.MESSAGE_MANAGE) ? PermissionLevel.DJ : PermissionLevel.USER;
        }

        GuildPermissions gp = EntityReader.getOrCreateEntity(member.getGuild().getId(), GuildPermissions.class);

        if (checkList(gp.getAdminList(), member)) return PermissionLevel.ADMIN;
        if (checkList(gp.getDjList(), member)) return PermissionLevel.DJ;
        if (checkList(gp.getUserList(), member)) return PermissionLevel.USER;

        return PermissionLevel.BASE;
    }

    public static boolean checkPerms(PermissionLevel minLevel, Member member) {
        return getPerms(member).getLevel() >= minLevel.getLevel();
    }

    /**
     * Check whether the invoker has at least the provided minimum permissions level
     */
    public static boolean checkPermsWithFeedback(PermissionLevel minLevel, CommandContext context) {
        PermissionLevel actual = getPerms(context.invoker);

        if (actual.getLevel() >= minLevel.getLevel()) {
            return true;
        } else {
            context.replyWithName(MessageFormat.format(
                    "You don''t have permission to run this command! This command requires `{0}` but you only have `{1}`",
                    minLevel, actual)); //TODO i18n
            return false;
        }
    }

    /**
     * returns true if the member is or holds a role defined as admin in the configuration file
     */
    private static boolean isAdmin(Member member) {
        boolean admin = false;
        for (String id : Config.CONFIG.getAdminIds()) {
            Role r = member.getGuild().getRoleById(id);
            if (member.getUser().getId().equals(id)
                    || (r != null && member.getRoles().contains(r))) {
                admin = true;
                break;
            }
        }
        return admin;
    }

    // TODO: Make private and use getPerms() instead
    public static boolean isUserBotOwner(User user) {
        return DiscordUtil.getOwnerId(user.getJDA()).equals(user.getId());
    }

    public static boolean checkList(List<String> list, Member member) {
        if (PermissionUtil.checkPermission(member, Permission.ADMINISTRATOR)) return true;

        for (String id : list) {
            if (id.isEmpty()) continue;

            if (id.equals(member.getUser().getId())) return true;

            Role role = member.getGuild().getRoleById(id);
            if (role != null &&
                    (role.isPublicRole() || member.getRoles().contains(role)))
                return true;
        }

        return false;
    }
}
