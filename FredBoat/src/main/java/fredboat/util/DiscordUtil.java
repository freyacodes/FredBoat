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

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import fredboat.Config;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.requests.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DiscordUtil {

    private static final Logger log = LoggerFactory.getLogger(DiscordUtil.class);

    private static final String USER_AGENT = "FredBoat DiscordBot (https://github.com/Frederikam/FredBoat, 1.0)";

    private DiscordUtil() {
    }

    public static boolean isMainBot() {
        return isMainBot(Config.CONFIG);
    }

    public static boolean isMusicBot() {
        return isMusicBot(Config.CONFIG);
    }

    public static boolean isSelfBot() {
        return isSelfBot(Config.CONFIG);
    }

    public static boolean isMainBot(Config conf) {
        return (conf.getScope() & 0x100) != 0;
    }

    public static boolean isMusicBot(Config conf) {
        return (conf.getScope() & 0x010) != 0;
    }

    public static boolean isSelfBot(Config conf) {
        return (conf.getScope() & 0x001) != 0;
    }

    public static boolean isUserBotOwner(User user) {
        return getOwnerId(user.getJDA()).equals(user.getId());
    }

    public static String getOwnerId(JDA jda) {
        try {
            return getApplicationInfo(jda.getToken().substring(4)).getJSONObject("owner").getString("id");
        } catch (UnirestException e) {
            throw new RuntimeException(e);
        }
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

    public static boolean isUserBotCommander(Guild guild, User user) {
        Member member = guild.getMember(user);
        if (member == null) return false;
        List<Role> roles = member.getRoles();

        for (Role r : roles) {
            if (r.getName().equals("Bot Commander")) {
                return true;
            }
        }

        return false;
    }

    public static void sendShardlessMessage(String channel, Message msg) {
        sendShardlessMessage(msg.getJDA(), channel, msg.getRawContent());
    }

    public static void sendShardlessMessage(JDA jda, String channel, String content) {
        JSONObject body = new JSONObject();
        body.put("content", content);
        new RestAction<Void>(jda, Route.Messages.SEND_MESSAGE.compile(channel), body) {
            @Override
            protected void handleResponse(Response response, Request request) {
                if (response.isOk())
                    request.onSuccess(null);
                else
                    request.onFailure(response);
            }
        }.queue();
    }

    public static int getRecommendedShardCount(String token) throws UnirestException {
        return Unirest.get(Requester.DISCORD_API_PREFIX + "gateway/bot")
                .header("Authorization", "Bot " + token)
                .header("User-agent", USER_AGENT)
                .asJson()
                .getBody()
                .getObject()
                .getInt("shards");
    }

    public static User getUserFromBearer(JDA jda, String token) {
        try {
            JSONObject user =  Unirest.get(Requester.DISCORD_API_PREFIX + "/users/@me")
                    .header("Authorization", "Bearer " + token)
                    .header("User-agent", USER_AGENT)
                    .asJson()
                    .getBody()
                    .getObject();

            if(user.has("id")){
                return jda.retrieveUserById(user.getString("id")).complete(true);
            }
        } catch (UnirestException | RateLimitedException ignored) {}

        return null;
    }

    // https://discordapp.com/developers/docs/topics/oauth2
    @Deprecated
    public static JSONObject getApplicationInfo(String token) throws UnirestException {
        return Unirest.get(Requester.DISCORD_API_PREFIX + "/oauth2/applications/@me")
                .header("Authorization", "Bot " + token)
                .header("User-agent", USER_AGENT)
                .asJson()
                .getBody()
                .getObject();
    }

}
