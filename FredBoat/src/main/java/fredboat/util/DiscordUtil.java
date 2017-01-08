/*
 * MIT License
 *
 * Copyright (c) 2016 Frederik Ar. Mikkelsen
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
import fredboat.FredBoat;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.requests.*;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DiscordUtil {

    private DiscordUtil() {
    }

    public static boolean isMainBot() {
        return (FredBoat.getScopes() & 0x100) != 0;
    }

    public static boolean isMusicBot() {
        return (FredBoat.getScopes() & 0x010) != 0;
    }

    public static boolean isSelfBot() {
        return (FredBoat.getScopes() & 0x001) != 0;
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
        List<Role> roles = guild.getMember(user).getRoles();

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
                .header("User-agent", "FredBoat DiscordBot (https://github.com/Frederikam/FredBoat, " + JDAInfo.VERSION + ")")
                .asJson()
                .getBody()
                .getObject()
                .getInt("shards");
    }

}
