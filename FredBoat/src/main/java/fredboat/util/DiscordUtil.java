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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.feature.I18n;
import fredboat.feature.metrics.Metrics;
import fredboat.main.BotController;
import fredboat.main.Config;
import fredboat.shared.constant.BotConstants;
import fredboat.util.rest.CacheUtil;
import fredboat.util.rest.Http;
import net.dv8tion.jda.bot.entities.ApplicationInfo;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
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
    private static final String USER_AGENT = String.format("DiscordBot (%s, %s)",
            BotConstants.GITHUB_URL, AppInfo.getAppInfo().getVersionBuild());

    private static volatile DiscordAppInfo selfDiscordAppInfo; //access this object through getApplicationInfo(jda)
    private static final Object selfDiscordAppInfoLock = new Object();

    private DiscordUtil() {
    }

    public static long getOwnerId(@Nonnull JDA jda) {
        return getApplicationInfo(jda).ownerIdLong;
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
    public static DiscordAppInfo getApplicationInfo(@Nonnull JDA jda) {
        //double checked lock pattern
        DiscordAppInfo info = selfDiscordAppInfo;
        if (info == null) {
            synchronized (selfDiscordAppInfoLock) {
                info = selfDiscordAppInfo;
                if (info == null) {
                    //todo this method can be improved by reloading the info regularly. possibly some async loading guava cache?
                    selfDiscordAppInfo = info = new DiscordAppInfo(jda.asBot().getApplicationInfo().complete());
                    Metrics.successfulRestActions.labels("getApplicationInfo").inc();
                }
            }
        }
        return info;
    }

    //token <-> botid
    @Nonnull
    public static final LoadingCache<String, Long> BOT_ID = CacheBuilder.newBuilder()
            .build(CacheLoader.asyncReloading(CacheLoader.from(DiscordUtil::getUserId), BotController.INS.getExecutor()));


    //uses our configured bot token to retrieve our own userid
    public static long getBotId() {
        return CacheUtil.getUncheckedUnwrapped(BOT_ID, Config.CONFIG.getBotToken());
    }

    private static long getUserId(@Nonnull String token) {
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
            throw new RuntimeException("Failed to retrieve my own userId from Discord, the result is empty");
        }

        long botId;
        try {
            botId = Long.parseUnsignedLong(result);
        } catch (NumberFormatException e) {
            //logging the error and rethrowing a new one, because it might expose information that we dont want users to see
            log.error("Failed to retrieve my own userId from Discord", e);
            throw new RuntimeException("Failed to retrieve my own userId from Discord, see error log for more information.");
        }

        return botId;
    }

    /**
     * @return true if this bot account is an "official" fredboat (music, patron, CE, etc).
     * This is useful to lock down features that we only need internally, like polling the docker hub for pull stats.
     */
    public static boolean isOfficialBot() {
        long botId = getBotId();
        return botId == BotConstants.MUSIC_BOT_ID
                || botId == BotConstants.PATRON_BOT_ID
                || botId == BotConstants.CUTTING_EDGE_BOT_ID
                || botId == BotConstants.BETA_BOT_ID
                || botId == BotConstants.MAIN_BOT_ID;
    }

    // ########## Moderation related helper functions
    public static String getReasonForModAction(CommandContext context) {
        String r = null;
        if (context.args.length > 1) { //ignore the first arg which contains the name/mention of the user
            r = String.join(" ", Arrays.copyOfRange(context.args, 1, context.args.length));
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


    //like JDAs ApplicationInfo but without any references to JDA objects to prevent leaks
    //use this to cache the app info
    public static class DiscordAppInfo {
        public final boolean doesBotRequireCodeGrant;
        public final boolean isBotPublic;
        //public final long botIdLong;
        //public final String botId;
        public final String iconId;
        public final String description;
        public final String appName;
        public final long ownerIdLong;
        public final String ownerId;
        public final String ownerName;

        public DiscordAppInfo(ApplicationInfo applicationInfo) {
            this.doesBotRequireCodeGrant = applicationInfo.doesBotRequireCodeGrant();
            this.isBotPublic = applicationInfo.isBotPublic();
            //for old accounts, like the public FredBoat♪♪ one, this does not return the public bot id that one gets
            // when rightclick -> copy Id or mentioning, but a different one, an application id. due to risks of
            // introducing bugs on the public boat when using this (as happened with the mention prefix) it has been
            // commented out and shall stay this way as a warning to not use it. Usually the JDA#getSelfUser() method is
            // accessible to gain access to our own bot id, otherwise use DiscordUtil.getDefaultBotId()
            //this.botIdLong = applicationInfo.getIdLong();
            //this.botId = applicationInfo.getId();
            this.iconId = applicationInfo.getIconId();
            this.description = applicationInfo.getDescription();
            this.appName = applicationInfo.getName();
            this.ownerIdLong = applicationInfo.getOwner().getIdLong();
            this.ownerId = applicationInfo.getOwner().getId();
            this.ownerName = applicationInfo.getOwner().getName();
        }
    }
}
