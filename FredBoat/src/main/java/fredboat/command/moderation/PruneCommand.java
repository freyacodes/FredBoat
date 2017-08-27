package fredboat.command.moderation;

import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IModerationCommand;
import fredboat.perms.PermissionLevel;
import fredboat.perms.PermsUtil;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

public class PruneCommand extends Command implements IModerationCommand, ICommandRestricted {
    private int THRESHOLD_NUMBER = 100; // >100 is not good. because discord
    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        if(!invoker.hasPermission(channel, Permission.MESSAGE_MANAGE) || !PermsUtil.isUserBotOwner(invoker.getUser())) {
            return; // Temp
        }

    }

    @Override
    public String help(Guild guild) {
        return null;
    }

    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.ADMIN;
    }
}
