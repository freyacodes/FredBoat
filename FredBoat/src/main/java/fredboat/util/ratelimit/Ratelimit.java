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

import fredboat.main.BotController;
import fredboat.messaging.internal.Context;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.util.Collections;
import java.util.Set;

/**
 * Created by napster on 17.04.17.
 * <p>
 * This class uses an algorithm based on leaky bucket, but is optimized, mainly we work around having tons of threads for
 * each bucket filling/emptying it, instead saving timestamps. As a result this class works better for shorter time
 * periods, as the amount of timestamps to hold decreases.
 * some calculations can be found here: https://docs.google.com/spreadsheets/d/1Afdn25AsFD-v3WQGp56rfVwO1y2d105IQk3dtfTcKwA/edit#gid=0
 */
public class Ratelimit {

    public enum Scope {USER, GUILD}

    private final Long2ObjectOpenHashMap<Rate> limits;
    private final long maxRequests;
    private final long timeSpan;

    //users that can never be limited
    private final Set<Long> userWhiteList;

    //are we limiting the individual user or whole guilds?
    public final Scope scope;

    //class of commands this ratelimiter should be restricted to
    //creative use allows usage of other classes
    private final Class clazz;

    public Class getClazz() {
        return clazz;
    }

    /**
     * @param userWhiteList whitelist of user that should never be rate limited or blacklisted by this object
     * @param scope         on which scope this rate limiter shall operate
     * @param maxRequests   how many maxRequests shall be possible in the specified time
     * @param milliseconds  time in milliseconds, in which maxRequests shall be allowed
     * @param clazz         the optional (=can be null) clazz of commands to be ratelimited by this ratelimiter
     */
    public Ratelimit(Set<Long> userWhiteList, Scope scope, long maxRequests, long milliseconds, Class clazz) {
        this.limits = new Long2ObjectOpenHashMap<>();

        this.userWhiteList = Collections.unmodifiableSet(userWhiteList);
        this.scope = scope;
        this.maxRequests = maxRequests;
        this.timeSpan = milliseconds;
        this.clazz = clazz;
    }

    public boolean isAllowed(Context context, int weight) {
        return isAllowed(context, weight, null);
    }

    /**
     * @return a RateResult object containing information whether the users request is rate limited or not and the reason for that
     * <p>
     * Caveat: This allows requests to overstep the ratelimit with single high weight requests.
     * The clearing of timestamps ensures it will take longer for them to get available again though.
     */
    public boolean isAllowed(Context context, int weight, Blacklist blacklist) {
        //This gets called real often, right before every command execution. Keep it light, don't do any blocking stuff,
        //ensure whatever you do in here is threadsafe, but minimize usage of synchronized as it adds overhead
        long id = context.getUser().getIdLong();
        //first of all, ppl that can never get limited or blacklisted, no matter what
        if (userWhiteList.contains(id)) return true;

        //user or guild scope?
        if (scope == Scope.GUILD) {
            id = context.getGuild().getIdLong();
        }

        Rate rate = limits.get(id);
        if (rate == null)
            rate = getOrCreateRate(id);

        //synchronize on the individual rate objects since we are about to change and save them
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (rate) {
            long now = System.currentTimeMillis();

            //clear outdated timestamps
            long maxTimeStampsToClear = (now - rate.lastUpdated) * maxRequests / timeSpan;
            long cleared = 0;
            while (rate.timeStamps.size() > 0 && rate.timeStamps.getLong(0) + timeSpan < now && cleared < maxTimeStampsToClear) {
                rate.timeStamps.removeLong(0);
                cleared++;
            }

            rate.lastUpdated = now;
            //ALLOWED?
            if (rate.timeStamps.size() < maxRequests) {
                for (int i = 0; i < weight; i++)
                    rate.timeStamps.add(now);
                //everything is fine, get out of this method
                return true;
            }
        }

        //reaching this point in the code means a rate limit was hit
        //the following code has to handle that

        if (blacklist != null && scope == Scope.USER)
            BotController.INS.getExecutor().submit(() -> bannerinoUserino(context, blacklist));
        return false;
    }

    /**
     * Notifies the autoblacklist that a user has hit a limit, and handles the response of the blacklist
     * Best run async as the blacklist might be hitting a database
     */
    private void bannerinoUserino(Context context, Blacklist blacklist) {
        long length = blacklist.hitRateLimit(context.getUser().getIdLong());
        if (length <= 0) {
            return; //nothing to do here
        }
        long s = length / 1000;
        String duration = String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
        String out = "\uD83D\uDD28 _**BLACKLISTED**_ \uD83D\uDD28 for **" + duration + "**";
        context.replyWithMention(out);
    }


    /**
     * synchronize the creation of new Rate objects
     */
    private synchronized Rate getOrCreateRate(long id) {
        //was one created on the meantime? use that
        Rate result = limits.get(id);
        if (result != null) return result;

        //create, save and return it
        result = new Rate(id);
        limits.put(id, result);
        return result;
    }

    /**
     * completely resets a limit for an id (user or guild for example)
     */
    public synchronized void liftLimit(long id) {
        limits.remove(id);
    }

    class Rate {
        //to whom this belongs
        final long id;

        //last time this object was updated
        //useful for keeping track of how many timeStamps should be removed to ensure the limit is enforced
        long lastUpdated;

        //collects the requests
        LongArrayList timeStamps;

        private Rate(long id) {
            this.id = id;
            this.lastUpdated = System.currentTimeMillis();
            this.timeStamps = new LongArrayList();
        }

        @Override
        public int hashCode() {
            return Long.hashCode(id);
        }
    }
}
