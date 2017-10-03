package fredboat.command.maintenance;

import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IMaintenanceCommand;
import fredboat.messaging.internal.Context;

import javax.annotation.Nonnull;

/**
 * Created by epcs on 6/30/2017.
 * Good enough of an indicator of the ping to Discord.
 */

public class PingCommand extends Command implements IMaintenanceCommand {
    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1}\n#Return the ping to Discord.";
    }
    
    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        long ping = context.guild.getJDA().getPing();
        context.reply(ping + "ms");
    }
}

//hello
//this is a comment
//I want pats
//multiple pats
//pats never seen before
