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

package fredboat.command.util;

import fredboat.command.info.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.util.ArgumentUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.IMentionable;
import net.dv8tion.jda.core.entities.Role;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by napster on 18.01.18.
 */
public class RoleInfoCommand extends Command {

    public RoleInfoCommand(@Nonnull String name, String... aliases) {
        super(name, aliases);
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} <role id/mention>\n#" + "Display information about a role."; //todo i18n
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {

        if (!context.hasArguments()) {
            HelpCommand.sendFormattedCommandHelp(context);
            return;
        }

        List<IMentionable> roles = new ArrayList<>(ArgumentUtil.fuzzyRoleSearch(context.guild, context.rawArgs));

        Role role = (Role) ArgumentUtil.checkSingleFuzzySearchResult(roles, context, context.rawArgs);
        if (role == null) return;

        EmbedBuilder eb = CentralMessaging.getClearThreadLocalEmbedBuilder();
        eb.setTitle("Information about role " + role.getName() + ":") //todo i18n
                .setColor(role.getColor())
                .addField("Name", role.getName(), true) //todo i18n
                .addField("Id", role.getId(), true) //todo i18n
                .addField("Color", colorToHex(role.getColor()), true) //todo i18n
                .addField("Hoisted", Boolean.toString(role.isHoisted()), true) //todo i18n
                .addField("Mentionable", Boolean.toString(role.isMentionable()), true) //todo i18n
                .addField("Managed", Boolean.toString(role.isManaged()), true) //todo i18n
                .addField("Permissions", permissionsToString(role.getPermissions()), true) //todo i18n
        ;
        context.reply(eb.build());
    }

    private static String permissionsToString(@Nullable Collection<Permission> perms) {
        if (perms == null || perms.isEmpty()) {
            return "none"; //todo i18n
        }
        return String.join(", ", perms.stream().map(Permission::getName).collect(Collectors.toList()));
    }

    private static String colorToHex(@Nullable Color color) {
        if (color == null) {
            return "none"; //todo i18n
        }
        return "#"
                + Integer.toString(color.getRed(), 16).toUpperCase()
                + Integer.toString(color.getGreen(), 16).toUpperCase()
                + Integer.toString(color.getBlue(), 16).toUpperCase();
    }
}
