/*
 * MIT License
 *
 * Copyright (c) 2017-2018 Frederik Ar. Mikkelsen
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
 */

package fredboat.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by napster on 03.03.18.
 */
@Component
@ConfigurationProperties(prefix = "credentials")
public class CredentialsProperties implements Credentials {

    private String discordBotToken = "";
    private List<String> googleApiKeys = new ArrayList<>();
    private String malUser = "";
    private String malPassword = "";
    private String imgurClientId = "";
    private String spotifyId = "";
    private String spotifySecret = "";
    private String openWeatherKey = "";
    private String sentryDsn = "";
    private String carbonKey = "";
    private String dikeUrl = "";


    @Override
    public String getBotToken() {
        return discordBotToken;
    }

    @Override
    public List<String> getGoogleKeys() {
        return googleApiKeys;
    }

    @Override
    public String getMalUser() {
        return malUser;
    }

    @Override
    public String getMalPassword() {
        return malPassword;
    }

    @Override
    public String getImgurClientId() {
        return imgurClientId;
    }

    @Override
    public String getSpotifyId() {
        return spotifyId;
    }

    @Override
    public String getSpotifySecret() {
        return spotifySecret;
    }

    @Override
    public String getOpenWeatherKey() {
        return openWeatherKey;
    }

    @Override
    public String getSentryDsn() {
        return sentryDsn;
    }

    @Override
    public String getCarbonKey() {
        return carbonKey;
    }

    @Override
    public String getDikeUrl() {
        return dikeUrl;
    }

    public void setDiscordBotToken(String discordBotToken) {
        this.discordBotToken = discordBotToken;
    }

    public void setGoogleApiKeys(List<String> googleApiKeys) {
        this.googleApiKeys = googleApiKeys;
    }

    public void setMalUser(String malUser) {
        this.malUser = malUser;
    }

    public void setMalPassword(String malPassword) {
        this.malPassword = malPassword;
    }

    public void setImgurClientId(String imgurClientId) {
        this.imgurClientId = imgurClientId;
    }

    public void setSpotifyId(String spotifyId) {
        this.spotifyId = spotifyId;
    }

    public void setSpotifySecret(String spotifySecret) {
        this.spotifySecret = spotifySecret;
    }

    public void setOpenWeatherKey(String openWeatherKey) {
        this.openWeatherKey = openWeatherKey;
    }

    public void setSentryDsn(String sentryDsn) {
        this.sentryDsn = sentryDsn;
    }

    public void setCarbonKey(String carbonKey) {
        this.carbonKey = carbonKey;
    }

    public void setDikeUrl(String dikeUrl) {
        this.dikeUrl = dikeUrl;
    }
}
