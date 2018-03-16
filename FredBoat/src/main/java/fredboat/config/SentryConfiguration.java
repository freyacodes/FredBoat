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

package fredboat.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.filter.ThresholdFilter;
import fredboat.config.property.Credentials;
import fredboat.util.GitRepoState;
import io.sentry.Sentry;
import io.sentry.SentryClient;
import io.sentry.logback.SentryAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Created by napster on 09.03.18.
 */
@Configuration
public class SentryConfiguration {

    //these mdc tags need to be added to the sentry client, so they end up as additional selectable/searchable
    // tags in their web dashboard
    public static final String SENTRY_MDC_TAG_GUILD = "guild";
    public static final String SENTRY_MDC_TAG_INVOKER = "invoker";
    public static final String SENTRY_MDC_TAG_CHANNEL = "channel";

    private static final Logger log = LoggerFactory.getLogger(SentryConfiguration.class);
    private static final String SENTRY_APPENDER_NAME = "SENTRY";

    public SentryConfiguration(Credentials credentials) {
        String sentryDsn = credentials.getSentryDsn();
        if (!sentryDsn.isEmpty()) {
            turnOn(sentryDsn);
        } else {
            turnOff();
        }
    }


    public void turnOn(String dsn) {
        log.info("Turning on sentry");
        SentryClient sentryClient = Sentry.init(dsn);
        sentryClient.setRelease(GitRepoState.getGitRepositoryState().commitId);
        sentryClient.addMdcTag(SENTRY_MDC_TAG_GUILD);
        sentryClient.addMdcTag(SENTRY_MDC_TAG_INVOKER);
        sentryClient.addMdcTag(SENTRY_MDC_TAG_CHANNEL);

        getSentryLogbackAppender().start();
    }

    public void turnOff() {
        log.warn("Turning off sentry");
        Sentry.close();
        getSentryLogbackAppender().stop();
    }

    //programmatically creates a sentry appender
    private static synchronized SentryAppender getSentryLogbackAppender() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

        SentryAppender sentryAppender = (SentryAppender) root.getAppender(SENTRY_APPENDER_NAME);
        if (sentryAppender == null) {
            sentryAppender = new SentryAppender();
            sentryAppender.setName(SENTRY_APPENDER_NAME);

            ThresholdFilter warningsOrAboveFilter = new ThresholdFilter();
            warningsOrAboveFilter.setLevel(Level.WARN.levelStr);
            warningsOrAboveFilter.start();
            sentryAppender.addFilter(warningsOrAboveFilter);

            sentryAppender.setContext(loggerContext);
            root.addAppender(sentryAppender);
        }
        return sentryAppender;
    }

}
