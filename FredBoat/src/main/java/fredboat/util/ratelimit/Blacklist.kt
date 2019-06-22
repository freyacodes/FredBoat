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

package fredboat.util.ratelimit

import fredboat.db.api.BlacklistRepository
import fredboat.feature.metrics.Metrics
import reactor.core.publisher.Mono
import java.util.*

/**
 * Created by napster on 17.04.17.
 *
 *
 * Provides a forgiving blacklist with progressively increasing blacklist lengths
 *
 *
 * In an environment where shards are running in different containers and not inside a single jar this class will need
 * some help in keeping bans up to date, that is, reading them from the database, either on changes (rethinkDB?) or
 * through an agent in regular periods
 */
class Blacklist(private val repository: BlacklistRepository, userWhiteList: Set<Long>, private val rateLimitHitsBeforeBlacklist: Long) {

    //users that can never be blacklisted
    private val userWhiteList: Set<Long> = Collections.unmodifiableSet(userWhiteList)


    /**
     * @param id check whether this id is blacklisted
     * @return true if the id is blacklisted, false if not
     */
    //This will be called really fucking often, should be able to be accessed non-synchronized for performance
    // -> don't do any writes in here
    // -> don't call expensive methods
    fun isBlacklisted(id: Long): Mono<Boolean> {

        //first of all, ppl that can never get blacklisted no matter what
        return if (userWhiteList.contains(id)) Mono.just(false) else repository.fetch(id).map { (_, level, _, _, blacklistTime) ->
            if (level < 0) return@map false // blacklist entry exists, but id hasn't actually been blacklisted yet

            // id was blacklisted, but it has run out
            if (System.currentTimeMillis() > blacklistTime + getBlacklistTimeLength(level)) {
                return@map false
            }

            // looks like this id is blacklisted ¯\_(ツ)_/¯
            true
        }

    }

    /**
     * @return length if issued blacklisting, 0 if none has been issued
     */
    fun hitRateLimit(id: Long): Mono<Long> {
        //update blacklist entry of this id
        var blacklistingLength: Long = 0
        return repository.fetch(id).map {

            //synchronize on the individual blacklist entries since we are about to change and convertAndSave them
            // we can use these to synchronize because they are backed by a cache, subsequent calls to fetch them
            // will return the same object

            synchronized(it) {
                val now = System.currentTimeMillis()

                //is the last ratelimit hit a long time away (1 hour)? then reset the ratelimit hits
                if (now - it!!.lastHitTime > 60 * 60 * 1000) {
                    it.hitCount = 0
                }
                it.incHitCount()
                it.lastHitTime = now
                if (it.hitCount >= rateLimitHitsBeforeBlacklist) {
                    //issue blacklist incident
                    it.incLevel()
                    if (it.level < 0) it.level = 0
                    Metrics.autoBlacklistsIssued.labels(Integer.toString(it.level)).inc()
                    it.blacklistTime = now
                    it.hitCount = 0 //reset these for the next time

                    blacklistingLength = getBlacklistTimeLength(it.level)
                }
                //persist it
                //if this turns up to be a performance bottleneck, have an agent run that persists the blacklist occasionally
                repository.update(it).subscribe()
                blacklistingLength
            }
        }


    }

    /**
     * completely resets a blacklist for an id
     */
    fun liftBlacklist(id: Long) {
        repository.remove(id)
    }

    /**
     * Return length of a blacklist incident in milliseconds depending on the blacklist level
     */
    private fun getBlacklistTimeLength(blacklistLevel: Int): Long {
        if (blacklistLevel < 0) return 0
        return if (blacklistLevel >= blacklistLevels.size) blacklistLevels[blacklistLevels.size - 1] else blacklistLevels[blacklistLevel]
    }

    companion object {

        //this holds progressively increasing lengths of blacklisting in milliseconds
        private val blacklistLevels: List<Long> = listOf(
                1000L * 60, //one minute
                1000L * 600, //ten minutes
                1000L * 3600, //one hour
                1000L * 3600 * 24, //24 hours
                1000L * 3600 * 24 * 7           //a week
        )

    }
}
