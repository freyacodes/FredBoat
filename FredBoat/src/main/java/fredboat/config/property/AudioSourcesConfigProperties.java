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

/**
 * Created by napster on 03.03.18.
 */
@Component
@ConfigurationProperties(prefix = "audio-sources")
public class AudioSourcesConfigProperties implements AudioSourcesConfig {

    // audio managers
    private boolean youtubeEnabled = true;
    private boolean soundcloudEnabled = true;
    private boolean bandcampEnabled = true;
    private boolean twitchEnabled = true;
    private boolean vimeoEnabled = true;
    private boolean mixerEnabled = true;
    private boolean spotifyEnabled = true;
    private boolean localEnabled = false;
    private boolean httpEnabled = false;

    @Override
    public boolean isYouTubeEnabled() {
        return youtubeEnabled;
    }

    @Override
    public boolean isSoundCloudEnabled() {
        return soundcloudEnabled;
    }

    @Override
    public boolean isBandCampEnabled() {
        return bandcampEnabled;
    }

    @Override
    public boolean isTwitchEnabled() {
        return twitchEnabled;
    }

    @Override
    public boolean isVimeoEnabled() {
        return vimeoEnabled;
    }

    @Override
    public boolean isMixerEnabled() {
        return mixerEnabled;
    }

    @Override
    public boolean isSpotifyEnabled() {
        return spotifyEnabled;
    }

    @Override
    public boolean isLocalEnabled() {
        return localEnabled;
    }

    @Override
    public boolean isHttpEnabled() {
        return httpEnabled;
    }

    public void setEnableYoutube(boolean youtubeEnabled) {
        this.youtubeEnabled = youtubeEnabled;
    }

    public void setEnableSoundcloud(boolean soundcloudEnabled) {
        this.soundcloudEnabled = soundcloudEnabled;
    }

    public void setEnableBandcamp(boolean bandcampEnabled) {
        this.bandcampEnabled = bandcampEnabled;
    }

    public void setEnableTwitch(boolean twitchEnabled) {
        this.twitchEnabled = twitchEnabled;
    }

    public void setEnableVimeo(boolean vimeoEnabled) {
        this.vimeoEnabled = vimeoEnabled;
    }

    public void setEnableMixer(boolean mixerEnabled) {
        this.mixerEnabled = mixerEnabled;
    }

    public void setEnableSpotify(boolean spotifyEnabled) {
        this.spotifyEnabled = spotifyEnabled;
    }

    public void setEnableLocal(boolean localEnabled) {
        this.localEnabled = localEnabled;
    }

    public void setEnableHttp(boolean httpEnabled) {
        this.httpEnabled = httpEnabled;
    }
}
