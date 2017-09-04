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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JDA methods/hacks that had merit to put in its own class.
 *
 * @author Shredder121
 */
public class JDAUtil {

    public static int countAllGuilds(List<FredBoat> shards) {
        return shards.stream()
                // don't do this at home, we only use it for the size()
                .mapToInt(shard -> ((JDAImpl) shard.getJda()).getGuildMap().size())
                .sum();
    }

    public static long countAllUniqueUsers(List<FredBoat> shards, AtomicInteger biggestUserCount) {
        int expected = biggestUserCount.get() > 0 ? biggestUserCount.get() : LongOpenHashSet.DEFAULT_INITIAL_SIZE;
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
        //never shrink the user count (might happen due to not connected shards)
        biggestUserCount.accumulateAndGet(uniqueUsers.size(), Math::max);
        return uniqueUsers.size();
    }

    public static List<Guild> getAllGuilds(List<FredBoat> shards) {
        ArrayList<Guild> list = new ArrayList<>();

        for (FredBoat fb : shards) {
            // addAll() does actually need to use .toArray() but 1 copy is better than 2
            list.addAll(((JDAImpl)fb.getJda()).getGuildMap().valueCollection());
        }

        return list;
    }
}
