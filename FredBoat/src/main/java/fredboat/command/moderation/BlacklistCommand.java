package fredboat.command.moderation;

import fredboat.command.util.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IModerationCommand;
import fredboat.db.EntityReader;
import fredboat.db.EntityWriter;
import fredboat.db.entity.GuildPermissions;
import fredboat.feature.togglz.FeatureFlags;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import fredboat.perms.PermsUtil;
import fredboat.shared.constant.BotConstants;
import fredboat.util.ArgumentUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.utils.PermissionUtil;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class BlacklistCommand extends Command implements IModerationCommand {
    @Override
    public void onInvoke(@Nonnull CommandContext context) {
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

    private void remove(CommandContext context) {
        Guild guild = context.guild;
        String term = ArgumentUtil.getSearchTerm(context.msg, context.args, 2);

        List<IMentionable> search = new ArrayList<>();
        search.addAll(ArgumentUtil.fuzzyRoleSearch(guild, term));
        search.addAll(ArgumentUtil.fuzzyMemberSearch(guild, term, false));
        GuildPermissions gp = EntityReader.getGuildPermissions(guild);

        IMentionable selected = ArgumentUtil.checkSingleFuzzySearchResult(search, context, term);
        if (selected == null) return;

        if (!gp.getBlacklistedList().contains(mentionableToId(selected))) {
            context.replyWithName(context. i18nFormat("permsNotAdded", "`" + mentionableToName(selected) + "`", "`" + PermissionLevel.BLACKLISTED + "`"));
            return;
        }

        List<String> newList = new ArrayList<>(gp.getBlacklistedList());
        newList.remove(mentionableToId(selected));
        gp.setBlacklistedList(newList);
        EntityWriter.mergeGuildPermissions(gp);

        context.replyWithName(context.i18nFormat("permsRemoved", mentionableToName(selected), PermissionLevel.BLACKLISTED));
    }

    private void add(CommandContext context) {
        Guild guild = context.guild;
        String term = ArgumentUtil.getSearchTerm(context.msg, context.args, 2);

        List<IMentionable> list = new ArrayList<>();
        list.addAll(ArgumentUtil.fuzzyRoleSearch(guild, term));
        list.addAll(ArgumentUtil.fuzzyMemberSearch(guild, term, false));
        GuildPermissions gp = EntityReader.getGuildPermissions(guild);

        IMentionable selected = ArgumentUtil.checkSingleFuzzySearchResult(list, context, term);
        if (selected == null) return;

        if (gp.getBlacklistedList().contains(mentionableToId(selected))) {
            context.replyWithName(context.i18nFormat("permsAlreadyAdded", "`" + mentionableToName(selected) + "`", "`" + PermissionLevel.BLACKLISTED + "`"));
            return;
        }

        List<String> newList = new ArrayList<>(gp.getBlacklistedList());
        newList.add(mentionableToId(selected));

        if (!PermissionUtil.checkPermission(context.invoker, Permission.ADMINISTRATOR)
                && PermsUtil.checkList(newList, context.invoker)) {
            context.replyWithName("You cant add this to the Blacklisted list as it would add yourself to the blacklist!");
            return;
        }

        gp.setBlacklistedList(newList);
        EntityWriter.mergeGuildPermissions(gp);

        context.replyWithName(context.i18nFormat("permsAdded", mentionableToName(selected), PermissionLevel.BLACKLISTED));
    }

    private void list(CommandContext context) {
        Guild guild = context.guild;
        Member invoker = context.invoker;
        GuildPermissions gp = EntityReader.getGuildPermissions(guild);

        List<IMentionable> mentionables = idsToMentionables(guild, gp.getBlacklistedList());

        StringBuilder roleMentions = new StringBuilder();
        StringBuilder memberMentions = new StringBuilder();

        for (IMentionable mentionable : mentionables) {
            if (mentionable instanceof Role) {
                if (((Role) mentionable).isPublicRole()) {
                    roleMentions.append("@everyone").append("\n"); // Prevents ugly double double @@
                } else {
                    roleMentions.append(mentionable.getAsMention()).append("\n");
                }
            } else {
                memberMentions.append(mentionable.getAsMention()).append("\n");
            }
        }

        PermissionLevel invokerPerms = PermsUtil.getPerms(invoker);
        boolean isBlacklisted = PermsUtil.isBlacklisted(invoker);

        if (roleMentions.length() == 0) roleMentions = new StringBuilder("<none>");
        if (memberMentions.length() == 0) memberMentions = new StringBuilder("<none>");

        EmbedBuilder eb = CentralMessaging.getClearThreadLocalEmbedBuilder()
                .setColor(BotConstants.FREDBOAT_COLOR)
                .setTitle("User and Roles which are Blacklisted")
                .setAuthor(invoker.getEffectiveName(), null, invoker.getUser().getAvatarUrl())
                .addField("Roles", roleMentions.toString(), true)
                .addField("Members", memberMentions.toString(), true)
                .addField(invoker.getEffectiveName(), (isBlacklisted ? ":white_check_mark:" : ":x:") + " (" + invokerPerms + ")", false);
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

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        String usage = "{0}{1} add <role/user>\n{0}{1} del <role/user>\n{0}{1} list\n#";
        return usage + "Add or Remove users from the Blacklist";
    }
}
