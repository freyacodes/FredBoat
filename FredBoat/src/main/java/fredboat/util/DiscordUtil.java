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

package fredboat.util;

import fredboat.commandmeta.abs.CommandContext;
import fredboat.feature.I18n;
import fredboat.shared.constant.BotConstants;
import fredboat.util.rest.Http;
import net.dv8tion.jda.bot.entities.ApplicationInfo;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.requests.Requester;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

public class DiscordUtil {

    private static final Logger log = LoggerFactory.getLogger(DiscordUtil.class);
    private static final String USER_AGENT = "FredBoat DiscordBot (https://github.com/Frederikam/FredBoat, 1.0)";

    private static volatile ApplicationInfo discordAppInfo; //access this object through getApplicationInfo(jda)
    private static final Object discordAppInfoLock = new Object();

    private DiscordUtil() {
    }
    
    public static String getOwnerId(JDA jda) {
        return getApplicationInfo(jda).getOwner().getId();
    }

    public static boolean isMainBotPresent(Guild guild) {
        JDA jda = guild.getJDA();
        User other = jda.getUserById(BotConstants.MAIN_BOT_ID);
        return other != null && guild.getMember(other) != null;
    }

    public static boolean isMusicBotPresent(Guild guild) {
        JDA jda = guild.getJDA();
        User other = jda.getUserById(BotConstants.MUSIC_BOT_ID);
        return other != null && guild.getMember(other) != null;
    }

    public static boolean isPatronBotPresentAndOnline(Guild guild) {
        JDA jda = guild.getJDA();
        User other = jda.getUserById(BotConstants.PATRON_BOT_ID);
        return other != null && guild.getMember(other) != null && guild.getMember(other).getOnlineStatus() == OnlineStatus.ONLINE;
    }

    public static int getHighestRolePosition(Member member) {
        List<Role> roles = member.getRoles();

        if (roles.isEmpty()) return -1;

        Role top = roles.get(0);

        for (Role r : roles) {
            if (r.getPosition() > top.getPosition()) {
                top = r;
            }
        }

        return top.getPosition();
    }

    public static int getRecommendedShardCount(@Nonnull String token) throws IOException, JSONException {
        Http.SimpleRequest request = Http.get(Requester.DISCORD_API_PREFIX + "gateway/bot")
                .auth("Bot " + token)
                .header("User-agent", USER_AGENT);

        try (Response response = request.execute()) {
            if (response.code() == 401) {
                throw new IllegalArgumentException("Invalid discord bot token provided!");
            } else if (!response.isSuccessful()) {
                log.error("Unexpected response from discord: {} {}", response.code(), response.toString());
            }
            //noinspection ConstantConditions
            return new JSONObject(response.body().string()).getInt("shards");
        }
    }

    @Nonnull
    public static ApplicationInfo getApplicationInfo(@Nonnull JDA jda) {
        //double checked lock pattern
        ApplicationInfo info = discordAppInfo;
        if (info == null) {
            synchronized (discordAppInfoLock) {
                info = discordAppInfo;
                if (info == null) {
                    discordAppInfo = info = jda.asBot().getApplicationInfo().complete();
                }
            }
        }
        return info;
    }

    @Nonnull
    public static String getUserId(@Nonnull String token) {
        Http.SimpleRequest request = Http.get(Requester.DISCORD_API_PREFIX + "/users/@me")
                .auth("Bot " + token)
                .header("User-agent", USER_AGENT);

        String result = "";
        int attempt = 0;
        while (result.isEmpty() && attempt++ < 5) {
            try {
                result = request.asJson().getString("id");
            } catch (Exception e) {
                log.error("Could not request my own userId from Discord, will retry a few times", e);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        if (result.isEmpty()) {
            throw new RuntimeException("Failed to retrieve my own userId from Discord");
        }
        return result;
    }

    // ########## Moderation related helper functions
    public static String getReasonForModAction(CommandContext context) {
        String r = null;
        if (context.args.length > 2) {
            r = String.join(" ", Arrays.copyOfRange(context.args, 2, context.args.length));
        }

        return context.i18n("modReason") + ": " + (r != null ? r : "No reason provided.");
    }

    public static String formatReasonForAuditLog(String plainReason, Member invoker) {
        String i18nAuditLogMessage = MessageFormat.format(I18n.get(invoker.getGuild()).getString("modAuditLogMessage"),
                invoker.getEffectiveName(), invoker.getUser().getDiscriminator(), invoker.getUser().getId()) + ", ";
        int auditLogMaxLength = 512 - i18nAuditLogMessage.length(); //512 is a hard limit by discord
        return i18nAuditLogMessage + (plainReason.length() > auditLogMaxLength ?
                plainReason.substring(0, auditLogMaxLength) : plainReason);
    }
}
