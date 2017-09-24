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

import fredboat.db.EntityReader;
import fredboat.db.EntityWriter;
import fredboat.db.entity.common.BlacklistEntry;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by napster on 17.04.17.
 * <p>
 * Provides a forgiving blacklist with progressively increasing blacklist lengths
 *
 * In an environment where shards are running in different containers and not inside a single jar this class will need
 * some help in keeping bans up to date, that is, reading them from the database, either on changes (rethinkDB?) or
 * through an agent in regular periods
 */
public class Blacklist {

    //this holds progressively increasing lengths of blacklisting in milliseconds
    private static final List<Long> blacklistLevels;

    static {
        List<Long> levels = new ArrayList<>();
        levels.add(1000L * 60);                     //one minute
        levels.add(1000L * 600);                    //ten minutes
        levels.add(1000L * 3600);                   //one hour
        levels.add(1000L * 3600 * 24);              //24 hours
        levels.add(1000L * 3600 * 24 * 7);          //a week

        blacklistLevels = Collections.unmodifiableList(levels);
    }

    private final long rateLimitHitsBeforeBlacklist;

    private final Long2ObjectOpenHashMap<BlacklistEntry> blacklist;

    //users that can never be blacklisted
    private final Set<Long> userWhiteList;


    public Blacklist(Set<Long> userWhiteList, long rateLimitHitsBeforeBlacklist) {
        this.blacklist = new Long2ObjectOpenHashMap<>();
        //load blacklist from database
        for (BlacklistEntry ble : EntityReader.loadAll(BlacklistEntry.class)) {
            blacklist.put(ble.id, ble);
        }

        this.rateLimitHitsBeforeBlacklist = rateLimitHitsBeforeBlacklist;
        this.userWhiteList = Collections.unmodifiableSet(userWhiteList);
    }

    /**
     * @param id check whether this id is blacklisted
     * @return true if the id is blacklisted, false if not
     */
    //This will be called really fucking often, should be able to be accessed non-synchronized for performance
    // -> don't do any writes in here
    // -> don't call expensive methods
    public boolean isBlacklisted(long id) {

        //first of all, ppl that can never get blacklisted no matter what
        if (userWhiteList.contains(id)) return false;

        BlacklistEntry blEntry = blacklist.get(id);
        if (blEntry == null) return false;     //blacklist entry doesn't even exist
        if (blEntry.level < 0) return false;   //blacklist entry exists, but id hasn't actually been blacklisted yet

        //id was a blacklisted, but it has run out
        if (System.currentTimeMillis() > blEntry.blacklistedTimestamp + (getBlacklistTimeLength(blEntry.level)))
            return false;

        //looks like this id is blacklisted ¯\_(ツ)_/¯
        return true;
    }

    /**
     * @return length if issued blacklisting, 0 if none has been issued
     */
    public long hitRateLimit(long id) {
        //update blacklist entry of this id
        long blacklistingLength = 0;
        BlacklistEntry blEntry = blacklist.get(id);
        if (blEntry == null)
            blEntry = getOrCreateBlacklistEntry(id);

        //synchronize on the individual blacklist entries since we are about to change and save them
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (blEntry) {
            long now = System.currentTimeMillis();

            //is the last ratelimit hit a long time away (1 hour)? then reset the ratelimit hits
            if (now - blEntry.rateLimitReachedTimestamp > 60 * 60 * 1000) {
                blEntry.rateLimitReached = 0;
            }
            blEntry.rateLimitReached++;
            blEntry.rateLimitReachedTimestamp = now;
            if (blEntry.rateLimitReached >= rateLimitHitsBeforeBlacklist) {
                //issue blacklist incident
                blEntry.level++;
                if (blEntry.level < 0) blEntry.level = 0;
                blEntry.blacklistedTimestamp = now;
                blEntry.rateLimitReached = 0; //reset these for the next time

                blacklistingLength = getBlacklistTimeLength(blEntry.level);
            }
            //persist it
            //if this turns up to be a performance bottleneck, have an agent run that persists the blacklist occasionally
            blacklist.put((long) blEntry.getId(), EntityWriter.merge(blEntry));
            return blacklistingLength;
        }
    }


    /**
     * synchronize the creation of new blacklist entries
     */
    private synchronized BlacklistEntry getOrCreateBlacklistEntry(long id) {
        //was one created in the meantime? use that
        BlacklistEntry result = blacklist.get(id);
        if (result != null) return result;

        //create and return it
        result = new BlacklistEntry(id);
        blacklist.put(id, result);
        return result;
    }

    /**
     * completely resets a blacklist for an id
     */
    public synchronized void liftBlacklist(long id) {
        blacklist.remove(id);
        EntityWriter.deleteObject(id, BlacklistEntry.class);
    }

    /**
     * Return length of a blacklist incident in milliseconds depending on the blacklist level
     */
    private long getBlacklistTimeLength(int blacklistLevel) {
        if (blacklistLevel < 0) return 0;
        return blacklistLevel >= blacklistLevels.size() ? blacklistLevels.get(blacklistLevels.size() - 1) : blacklistLevels.get(blacklistLevel);
    }
}
