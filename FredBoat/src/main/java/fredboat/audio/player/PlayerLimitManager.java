package fredboat.audio.player;

import fredboat.feature.I18n;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;

import java.text.MessageFormat;

public class PlayerLimitManager {

    // A negative limit means unlimited
    private static int limit = -1;

    public static boolean checkLimit(Guild guild) {
        GuildPlayer guildPlayer = PlayerRegistry.getExisting(guild);
        //noinspection SimplifiableIfStatement
        if (guildPlayer != null && guildPlayer.getSongCount() > 0)
            return true;

        return limit < 0
                || PlayerRegistry.getPlayingPlayers().size() < limit;

    }

    public static boolean checkLimitResponsive(TextChannel channel) {
        boolean b =  checkLimit(channel.getGuild());

        if (!b) {
            String patronUrl = "<https://fredboat.com/docs/donate>";
            String msg = MessageFormat.format(I18n.get(channel.getGuild()).getString("playersLimited"), limit, patronUrl);
            channel.sendMessage(msg).queue();
        }

        return b;
    }

    public static int getLimit() {
        return limit;
    }

    public static void setLimit(int limit) {
        PlayerLimitManager.limit = limit;
    }
}
