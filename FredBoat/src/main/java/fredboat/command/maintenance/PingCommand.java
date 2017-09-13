package fredboat.command.maintenance;

import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IMaintenanceCommand;
import net.dv8tion.jda.core.entities.Guild;

/**
 * Created by epcs on 6/30/2017.
 * Good enough of an indicator of the ping to Discord.
 */

public class PingCommand extends Command implements IMaintenanceCommand {
    @Override
    public String help(Guild guild) {
        return "{0}{1}\n#Returns the ping to Discord.";  //TODO: i18n
    }
    
    @Override
    public void onInvoke(CommandContext context) {
        long ping = context.guild.getJDA().getPing();
        context.reply(ping + "ms");
    }
}

//hello
//this is a comment
//I want pats
//multiple pats
//pats never seen before
