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

import fredboat.Config;
import fredboat.command.util.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.IModerationCommand;
import fredboat.feature.I18n;
import fredboat.util.ArgumentUtil;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.utils.PermissionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.List;

public class SoftbanCommand extends Command implements IModerationCommand {

    private static final Logger log = LoggerFactory.getLogger(SoftbanCommand.class);

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        //Ensure we have a search term
        if(args.length == 1){
            String command = args[0].substring(Config.CONFIG.getPrefix().length());
            HelpCommand.sendFormattedCommandHelp(guild, channel, invoker, command);
            return;
        }

        Member target = ArgumentUtil.checkSingleFuzzySearchResult(channel, args[1]);

        if (target == null) return;

        if (!checkAuthorization(channel, invoker, target)) return;

        target.getGuild().getController().ban(target, 7).queue(
                aVoid -> {
                    target.getGuild().getController().unban(target.getUser()).queue();
                    TextUtils.replyWithName(channel, invoker, MessageFormat.format(I18n.get(guild).getString("softbanSuccess"), target.getUser().getName(), target.getUser().getDiscriminator(), target.getUser().getId()));
                },
                throwable -> log.error(MessageFormat.format(I18n.get(guild).getString("softbanFail"), target.getUser()))
        );
    }

    private boolean checkAuthorization(TextChannel channel, Member mod, Member target) {
        if(mod == target) {
            TextUtils.replyWithName(channel, mod, I18n.get(channel.getGuild()).getString("softbanFailSelf"));
            return false;
        }

        if(target.isOwner()) {
            TextUtils.replyWithName(channel, mod, I18n.get(channel.getGuild()).getString("softbanFailOwner"));
            return false;
        }

        if(target == target.getGuild().getSelfMember()) {
            TextUtils.replyWithName(channel, mod, I18n.get(channel.getGuild()).getString("softbanFailMyself"));
            return false;
        }

        if(!PermissionUtil.checkPermission(mod.getGuild(), mod, Permission.BAN_MEMBERS, Permission.KICK_MEMBERS) && !mod.isOwner()) {
            TextUtils.replyWithName(channel, mod, I18n.get(channel.getGuild()).getString("softbanFailUserPerms"));
            return false;
        }

        if(getHighestRolePosition(mod) <= getHighestRolePosition(target) && !mod.isOwner()) {
            TextUtils.replyWithName(channel, mod, MessageFormat.format(I18n.get(channel.getGuild()).getString("softbanFailUserHierachy"), target.getEffectiveName()));
            return false;
        }

        if(!PermissionUtil.checkPermission(mod.getGuild(), mod.getGuild().getSelfMember(), Permission.BAN_MEMBERS)) {
            TextUtils.replyWithName(channel, mod, I18n.get(channel.getGuild()).getString("softbanBotPerms"));
            return false;
        }

        if(getHighestRolePosition(mod.getGuild().getSelfMember()) <= getHighestRolePosition(target)) {
            TextUtils.replyWithName(channel, mod, MessageFormat.format(I18n.get(channel.getGuild()).getString("softbanFailBotHierachy"), target.getEffectiveName()));
            return false;
        }

        return true;
    }

    private int getHighestRolePosition(Member member) {
        List<Role> roles = member.getRoles();

        if(roles.isEmpty()) return -1;

        Role top = roles.get(0);

        for (Role r : roles){
            if(r.getPosition() > top.getPosition()){
                top = r;
            }
        }

        return top.getPosition();
    }

    @Override
    public String help(Guild guild) {
        String usage = "{0}{1} <user>\n#";
        return usage + I18n.get(guild).getString("helpSoftbanCommand");
    }
}
