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

import org.springframework.stereotype.Component;

/**
 * Created by napster on 03.03.18.
 */
@Component
public class SpringConfigPropertiesProvider implements ConfigPropertiesProvider {

    private final AppConfig appConfig;
    private final AudioSourcesConfig audioSourcesConfig;
    private final Credentials credentials;
    private final DatabaseConfig databaseConfig;
    private final EventLoggerConfig eventLoggerConfig;
    private final LavalinkConfig lavalinkConfig;

    public SpringConfigPropertiesProvider(AppConfig appConfig, AudioSourcesConfig audioSourcesConfig,
                                          Credentials credentials, DatabaseConfig databaseConfig,
                                          EventLoggerConfig eventLoggerConfig, LavalinkConfig lavalinkConfig) {
        this.appConfig = appConfig;
        this.audioSourcesConfig = audioSourcesConfig;
        this.credentials = credentials;
        this.databaseConfig = databaseConfig;
        this.eventLoggerConfig = eventLoggerConfig;
        this.lavalinkConfig = lavalinkConfig;
    }

    @Override
    public AppConfig getAppConfig() {
        return appConfig;
    }

    @Override
    public AudioSourcesConfig getAudioSourcesConfig() {
        return audioSourcesConfig;
    }

    @Override
    public Credentials getCredentials() {
        return credentials;
    }

    @Override
    public DatabaseConfig getDatabaseConfig() {
        return databaseConfig;
    }

    @Override
    public EventLoggerConfig getEventLoggerConfig() {
        return eventLoggerConfig;
    }

    @Override
    public LavalinkConfig getLavalinkConfig() {
        return lavalinkConfig;
    }
}
