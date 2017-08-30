package fredboat.command.music.control;

import fredboat.Config;
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.PlayerRegistry;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.feature.I18n;
import fredboat.perms.PermissionLevel;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;

public class NumericSelectTrackCommand extends Command implements IMusicCommand, ICommandRestricted {

    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.USER;
    }

    @Override
    public void onInvoke(Guild guild, TextChannel channel, Member invoker, Message message, String[] args) {
        if (!invoker.getVoiceState().inVoiceChannel()) {
            channel.sendMessage(I18n.get(guild).getString("playerUserNotInChannel")).queue();
            return;
        }

        if (!message.getAttachments().isEmpty()) {
            GuildPlayer player = PlayerRegistry.get(guild);
            player.setCurrentTC(channel);

            for (Message.Attachment atc : message.getAttachments()) {
                player.queue(atc.getUrl(), channel, invoker);
            }

            player.setPause(false);

            return;
        }

        if (args.length == 1 && message.getContent().length() > Config.CONFIG.getPrefix().length()) {
            String contentWithoutPrefix = message.getContent().substring(Config.CONFIG.getPrefix().length());

            if (StringUtils.isNumeric(contentWithoutPrefix)) {
                String[] newArgs = new String[2];
                newArgs[0] = args[0];
                newArgs[1] = contentWithoutPrefix;

                SelectCommand.select(guild, channel, invoker, message, newArgs);
                return;
            }
        }

        //What if we want to select a selection instead?
        if (args.length == 2 && StringUtils.isNumeric(args[1])) {
            SelectCommand.select(guild, channel, invoker, message, args);
            return;
        }

        GuildPlayer player = PlayerRegistry.get(guild);
        player.setCurrentTC(channel);

        player.queue(args[1], channel, invoker);
        player.setPause(false);

        try {
            message.delete().queue();
        } catch (Exception ignored) {

        }
    }

    @Override
    public String help(Guild guild) {
        String usage = "{0}{1} <url> OR {0}{1} <search-term>\n#";
        return usage + I18n.get(guild).getString("helpPlayCommand");
    }
}
