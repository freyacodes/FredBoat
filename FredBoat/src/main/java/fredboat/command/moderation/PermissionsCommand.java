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

package fredboat.command.moderation;

import fredboat.command.util.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IModerationCommand;
import fredboat.db.EntityReader;
import fredboat.db.EntityWriter;
import fredboat.db.entity.GuildPermissions;
import fredboat.feature.I18n;
import fredboat.feature.togglz.FeatureFlags;
import fredboat.messaging.CentralMessaging;
import fredboat.perms.PermissionLevel;
import fredboat.perms.PermsUtil;
import fredboat.shared.constant.BotConstants;
import fredboat.util.ArgumentUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.IMentionable;
import net.dv8tion.jda.core.entities.ISnowflake;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.utils.PermissionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class PermissionsCommand extends Command implements IModerationCommand {

    private static final Logger log = LoggerFactory.getLogger(PermissionsCommand.class);

    public final PermissionLevel permissionLevel;

    public PermissionsCommand(PermissionLevel permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    @Override
    public void onInvoke(CommandContext context) {
        if (!FeatureFlags.PERMISSIONS.isActive()) {
            context.reply("Permissions are currently disabled.");
            return;
        }
        String[] args = context.args;
        if (args.length < 2) {
            HelpCommand.sendFormattedCommandHelp(context);
            return;
        }

        switch (args[1]) {
            case "del":
            case "delete":
            case "remove":
            case "rem":
            case "rm":
                if (!PermsUtil.checkPermsWithFeedback(PermissionLevel.ADMIN, context)) return;

                if (args.length < 3) {
                    HelpCommand.sendFormattedCommandHelp(context);
                    return;
                }

                remove(context);
                break;
            case "add":
                if (!PermsUtil.checkPermsWithFeedback(PermissionLevel.ADMIN, context)) return;

                if (args.length < 3) {
                    HelpCommand.sendFormattedCommandHelp(context);
                    return;
                }

                add(context);
                break;
            case "list":
            case "ls":
                list(context);
                break;
            default:
                HelpCommand.sendFormattedCommandHelp(context);
                break;
        }
    }

    public void remove(CommandContext context) {
        Guild guild = context.guild;
        Member invoker = context.invoker;
        String term = ArgumentUtil.getSearchTerm(context.msg, context.args, 2);

        List<IMentionable> curList = new ArrayList<>();
        List<IMentionable> search = new ArrayList<>();
        search.addAll(ArgumentUtil.fuzzyRoleSearch(guild, term));
        search.addAll(ArgumentUtil.fuzzyMemberSearch(guild, term, false));
        GuildPermissions gp = EntityReader.getGuildPermissions(guild);
        curList.addAll(idsToMentionables(guild, gp.getFromEnum(permissionLevel)));

        List<IMentionable> itemsInBothLists = new ArrayList<>();

        curList.forEach(mentionable -> {
            if (search.contains(mentionable)) itemsInBothLists.add(mentionable);
        });

        IMentionable selected = ArgumentUtil.checkSingleFuzzySearchResult(itemsInBothLists, context, term);
        if (selected == null) return;

        List<String> newList = new ArrayList<>(gp.getFromEnum(permissionLevel));
        newList.remove(mentionableToId(selected));

        if (permissionLevel == PermissionLevel.ADMIN
                && PermissionLevel.BOT_ADMIN.getLevel() > PermsUtil.getPerms(invoker).getLevel()
                && !PermissionUtil.checkPermission(invoker, Permission.ADMINISTRATOR)
                && !PermsUtil.checkList(newList, invoker)) {
            context.replyWithName(I18n.get(context, "permsFailSelfDemotion"));
            return;
        }

        gp.setFromEnum(permissionLevel, newList);
        EntityWriter.mergeGuildPermissions(gp);

        context.replyWithName(MessageFormat.format(I18n.get(context, "permsRemoved"), mentionableToName(selected), permissionLevel));
    }

    public void add(CommandContext context) {
        Guild guild = context.guild;
        String term = ArgumentUtil.getSearchTerm(context.msg, context.args, 2);

        List<IMentionable> list = new ArrayList<>();
        list.addAll(ArgumentUtil.fuzzyRoleSearch(guild, term));
        list.addAll(ArgumentUtil.fuzzyMemberSearch(guild, term, false));
        GuildPermissions gp = EntityReader.getGuildPermissions(guild);
        list.removeAll(idsToMentionables(guild, gp.getFromEnum(permissionLevel)));

        IMentionable selected = ArgumentUtil.checkSingleFuzzySearchResult(list, context, term);
        if (selected == null) return;

        List<String> newList = new ArrayList<>(gp.getFromEnum(permissionLevel));
        newList.add(mentionableToId(selected));
        gp.setFromEnum(permissionLevel, newList);
        EntityWriter.mergeGuildPermissions(gp);

        context.replyWithName(MessageFormat.format(I18n.get(guild).getString("permsAdded"), mentionableToName(selected), permissionLevel));
    }

    public void list(CommandContext context) {
        Guild guild = context.guild;
        Member invoker = context.invoker;
        GuildPermissions gp = EntityReader.getGuildPermissions(guild);

        List<IMentionable> mentionables = idsToMentionables(guild, gp.getFromEnum(permissionLevel));

        String roleMentions = "";
        String memberMentions = "";

        for (IMentionable mentionable : mentionables) {
            if (mentionable instanceof Role) {
                if (((Role) mentionable).isPublicRole()) {
                    roleMentions = roleMentions + "@everyone" + "\n"; // Prevents ugly double double @@
                } else {
                    roleMentions = roleMentions + mentionable.getAsMention() + "\n";
                }
            } else {
                memberMentions = memberMentions + mentionable.getAsMention() + "\n";
            }
        }

        PermissionLevel invokerPerms = PermsUtil.getPerms(invoker);
        boolean invokerHas = PermsUtil.checkPerms(permissionLevel, invoker);

        if (roleMentions.isEmpty()) roleMentions = "<none>";
        if (memberMentions.isEmpty()) memberMentions = "<none>";

        EmbedBuilder eb = CentralMessaging.getClearThreadLocalEmbedBuilder()
                .setColor(BotConstants.FREDBOAT_COLOR)
                .setTitle(MessageFormat.format(I18n.get(guild).getString("permsListTitle"), permissionLevel))
                .setAuthor(invoker.getEffectiveName(), null, invoker.getUser().getAvatarUrl())
                .addField("Roles", roleMentions, true)
                .addField("Members", memberMentions, true)
                .addField(invoker.getEffectiveName(), (invokerHas ? ":white_check_mark:" : ":x:") + " (" + invokerPerms + ")", false);
        context.reply(CentralMessaging.addFooter(eb, guild.getSelfMember()).build());
    }

    private static String mentionableToId(IMentionable mentionable) {
        if (mentionable instanceof ISnowflake) {
            return ((ISnowflake) mentionable).getId();
        } else if (mentionable instanceof Member) {
            return ((Member) mentionable).getUser().getId();
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static String mentionableToName(IMentionable mentionable) {
        if (mentionable instanceof Role) {
            return ((Role) mentionable).getName();
        } else if (mentionable instanceof Member) {
            return ((Member) mentionable).getUser().getName();
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static List<IMentionable> idsToMentionables(Guild guild, List<String> list) {
        List<IMentionable> out = new ArrayList<>();

        for (String id : list) {
            if (id.equals("")) continue;

            if (guild.getRoleById(id) != null) {
                out.add(guild.getRoleById(id));
                continue;
            }

            if (guild.getMemberById(id) != null) {
                out.add(guild.getMemberById(id));
            }
        }

        return out;
    }

    @Override
    public String help(Guild guild) {
        String usage = "{0}{1} add <role/user>\n{0}{1} del <role/user>\n{0}{1} list\n#";
        return usage + MessageFormat.format(I18n.get(guild).getString("helpPerms"), permissionLevel.getName()) + " https://docs.fredboat.com/permissions";
    }

}
