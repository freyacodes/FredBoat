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

import fredboat.commandmeta.MessagingException;
import fredboat.util.DiscordUtil;
import fredboat.util.rest.CacheUtil;

import java.util.List;

/**
 * Created by napster on 19.02.18.
 * <p>
 * All of these are documented in depth in the fredboat.yaml.example file (to help selfhosters)
 */
public interface Credentials {

    /**
     * @return Discord bot token
     */
    String getBotToken();

    /**
     * @return Youtube API enabled google keys
     */
    List<String> getGoogleKeys();

    /**
     * @return a random Youtube API enabled google key
     */
    default String getRandomGoogleKey() {
        List<String> googleKeys = getGoogleKeys();
        if (googleKeys.isEmpty()) {
            throw new MessagingException("No Youtube API key detected. Please read the documentation of the fredboat.yaml file on how to obtain one.");
        }
        return googleKeys.get((int) Math.floor(Math.random() * getGoogleKeys().size()));
    }

    /**
     * @return MyAnimeList username
     */
    String getMalUser();

    /**
     * @return MyAnimeList password
     */
    String getMalPassword();

    /**
     * @return imgur client id
     */
    String getImgurClientId();

    /**
     * @return spotify client id
     */
    String getSpotifyId();

    /**
     * @return spotify client secret
     */
    String getSpotifySecret();

    /**
     * @return open weather api key
     */
    String getOpenWeatherKey();

    /**
     * @return the sentry dsn
     */
    String getSentryDsn();


    default int getRecommendedShardCount() {
        return CacheUtil.getUncheckedUnwrapped(DiscordUtil.shardCount, getBotToken());
    }

    // ********************************************************************************
    //                       Undocumented values
    // ********************************************************************************

    String getCarbonKey();

    String getDikeUrl();
}
