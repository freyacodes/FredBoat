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

import fredboat.Config;
import fredboat.FredBoat;
import fredboat.audio.queue.PlaylistInfo;
import fredboat.command.maintenance.ShardsCommand;
import fredboat.command.music.control.SkipCommand;
import fredboat.command.util.WeatherCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.messaging.internal.Context;
import fredboat.util.DiscordUtil;
import fredboat.util.Tuple2;
import net.dv8tion.jda.core.JDA;
import org.eclipse.jetty.util.ConcurrentHashSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by napster on 17.04.17.
 * <p>
 * this object should be threadsafe by itself
 * <p>
 * http://i.imgur.com/ha0R3XZ.gif
 */
public class Ratelimiter {

    private static final int RATE_LIMIT_HITS_BEFORE_BLACKLIST = 10;

    //one ratelimiter for all running shards
    private static volatile Ratelimiter ratelimiterSingleton;

    public static Ratelimiter getRatelimiter() {
        Ratelimiter singleton = ratelimiterSingleton;
        if (singleton == null) {
            //we can't use the holder class pattern.
            //we have to use double-checked locking,
            //since we need to be able to retry the creation.
            synchronized (Ratelimiter.class) {
                singleton = ratelimiterSingleton;
                if (singleton == null) {
                    ratelimiterSingleton = singleton = new Ratelimiter();
                }
            }
        }
        return singleton;
    }

    private final List<Ratelimit> ratelimits;
    private Blacklist autoBlacklist = null;

    private Ratelimiter() {
        Set<Long> whitelist = new ConcurrentHashSet<>();

        //it is ok to use the jda of any shard as long as we aren't using it for guild specific stuff
        JDA jda = FredBoat.getFirstJDA();
        whitelist.add(Long.valueOf(DiscordUtil.getOwnerId(jda)));
        whitelist.add(jda.getSelfUser().getIdLong());
        //only works for those admins who are added with their userId and not through a roleId
        for (String admin : Config.CONFIG.getAdminIds())
            whitelist.add(Long.valueOf(admin));


        //Create all the rate limiters we want
        ratelimits = new ArrayList<>();

        if (Config.CONFIG.useAutoBlacklist())
            autoBlacklist = new Blacklist(whitelist, RATE_LIMIT_HITS_BEFORE_BLACKLIST);

        //sort these by harsher limits coming first
        ratelimits.add(new Ratelimit(whitelist, Ratelimit.Scope.USER, 2, 30000, ShardsCommand.class));
        ratelimits.add(new Ratelimit(whitelist, Ratelimit.Scope.USER, 5, 20000, SkipCommand.class));
        ratelimits.add(new Ratelimit(whitelist, Ratelimit.Scope.USER, 5, 10000, Command.class));

        ratelimits.add(new Ratelimit(whitelist, Ratelimit.Scope.GUILD, 30, 180000, WeatherCommand.class));
        ratelimits.add(new Ratelimit(whitelist, Ratelimit.Scope.GUILD, 1000, 120000, PlaylistInfo.class));
        ratelimits.add(new Ratelimit(whitelist, Ratelimit.Scope.GUILD, 10, 10000, Command.class));
    }

    /**
     * @param context           the context of the request
     * @param command           the command or other kind of object to be used
     * @param weight            how heavy the request is, default should be 1
     * @return a result object containing further information
     */
    public Tuple2<Boolean, Class> isAllowed(Context context, Object command, int weight) {
        for (Ratelimit ratelimit : ratelimits) {
            if (ratelimit.getClazz().isInstance(command)) {
                boolean allowed;
                //don't blacklist guilds
                if (ratelimit.scope == Ratelimit.Scope.GUILD) {
                    allowed = ratelimit.isAllowed(context, weight);
                } else {
                    allowed = ratelimit.isAllowed(context, weight, autoBlacklist);
                }
                if (!allowed) return new Tuple2<>(false, ratelimit.getClazz());
            }
        }
        return new Tuple2<>(true, null);
    }

    /**
     * @param id Id of the object whose blacklist status is to be checked, for example a userId or a guildId
     * @return true if the id is blacklisted, false if it's not
     */
    public boolean isBlacklisted(long id) {
        return autoBlacklist != null && autoBlacklist.isBlacklisted(id);
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
