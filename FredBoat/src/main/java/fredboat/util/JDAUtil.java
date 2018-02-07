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

import gnu.trove.procedure.TObjectProcedure;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.impl.JDAImpl;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

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
    public static int countGuilds(@Nonnull Collection<JDA> shards) {
        long result = shards.stream()
                .mapToLong(shard -> shard.getGuildCache().size())
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
    public static int countUniqueUsers(@Nonnull Collection<JDA> shards, @Nullable AtomicInteger expectedUserCount) {
        if (shards.size() == 1) { //a single shard provides a cheap call for getting user cardinality
            return Math.toIntExact(shards.iterator().next().getUserCache().size());
        }

        int expected = expectedUserCount != null && expectedUserCount.get() > 0 ? expectedUserCount.get() : LongOpenHashSet.DEFAULT_INITIAL_SIZE;
        LongOpenHashSet uniqueUsers = new LongOpenHashSet(expected + 10000); //add 10k for good measure
        TObjectProcedure<User> adder = user -> {
            uniqueUsers.add(user.getIdLong());
            return true;
        };
        Collections.unmodifiableCollection(shards).forEach(
                // IMPLEMENTATION NOTE: READ
                // careful, touching the map is in not all cases safe
                // In this case, it just so happens to be safe, because the map is synchronized
                // this means however, that for the (small) duration, the map cannot be used by other threads (if there are any)
                shard -> ((JDAImpl) shard).getUserMap().forEachValue(adder)
        );
        return uniqueUsers.size();
    }

    /**
     * @return Sum of amount of TextChannels in the provided shards. The result will be a unique count if the provided
     * shards are unique since each TextChannel can only be present in one guild which can only be present in one shard.
     */
    @CheckReturnValue
    public static int countTextChannels(@Nonnull Collection<JDA> shards) {
        long result = shards.stream()
                .mapToLong(shard -> shard.getTextChannelCache().size())
                .sum();
        return Math.toIntExact(result);
    }

    /**
     * @return Sum of amount of VoiceChannels in the provided shards. The result will be a unique count if the provided
     * shards are unique since each VoiceChannel can only be present in one guild which can only be present in one shard.
     */
    @CheckReturnValue
    public static int countVoiceChannels(@Nonnull Collection<JDA> shards) {
        long result = shards.stream()
                .mapToLong(shard -> shard.getVoiceChannelCache().size())
                .sum();
        return Math.toIntExact(result);
    }

    /**
     * @return Sum of amount of Category in the provided shards. The result will be a unique count if the provided
     * shards are unique since each Category can only be present in one guild which can only be present in one shard.
     */
    @CheckReturnValue
    public static int countCategories(@Nonnull Collection<JDA> shards) {
        long result = shards.stream()
                .mapToLong(shard -> shard.getCategoryCache().size())
                .sum();
        return Math.toIntExact(result);
    }

    /**
     * @return Sum of amount of Emotes in the provided shards. The result will be a unique count if the provided
     * shards are unique since each Emote can only be present in one guild which can only be present in one shard.
     */
    @CheckReturnValue
    public static int countEmotes(@Nonnull Collection<JDA> shards) {
        long result = shards.stream()
                .mapToLong(shard -> shard.getEmoteCache().size())
                .sum();
        return Math.toIntExact(result);
    }

    /**
     * @return Sum of amount of Roles in the provided shards. The result will be a unique count if the provided
     * shards are unique since each Role can only be present in one guild which can only be present in one shard.
     */
    @CheckReturnValue
    public static int countRoles(@Nonnull Collection<JDA> shards) {
        long result = shards.stream()
                .mapToLong(shard -> shard.getRoleCache().size())
                .sum();
        return Math.toIntExact(result);
    }
}
