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

package fredboat.command.admin;

import fredboat.FredBoat;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.PermissionOverride;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by napster on 18.01.18.
 *
 * This command is for debugging and FredBoat support staff.
 */
public class DiscordPermissionCommand extends Command implements ICommandRestricted {

    public DiscordPermissionCommand(@Nonnull String name, String... aliases) {
        super(name, aliases);
    }

    @Nonnull
    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.BOT_ADMIN;
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} [channel id or mention] [user id or mention]\n#Show permissions of a user and any roles they "
                + "have for a channel and guild. If no user is provided, FredBoat shows permissions for itself. If no "
                + "channel is provided, permissions for the channel where the command is issued are shown.";
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {

        TextChannel tc = context.channel;

        if (!context.msg.getMentionedChannels().isEmpty()) {
            tc = context.msg.getMentionedChannels().get(0);
        } else if (context.hasArguments()) {
            try {
                long channelId = Long.parseUnsignedLong(context.args[0]);
                TextChannel textChannel = FredBoat.getTextChannelById(channelId);
                if (textChannel == null) {
                    context.reply(String.format("No text channel with id `%s` found.", channelId));
                    return;
                }
                tc = textChannel;
            } catch (NumberFormatException ignored) {
            }
        }
        Guild guild = tc.getGuild();


        Member member = guild.getSelfMember();
        if (!context.getMentionedUsers().isEmpty()) {
            Member m = guild.getMember(context.getMentionedUsers().get(0));
            if (m != null) {
                member = m;
            }
        } else if (context.args.length > 1) {
            try {
                long userId = Long.parseUnsignedLong(context.args[1]);
                Member m = guild.getMemberById(userId);
                if (m == null) {
                    context.reply("No member with id `%s` found in the specified guild.");
                    return;
                }
                member = m;
            } catch (NumberFormatException ignored) {
            }
        }


        //we need the following things:
        // server permissions for all roles of the member, including the @everyone role
        // category overrides for roles + @everyone
        // category override for member
        // channel overrides for roles + @everyone
        // channel override for member

        EmbedBuilder eb = CentralMessaging.getColoredEmbedBuilder();
        eb.setTitle("Full allowed/denied discord permissions");
        eb.setDescription("Empty roles / permission overrides omitted.");

        String userStr = member.getAsMention()
                + "\n" + member.getUser().getIdLong()
                + "\n" + TextUtils.escapeMarkdown(member.getEffectiveName())
                + "\n" + TextUtils.escapeMarkdown(member.getUser().getName());
        String guildStr = TextUtils.escapeMarkdown(guild.getName()) +
                "\n" + guild.getIdLong();
        String channelStr = TextUtils.escapeMarkdown(tc.getName()) +
                "\n" + tc.getIdLong();
        eb.addField("User", userStr, true);
        eb.addField("Guild", guildStr, true);
        eb.addField("Channel", channelStr, true);

        List<Role> roles = new ArrayList<>(member.getRoles());
        roles.add(guild.getPublicRole());

        for (Role role : roles) {
            StringBuilder roleField = new StringBuilder();
            if (!role.getPermissions().isEmpty()) {
                for (Permission permission : role.getPermissions()) {
                    roleField.append("+ ").append(permission.name()).append("\n");
                }
                eb.addField("Level: Server, Role: " + role.getName(), TextUtils.asCodeBlock(roleField.toString(), "diff"), false);
            }
        }

        Category category = tc.getParent();
        if (category == null) {
            eb.addField("No parent category", "", false);
        } else {
            for (Role role : roles) {
                formatPermissionOverride(eb, category.getPermissionOverride(role));
            }
            formatPermissionOverride(eb, category.getPermissionOverride(member));
        }

        for (Role role : roles) {
            formatPermissionOverride(eb, tc.getPermissionOverride(role));
        }
        formatPermissionOverride(eb, tc.getPermissionOverride(member));

        context.reply(eb.build());
    }

    //add formatted information about a permission override to the embed builder, if there is any
    private static void formatPermissionOverride(@Nonnull EmbedBuilder eb, @Nullable PermissionOverride po) {
        if (po == null) {
            return;
        }
        if (po.getAllowed().isEmpty() && po.getDenied().isEmpty()) {
            return;
        }

        String name = "Level: " + (po.getChannel().getType() == ChannelType.CATEGORY ? "Category" : "Channel");
        name += ", ";
        Role role = po.getRole();
        if (role != null) {
            name += "Role: " + role.getName();
        } else {
            name += "Self Member";
        }

        eb.addField(name, permOverrideAsDiffMarkdown(po), false);
    }

    private static String permOverrideAsDiffMarkdown(@Nonnull PermissionOverride override) {
        StringBuilder sb = new StringBuilder();
        for (Permission permission : override.getAllowed()) {
            sb.append("+ ").append(permission.name()).append("\n");
        }
        for (Permission permission : override.getDenied()) {
            sb.append("- ").append(permission.name()).append("\n");
        }
        return TextUtils.asCodeBlock(sb.toString(), "diff");
    }
}
