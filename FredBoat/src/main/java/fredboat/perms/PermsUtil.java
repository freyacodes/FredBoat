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

import fredboat.commandmeta.abs.CommandContext;
import fredboat.db.entity.main.GuildPermissions;
import fredboat.feature.togglz.FeatureFlags;
import fredboat.main.BotController;
import fredboat.main.Config;
import fredboat.util.DiscordUtil;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;

import java.util.List;

/**
 * This class provides utility methods for FredBoat's own permission system, **not** the Discord permission system.
 */
public class PermsUtil {

    public static PermissionLevel getPerms(Member member) {
        if (DiscordUtil.getOwnerId(member.getJDA()) == member.getUser().getIdLong()) {
            return PermissionLevel.BOT_OWNER; // https://fred.moe/Q-EB.png
        } else if (isBotAdmin(member)) {
            return PermissionLevel.BOT_ADMIN;
        } else if (member.hasPermission(Permission.ADMINISTRATOR)) {
            return PermissionLevel.ADMIN;
        }

        if (!FeatureFlags.PERMISSIONS.isActive()) {
            return member.hasPermission(Permission.MESSAGE_MANAGE) ? PermissionLevel.DJ : PermissionLevel.USER;
        }

        GuildPermissions gp = BotController.INS.getEntityIO().fetchGuildPermissions(member.getGuild());

        if (checkList(gp.getAdminList(), member)) return PermissionLevel.ADMIN;
        if (checkList(gp.getDjList(), member)) return PermissionLevel.DJ;
        if (checkList(gp.getUserList(), member)) return PermissionLevel.USER;

        return PermissionLevel.BASE;
    }

    /**
     * @return True if the provided member has at least the requested PermissionLevel or higher. False if not.
     */
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
            context.replyWithName(context.i18nFormat("cmdPermsTooLow", minLevel, actual));
            return false;
        }
    }

    /**
     * returns true if the member is or holds a role defined as admin in the configuration file
     */
    private static boolean isBotAdmin(Member member) {
        boolean botAdmin = false;
        for (String id : Config.CONFIG.getAdminIds()) {
            Role r = member.getGuild().getRoleById(id);
            if (member.getUser().getId().equals(id)
                    || (r != null && member.getRoles().contains(r))) {
                botAdmin = true;
                break;
            }
        }
        return botAdmin;
    }

    public static boolean checkList(List<String> list, Member member) {
        if (member.hasPermission(Permission.ADMINISTRATOR)) return true;

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
