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

package fredboat.db.entity.common;

import fredboat.db.entity.IEntity;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by napster on 26.04.17.
 * <p>
 * closely related to the stuff in fredboat.util.ratelimit
 */
@Entity
@Table(name = "blacklist")
public class BlacklistEntry implements IEntity<Long> {

    //id of the user or guild that this blacklist entry belongs to
    @Id
    public long id;

    //blacklist level that the user or guild is on
    //this should increase every time progressively
    @Column(name = "level", nullable = false)
    public int level;

    //keeps track of how many times a user or guild reached the rate limit on the current blacklist level
    @Column(name = "rate_limit_reached", nullable = false)
    public int rateLimitReached;

    //when was the ratelimit hit the last time?
    @Column(name = "rate_limit_timestamp", nullable = false)
    @ColumnDefault("0") //tells hibernate ddl how to fill this by default with a zero
    public long rateLimitReachedTimestamp;

    //time when the id was blacklisted
    @Column(name = "blacklisted_timestamp", nullable = false)
    public long blacklistedTimestamp;

    public BlacklistEntry(long id) {
        this.id = id;
        this.level = -1;
        this.rateLimitReached = 0;
        this.blacklistedTimestamp = System.currentTimeMillis();
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    //Boilerplate code below

    public BlacklistEntry() {
    }

    @Override
    public Long getId() {
        return id;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getRateLimitReached() {
        return rateLimitReached;
    }

    public void setRateLimitReached(int rateLimitReached) {
        this.rateLimitReached = rateLimitReached;
    }

    public long getRateLimitReachedTimestamp() {
        return rateLimitReachedTimestamp;
    }

    public void setRateLimitReachedTimestamp(long rateLimitReachedTimestamp) {
        this.rateLimitReachedTimestamp = rateLimitReachedTimestamp;
    }

    public long getBlacklistedTimestamp() {
        return blacklistedTimestamp;
    }

    public void setBlacklistedTimestamp(long blacklistedTimestamp) {
        this.blacklistedTimestamp = blacklistedTimestamp;
    }
}
