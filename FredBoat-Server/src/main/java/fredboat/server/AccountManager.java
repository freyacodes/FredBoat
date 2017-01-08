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

package fredboat.server;

import fredboat.common.db.DatabaseManager;
import fredboat.common.db.entities.UConfig;
import fredboat.common.Crypto;
import fredboat.common.db.entities.UserSession;
import fredboat.util.DiscordApiUtil;
import fredboat.server.webentity.WebUser;
import java.io.IOException;
import java.net.URI;
import org.dmfs.httpessentials.client.HttpRequestExecutor;
import org.dmfs.httpessentials.exceptions.ProtocolError;
import org.dmfs.httpessentials.exceptions.ProtocolException;
import org.dmfs.httpessentials.httpurlconnection.HttpUrlConnectionExecutor;
import org.dmfs.oauth2.client.BasicOAuth2AuthorizationProvider;
import org.dmfs.oauth2.client.BasicOAuth2Client;
import org.dmfs.oauth2.client.BasicOAuth2ClientCredentials;
import org.dmfs.oauth2.client.OAuth2AccessToken;
import org.dmfs.oauth2.client.OAuth2AuthorizationProvider;
import org.dmfs.oauth2.client.OAuth2Client;
import org.dmfs.oauth2.client.OAuth2ClientCredentials;
import org.dmfs.oauth2.client.grants.ClientCredentialsGrant;
import org.dmfs.oauth2.client.scope.BasicScope;
import org.dmfs.rfc5545.Duration;
import org.slf4j.LoggerFactory;

public class AccountManager {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(AccountManager.class);
    private static OAuth2Client oauth = null;
    private static HttpRequestExecutor executor = null;

    public static void init(String clientId, String secret) {
        executor = new HttpUrlConnectionExecutor();

        // Create OAuth2 provider
        OAuth2AuthorizationProvider provider = new BasicOAuth2AuthorizationProvider(
                URI.create("https://discordapp.com/api/oauth2/authorize"),
                URI.create("https://discordapp.com/api/oauth2/token"),
                new Duration(1, 0, 3600) /* default expiration time in case the server doesn't return any */);

        OAuth2ClientCredentials credentials = new BasicOAuth2ClientCredentials(
                clientId, secret);

        oauth = new BasicOAuth2Client(
                provider,
                credentials,
                URI.create("http://localhost") /* Redirect URL, unused */);
    }

    public static CallbackResult handleCallback(String code) {
        try {
            // Request access token using a Client Credentials Grant
            OAuth2AccessToken token = new ClientCredentialsGrant(oauth, new BasicScope("scope")).accessToken(executor);
            if (!token.scope().hasToken("guild") || !token.scope().hasToken("identify")) {
                log.warn("Got invalid OAuth2 scopes.");
                return null;
            }

            WebUser user = DiscordApiUtil.getCurrentUser(token.accessToken());
            String unhashed = Crypto.generateRandomString(32);
            String hash = Crypto.hash(unhashed);

            UConfig uconfig = DatabaseManager.getUConfig(user.getId());

            uconfig = uconfig == null ? new UConfig() : uconfig;

            uconfig = uconfig
                    .setBearer(token.accessToken())
                    .setBearerExpiration(token.expiriationDate().getTimestamp())
                    .setRefresh(token.refreshToken())
                    .setUserId(user.getId());

            user.setConfig(uconfig);
            UserSession session = new UserSession(uconfig.getUserId(), hash);

            //Save to database
            DatabaseManager.mergeUserConfig(uconfig, true);
            SessionManager.mergeSession(session);

            return new CallbackResult(user, session, unhashed);
        } catch (IOException | ProtocolError | ProtocolException ex) {
            throw new RuntimeException("Failed oauth access token grant", ex);
        }
    }

    public static class CallbackResult {

        private final WebUser user;
        private final UserSession session;
        private final String unhashedToken;

        private CallbackResult(WebUser user, UserSession session, String unhashedToken) {
            this.user = user;
            this.session = session;
            this.unhashedToken = unhashedToken;
        }

        public UserSession getSession() {
            return session;
        }

        public WebUser getUser() {
            return user;
        }

        public String getUnhashedToken() {
            return unhashedToken;
        }

    }
}
