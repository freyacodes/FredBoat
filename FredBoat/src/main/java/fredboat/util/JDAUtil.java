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
 *
 */
package fredboat.util;

import fredboat.FredBoat;
import gnu.trove.procedure.TObjectProcedure;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.impl.JDAImpl;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * JDA methods/hacks that had merit to put in its own class.
 *
 * @author Shredder121
 */
public class JDAUtil {

    /**
     * @return Sum of amount of guilds in the provided shards. The result will be a unique count if the provided
     * shards are unique since each guild can only be present in one shard.
     */
    @CheckReturnValue
    public static int countGuilds(@Nonnull List<FredBoat> shards) {
        long result = shards.stream()
                .mapToLong(shard -> shard.getJda().getGuildCache().size())
                .sum();
        return Math.toIntExact(result); //the day where there are more than 2^32 guilds served by fredboat will be a glorious one. until then this is fine
    }

    /**
     * A count of unique users over the provided shards. This is an expensive operation given FredBoats scale.
     * <p>
     * Optionally pass in a value of value of previous counts / expected size to that we can initialize the set used
     * to count the unique values with an approriate size reducing expensive resizing operations.
     */
    @CheckReturnValue
    public static int countUniqueUsers(@Nonnull List<FredBoat> shards, @Nullable AtomicInteger biggestUserCount) {
        int expected = biggestUserCount != null && biggestUserCount.get() > 0 ? biggestUserCount.get() : LongOpenHashSet.DEFAULT_INITIAL_SIZE;
        LongOpenHashSet uniqueUsers = new LongOpenHashSet(expected + 100000); //add 100k for good measure
        TObjectProcedure<User> adder = user -> {
            uniqueUsers.add(user.getIdLong());
            return true;
        };
        Collections.unmodifiableCollection(shards).forEach(
                // IMPLEMENTATION NOTE: READ
                // careful, touching the map is in not all cases safe
                // In this case, it just so happens to be safe, because the map is synchronized
                // this means however, that for the (small) duration, the map cannot be used by other threads (if there are any)
                shard -> ((JDAImpl) shard.getJda()).getUserMap().forEachValue(adder)
        );
        return uniqueUsers.size();
    }

    /**
     * @return Returns a non-distinct stream over all Guild entities in the provided shards.
     */
    @Nonnull
    @CheckReturnValue
    public static Stream<Guild> getGuilds(@Nonnull List<FredBoat> shards) {
        return shards.stream().flatMap(fb -> fb.getJda().getGuildCache().stream());
    }

    /**
     * @return Returns a non-distinct stream over all User entities in the provided shards.
     */
    @Nonnull
    @CheckReturnValue
    public static Stream<User> getUsers(@Nonnull List<FredBoat> shards) {
        return shards.stream().flatMap(fb -> fb.getJda().getUserCache().stream());
    }
}
