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

package fredboat.db.entity.common;

import fredboat.FredBoat;
import fredboat.db.EntityReader;
import fredboat.db.EntityWriter;
import fredboat.db.adapters.LongArrayListSQLiteUserType;
import fredboat.db.adapters.LongArrayListUserType;
import fredboat.db.entity.IEntity;
import fredboat.db.entity.postgres.PostgresTracklist;
import fredboat.db.entity.sqlite.SQLiteTracklist;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.EmbeddedId;
import javax.persistence.MappedSuperclass;
import java.util.Collection;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by napster on 27.07.17.
 */
@TypeDefs({
        @TypeDef(
                name = "long-array-list",
                typeClass = LongArrayListUserType.class
        ),
        @TypeDef(
                name = "long-array-list-sqlite",
                typeClass = LongArrayListSQLiteUserType.class
        )
})
@MappedSuperclass
public abstract class Tracklist implements IEntity<TracklistId> {

    @EmbeddedId
    protected TracklistId id;


    //implement these to provide access to the database specific implementations of these
    protected abstract LongArrayList getTracklist();

    protected abstract void setTracklist(LongArrayList tracklist);

    public static Tracklist create(long ownerId, String name, long[] trackIds) {
        if (FredBoat.obtainAvailableDbManager().isSQLiteDB()) {
            return new SQLiteTracklist(ownerId, name, trackIds);
        } else {
            return new PostgresTracklist(ownerId, name, trackIds);
        }
    }

    public static Tracklist load(long ownerId, String name) {
        Class<? extends Tracklist> clazz;
        if (FredBoat.obtainAvailableDbManager().isSQLiteDB()) {
            clazz = SQLiteTracklist.class;
        } else {
            clazz = PostgresTracklist.class;
        }

        return EntityReader.getOrCreateEntity(new TracklistId(ownerId, name), clazz);
    }

    public static void delete(long id) {
        EntityWriter.deleteObject(id, Tracklist.class);
    }

    public Tracklist save() {
        return EntityWriter.merge(this);
    }


    public long get(int index) {
        return getTracklist().getLong(index);
    }

    public long[] subArray(int start, int end) {
        getTracklist().trim();
        int range = end - start;
        long[] result = new long[range];
        getTracklist().getElements(start, result, 0, range);
        return result;
    }

    public boolean remove(long item) {
        return getTracklist().rem(item);
    }

    public long removeAt(int index) {
        return getTracklist().removeLong(index);
    }

    public Tracklist removeAll(Collection<Long> toRemove) {
        getTracklist().removeAll(toRemove);
        return this;
    }

    public Tracklist clear() {
        getTracklist().clear();
        getTracklist().trim();
        return this;
    }

    public boolean isEmpty() {
        return getTracklist().isEmpty();
    }

    public int size() {
        return getTracklist().size();
    }

    public Tracklist addAt(int index, long trackId) {
        getTracklist().add(index, trackId);
        return this;
    }

    public Tracklist add(long trackId) {
        getTracklist().add(trackId);
        return this;
    }

    public Tracklist addAll(Collection<Long> trackIds) {
        getTracklist().addAll(trackIds);
        return this;
    }

    //shuffle a track into the tracklist
    public Tracklist randAdd(long trackId) {

        if (getTracklist().isEmpty()) {
            getTracklist().add(trackId);
        } else {
            getTracklist().add(ThreadLocalRandom.current().nextInt(getTracklist().size()), trackId);
        }

        return this;
    }

    public Tracklist shuffle() {
        getTracklist().trim();
        shuffleArray(getTracklist().elements());
        return this;
    }

    // Fisher–Yates shuffle
    public static void shuffleArray(long[] ar) {
        Random rnd = ThreadLocalRandom.current();
        for (int i = ar.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            long a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Tracklist) && ((Tracklist) obj).id.equals(this.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public void setId(TracklistId id) {
        this.id = id;
    }

    @Override
    public TracklistId getId() {
        return id;
    }
}
