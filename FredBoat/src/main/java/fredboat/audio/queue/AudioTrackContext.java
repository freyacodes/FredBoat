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

package fredboat.audio.queue;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fredboat.FredBoat;
import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.PlayerRegistry;
import fredboat.messaging.internal.LeakSafeContext;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ThreadLocalRandom;

public class AudioTrackContext extends LeakSafeContext implements Comparable<AudioTrackContext> {

    protected final AudioTrack track;
    private final long added;
    private int rand;
    private final long trackId; //used to identify this track even when the track gets cloned and the rand reranded

    public AudioTrackContext(AudioTrack at, Member member) {
        this(at, member.getGuild().getIdLong(), member.getUser().getIdLong());
    }

    protected AudioTrackContext(AudioTrack at, long guildId, long userId) {
        //It's ok to set a non-existing channelId, since inside the AudioTrackContext, the channel needs to be looked up
        // every time. See the getTextChannel() below for doing that.
        super(-1, guildId, userId);
        this.track = at;
        this.added = System.currentTimeMillis();
        this.rand = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
        this.trackId = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
    }

    public AudioTrack getTrack() {
        return track;
    }

    public long getUserId() {
        return userId;
    }

    public long getGuildId() {
        return guildId;
    }

    public long getAdded() {
        return added;
    }

    public int getRand() {
        return rand;
    }

    public long getTrackId() {
        return trackId;
    }

    public void setRand(int rand) {
        this.rand = rand;
    }

    public int randomize() {
        rand = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
        return rand;
    }

    public AudioTrackContext makeClone() {
        return new AudioTrackContext(track.makeClone(), guildId, userId);
    }

    public long getEffectiveDuration() {
        return track.getDuration();
    }

    //NOTE: convenience method that returns the position of the track currently playing in the guild where this track was added
    public long getEffectivePosition() {
        Guild guild = FredBoat.getGuildById(guildId);
        if (guild != null) {
            return PlayerRegistry.getOrCreate(guild).getPosition();
        } else {
            return getStartPosition();
        }
    }

    public String getEffectiveTitle() {
        return track.getInfo().title;
    }

    public long getStartPosition() {
        return 0;
    }

    @Override
    public int compareTo(@Nonnull AudioTrackContext atc) {
        return Integer.compare(rand, atc.getRand());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AudioTrackContext)) return false;

        AudioTrackContext that = (AudioTrackContext) o;

        if (getRand() != that.getRand()) return false;
        if (!getTrack().equals(that.getTrack())) return false;
        if (userId != that.userId) return false;
        return guildId == that.guildId;
    }

    @Override
    public int hashCode() {
        int result = track.hashCode();
        result = 31 * result + Long.hashCode(userId);
        result = 31 * result + Long.hashCode(guildId);
        result = 31 * result + Long.hashCode(trackId);
        return result;
    }

    //return the currently active text channel of the associated guildplayer
    @Override
    @Nullable
    public TextChannel getTextChannel() {
        Guild guild = FredBoat.getGuildById(guildId);
        if (guild != null) {
            GuildPlayer guildPlayer = PlayerRegistry.getExisting(guild);
            if (guildPlayer != null) {
                return guildPlayer.getActiveTextChannel();
            }
        }

        return null;
    }
}
