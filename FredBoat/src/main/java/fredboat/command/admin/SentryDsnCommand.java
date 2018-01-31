/*
 *
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
 */

package fredboat.command.admin;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.filter.ThresholdFilter;
import fredboat.command.info.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import fredboat.util.GitRepoState;
import io.sentry.Sentry;
import io.sentry.logback.SentryAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * Created by napster on 07.09.17.
 * <p>
 * Override the DSN for sentry. Pass stop or clear to turn it off.
 */
public class SentryDsnCommand extends Command implements ICommandRestricted {

    private static final Logger log = LoggerFactory.getLogger(SentryDsnCommand.class);
    private static final String SENTRY_APPENDER_NAME = "SENTRY";

    public SentryDsnCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        if (!context.hasArguments()) {
            HelpCommand.sendFormattedCommandHelp(context);
            return;
        }
        String dsn = context.rawArgs;

        if (dsn.equalsIgnoreCase("stop") || dsn.equalsIgnoreCase("clear")) {
            turnOff();
            context.replyWithName("Sentry service has been stopped");
        } else {
            turnOn(dsn);
            context.replyWithName("New Sentry DSN has been set!");
        }
    }

    public static void turnOn(String dsn) {
        log.info("Turning on sentry");
        Sentry.init(dsn).setRelease(GitRepoState.getGitRepositoryState().commitId);
        getSentryLogbackAppender().start();
    }

    public static void turnOff() {
        log.info("Turning off sentry");
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

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} <sentry DSN> OR {0}{1} stop\n#Set a temporary sentry DSN overriding the one from the config until" +
                " the next restart, or stop the sentry service.";
    }

    @Nonnull
    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.BOT_ADMIN;
    }
}
