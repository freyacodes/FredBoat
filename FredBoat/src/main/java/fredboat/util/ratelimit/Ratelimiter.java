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
 */

package fredboat.util.ratelimit;

import fredboat.audio.queue.PlaylistInfo;
import fredboat.command.info.ShardsCommand;
import fredboat.command.music.control.SkipCommand;
import fredboat.command.music.info.ExportCommand;
import fredboat.command.util.WeatherCommand;
import fredboat.commandmeta.CommandInitializer;
import fredboat.commandmeta.abs.JCommand;
import fredboat.config.property.AppConfig;
import fredboat.db.api.BlacklistRepository;
import fredboat.feature.metrics.Metrics;
import fredboat.messaging.internal.Context;
import fredboat.util.TextUtils;
import io.prometheus.client.guava.cache.CacheMetricsCollector;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Created by napster on 17.04.17.
 * <p>
 * this object should be threadsafe by itself
 * <p>
 * http://i.imgur.com/ha0R3XZ.gif
 */
@Component
public class Ratelimiter {

    private static final int RATE_LIMIT_HITS_BEFORE_BLACKLIST = 10;

    private final List<Ratelimit> ratelimits;
    @Nullable
    private final Blacklist autoBlacklist;

    public Ratelimiter(AppConfig appConfig, ExecutorService executor, BlacklistRepository repository,
                       CacheMetricsCollector cacheMetrics) {
        Set<Long> whitelist = ConcurrentHashMap.newKeySet();

        //only works for those admins who are added with their userId and not through a roleId
        //TODO: Add ownerIds (needs merge with dev)
        whitelist.addAll(appConfig.getAdminIds());

        //Create all the rate limiters we want
        ratelimits = new ArrayList<>();

        if (appConfig.useAutoBlacklist()) {
            autoBlacklist = new Blacklist(repository, whitelist, RATE_LIMIT_HITS_BEFORE_BLACKLIST);
        } else {
            autoBlacklist = null;
        }

        Function<Context, String> defaultUserMessage = context -> context.i18n("ratelimitedCommandsUser");
        Function<Context, String> defaultGuildMessage = context -> context.i18n("ratelimitedCommandsGuild");
        Function<Context, String> skipMessage = context -> context.i18n("ratelimitedCommandsUser") + "\n"
                + context.i18nFormat("ratelimitedSkipCommand",
                "`" + TextUtils.escapeMarkdown(context.getPrefix()) + CommandInitializer.SKIP_COMM_NAME + " n-m`");
        Function<Context, String> playlistMessage = context -> context.i18n("ratelimitedGuildSlowLoadingPlaylist");

        //sort these by harsher limits coming first
        ratelimits.add(new Ratelimit("userShardsComm", cacheMetrics, executor, whitelist, Ratelimit.Scope.USER,
                2, 30000, ShardsCommand.class, defaultUserMessage));
        ratelimits.add(new Ratelimit("userSkipComm", cacheMetrics, executor, whitelist, Ratelimit.Scope.USER,
                5, 20000, SkipCommand.class, skipMessage));
        ratelimits.add(new Ratelimit("userExportComm", cacheMetrics, executor, whitelist, Ratelimit.Scope.USER,
                2, 60000, ExportCommand.class, defaultUserMessage));
        ratelimits.add(new Ratelimit("userAllComms", cacheMetrics, executor, whitelist, Ratelimit.Scope.USER,
                5, 10000, JCommand.class, defaultUserMessage));

        ratelimits.add(new Ratelimit("guildWeatherComm", cacheMetrics, executor, whitelist, Ratelimit.Scope.GUILD,
                30, 180000, WeatherCommand.class, defaultGuildMessage));
        ratelimits.add(new Ratelimit("guildSongsAdded", cacheMetrics, executor, whitelist, Ratelimit.Scope.GUILD,
                1000, 120000, PlaylistInfo.class, playlistMessage));
        ratelimits.add(new Ratelimit("guildAllComms", cacheMetrics, executor, whitelist, Ratelimit.Scope.GUILD,
                10, 10000, JCommand.class, defaultGuildMessage));
    }

    /**
     * @param context           the context of the request
     * @param command           the command or other kind of object to be used
     * @param weight            how heavy the request is, default should be 1
     *
     * @return Whether this user is allowed to proceed. (true if they are ratelimited, false if everythign is fine).
     * If they happen to be ratelimited, they will be messaged, so the caller of this can just return.
     */
    public boolean isRatelimited(Context context, Object command, int weight) {
        for (Ratelimit ratelimit : ratelimits) {
            if (ratelimit.getClazz().isInstance(command)) {
                boolean allowed;
                //don't blacklist guilds
                if (ratelimit.scope == Ratelimit.Scope.GUILD) {
                    allowed = ratelimit.isAllowed(context, weight);
                } else {
                    allowed = ratelimit.isAllowed(context, weight, autoBlacklist);
                }
                if (!allowed) {
                    Metrics.commandsRatelimited.labels(command.getClass().getSimpleName()).inc();
                    context.replyWithMention(ratelimit.getMessage().apply(context));
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isRatelimited(Context context, Object command) {
        return isRatelimited(context, command, 1);
    }

    /**
     * @param id Id of the object whose blacklist status is to be checked, for example a userId or a guildId
     * @return true if the id is blacklisted, false if it's not
     */
    public Mono<Boolean> isBlacklisted(long id) {
        return (autoBlacklist != null) ? autoBlacklist.isBlacklisted(id) : Mono.just(false);
    }

    /**
     * Reset rate limits for the given id and removes it from the blacklist
     */
    public void liftLimitAndBlacklist(long id) {
        for (Ratelimit ratelimit : ratelimits) {
            ratelimit.liftLimit(id);
        }
        if (autoBlacklist != null)

            autoBlacklist.liftBlacklist(id);
    }
}
