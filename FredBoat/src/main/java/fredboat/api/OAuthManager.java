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

package fredboat.api;

import com.mashape.unirest.http.exceptions.UnirestException;
import fredboat.FredBoat;
import fredboat.db.EntityReader;
import fredboat.db.EntityWriter;
import fredboat.db.entity.UConfig;
import fredboat.util.DiscordUtil;
import net.dv8tion.jda.core.entities.User;
import org.slf4j.LoggerFactory;

@Deprecated //this hasn't been used for a long time, take care
public class OAuthManager {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(OAuthManager.class);

    private static OAuth2Client oauth = null;

    public static void start(String botToken, String secret) throws UnirestException {
        // Create OAuth2 provider
        oauth = new OAuth2Client(
                DiscordUtil.getApplicationInfo(botToken).getString("id"),
                secret,
                "https://discordapp.com/api/oauth2/token",
                "http://localhost:1337/callback"
        );
    }

    static UConfig handleCallback(String code) {
        try {
            // Request access token using a Client Credentials Grant
            TokenGrant token = oauth.grantToken(code);
            if (!token.getScope().contains("guild")
                    || !token.getScope().contains("identify")) {
                throw new RuntimeException("Got invalid OAuth2 scopes.");
            }

            return saveTokenToConfig(token);
        } catch (UnirestException ex) {
            throw new RuntimeException("Failed oauth access token grant", ex);
        }
    }

    public static UConfig ensureUnexpiredBearer(UConfig config) {
        long cur = System.currentTimeMillis() / 1000;

        try {
            if(cur + 60 > config.getBearerExpiration()){
                //Will soon expire if it hasn't already
                TokenGrant token = oauth.refreshToken(config.getRefresh());

                config = saveTokenToConfig(token);
            }
        } catch (UnirestException e) {
            throw new RuntimeException(e);
        }

        return config;
    }

    private static UConfig saveTokenToConfig(TokenGrant token){
        //NOTE: the returned user might be fake, which means opening a private channel might throw an exception
        User user = DiscordUtil.getUserFromBearer(FredBoat.getFirstJDA(), token.getBearer());

        UConfig uconfig = EntityReader.getUConfig(user.getId());

        uconfig = uconfig == null ? new UConfig() : uconfig;

        uconfig.setBearer(token.getBearer())
                .setBearerExpiration(token.getExpirationTime())
                .setRefresh(token.getRefresh())
                .setUserId(user.getId());

        //Save to database
        EntityWriter.mergeUConfig(uconfig);

        return uconfig;
    }

}
