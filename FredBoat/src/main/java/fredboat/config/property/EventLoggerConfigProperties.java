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
@ConfigurationProperties(prefix = "event-logger")
public class EventLoggerConfigProperties implements EventLoggerConfig {

    private String eventLogWebhook = "";
    private int eventLogInterval = 1;
    private String guildStatsWebhook = "";
    private int guildStatsInterval = 60;


    @Override
    public String getEventLogWebhook() {
        return eventLogWebhook;
    }

    @Override
    public int getEventLogInterval() {
        return eventLogInterval;
    }

    @Override
    public String getGuildStatsWebhook() {
        return guildStatsWebhook;
    }

    @Override
    public int getGuildStatsInterval() {
        return guildStatsInterval;
    }

    public void setEventLogWebhook(String eventLogWebhook) {
        this.eventLogWebhook = eventLogWebhook;
    }

    public void setEventLogInterval(int eventLogInterval) {
        this.eventLogInterval = eventLogInterval;
    }

    public void setGuildStatsWebhook(String guildStatsWebhook) {
        this.guildStatsWebhook = guildStatsWebhook;
    }

    public void setGuildStatsInterval(int guildStatsInterval) {
        this.guildStatsInterval = guildStatsInterval;
    }
}
