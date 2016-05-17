package fredboat.command.fun;

import fredboat.commandmeta.Command;
import fredboat.commandmeta.ICommand;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;

public class RiotCommand extends Command implements ICommand {

    @Override
    public void onInvoke(Guild guild, TextChannel channel, User invoker, Message message, String[] args) {
        channel.sendMessage("ヽ༼ຈل͜ຈ༽ﾉ **" + message.getContent().replaceFirst("\\S*\\s+", "").toUpperCase()+"** ヽ༼ຈل͜ຈ༽ﾉ");
    }
}
