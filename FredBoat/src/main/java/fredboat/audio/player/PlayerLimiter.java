package fredboat.audio.player;

import fredboat.commandmeta.abs.CommandContext;
import fredboat.config.property.AppConfig;
import fredboat.shared.constant.BotConstants;
import net.dv8tion.jda.core.entities.Guild;
import org.springframework.stereotype.Component;

@Component
public class PlayerLimiter {

    // A negative limit means unlimited
    private int limit;

    public PlayerLimiter(AppConfig appConfig) {
        limit = appConfig.getPlayerLimit();
    }

    public boolean checkLimit(Guild guild, PlayerRegistry playerRegistry) {
        GuildPlayer guildPlayer = playerRegistry.getExisting(guild);
        //noinspection SimplifiableIfStatement
        if (guildPlayer != null && guildPlayer.getTrackCount() > 0)
            return true;

        return limit < 0
                || playerRegistry.getPlayingPlayers().size() < limit;

    }

    public boolean checkLimitResponsive(CommandContext context, PlayerRegistry playerRegistry) {
        boolean b = checkLimit(context.guild, playerRegistry);

        if (!b) {
            String patronUrl = "<" + BotConstants.DOCS_DONATE_URL + ">";
            String msg = context.i18nFormat("playersLimited", limit, patronUrl);
            context.reply(msg);
        }

        return b;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
