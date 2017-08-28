package fredboat.command.moderation;

import fredboat.FredBoat;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IModerationCommand;
import fredboat.perms.PermissionLevel;
import fredboat.perms.PermsUtil;
import fredboat.util.ArgumentUtil;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.utils.PermissionUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class PruneCommand extends Command implements IModerationCommand, ICommandRestricted {
    private int THRESHOLD_NUMBER = 100; // >100 is not good. because discord
    private int DEFAULT_NUMBER = 50; // half of it.
    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        if(!invoker.hasPermission(channel, Permission.MESSAGE_MANAGE) && !PermsUtil.isUserBotOwner(invoker.getUser())) {
            return; // Temp
        }

        if(args.length == 1) {
            return;
        } else if(args.length == 2 && StringUtils.isNumeric(args[1])) { // ;;prune <num>
            int num = Integer.parseInt(args[1]);
            channel.sendMessage(String.valueOf(num)).queue();
            if(num < THRESHOLD_NUMBER) {
                try {
                    if(guild.getSelfMember().hasPermission(Permission.MESSAGE_HISTORY)) {
                        List<Message> messages = channel.getHistory().retrievePast(num).complete(true);
                        channel.deleteMessages(messages).queue();
                        return;
                    }
                    return;
                    //channel.sendMessage(String.valueOf(messages)).queue();
                } catch (RateLimitedException e) {
                    e.printStackTrace();
                }
            }
        } else if(args.length == 2) {
            Member m = ArgumentUtil.checkSingleFuzzyMemberSearchResult(channel,args[1]); // ;;prune <user>
            if(guild.getSelfMember().hasPermission(Permission.MESSAGE_HISTORY)) {
                List<Message> messages = null;
                List<Message> todelete = new ArrayList<>();
                try {
                    messages = channel.getHistory().retrievePast(DEFAULT_NUMBER).complete(true);
                    for(Message mes:messages){
                        if(mes.getAuthor().equals(m.getUser())){todelete.add(mes);}
                    }
                    channel.sendMessage(String.valueOf(todelete.size())).queue();
                    channel.deleteMessages(todelete).queue();
                } catch (RateLimitedException e) {
                    e.printStackTrace();
                }
                return;
            }
            return;
        } else if(args.length == 3 && StringUtils.isNumeric(args[2])) { // ;;prune <user> <num>
            return;
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
