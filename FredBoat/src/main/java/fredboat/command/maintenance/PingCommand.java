package fredboat.command.maintenance;

import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.IMaintenanceCommand;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;

import static fredboat.util.BotConstants.FREDBOAT_COLOR;

/**
 * Created by epcs on 6/30/2017.
 * Good enough of an indicator to Discord.
 */

public class PingCommand extends Command implements IMaintenanceCommand {
    @Override
    public String help(Guild guild) {
        return null;
    }
    
    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {

        JDA jda = guild.getJDA();
        long ping = jda.getPing();
        String pingMilliSeconds = String.valueOf(ping) + "ms";

        MessageEmbed embed = new EmbedBuilder()
                .setDescription(pingMilliSeconds)
                .setColor(FREDBOAT_COLOR)
                .setFooter(channel.getJDA().getSelfUser().getName(), channel.getJDA().getSelfUser().getAvatarUrl())
                .build();

        channel.sendMessage(embed).queue();
    }
}

//hello
//this is a comment
//I want pats
//multiple pats
//pats never seen before
