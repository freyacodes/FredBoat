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

package fredboat.db.entity.sqlite;

import fredboat.db.entity.common.Tracklist;
import fredboat.db.entity.common.TracklistId;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Created by napster on 12.08.17.
 */
@Entity
@Table(name = "tracklists")
public class SQLiteTracklist extends Tracklist {

    @Column(name = "track_ids", columnDefinition = "BLOB")
    @Type(type = "long-array-list-sqlite")
    protected LongArrayList trackIds = new LongArrayList();

    @Override
    protected LongArrayList getTracklist() {
        return trackIds;
    }

    @Override
    protected void setTracklist(LongArrayList tracklist) {
        trackIds = tracklist;
    }

    //for jpa and IEntity
    public SQLiteTracklist() {
    }

    public SQLiteTracklist(long ownerId, String name, long[] trackIds) {
        this.id = new TracklistId(ownerId, name);
        this.trackIds = new LongArrayList(trackIds);
    }
}
