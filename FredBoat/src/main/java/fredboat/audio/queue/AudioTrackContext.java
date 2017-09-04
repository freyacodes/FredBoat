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
import fredboat.audio.player.PlayerRegistry;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;

import java.util.concurrent.ThreadLocalRandom;

public class AudioTrackContext implements Comparable<AudioTrackContext> {

    protected final AudioTrack track;
    private final String userId;
    private final String guildId;
    private final FredBoat shard;
    private int rand;
    private final int id; //used to identify this track even when the track gets cloned and the rand reranded

    public AudioTrackContext(AudioTrack at, Member member) {
        this.track = at;
        this.userId = member.getUser().getId();
        this.guildId = member.getGuild().getId();
        this.shard = FredBoat.getInstance(member.getJDA());
        this.rand = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
        this.id = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
    }

    public AudioTrack getTrack() {
        return track;
    }

    public Member getMember() {
        //if we can't find the user anymore
        //work around tons of null pointer exceptions throwing/handling by setting fredboat as the owner of the song
        User user = getJda().getUserById(userId);
        if (user == null) { //the bot has no shared servers with the user
            user = getJda().getSelfUser();
        }
        Member songOwner = getJda().getGuildById(guildId).getMember(user);
        if (songOwner == null) //member left the guild
            songOwner = getJda().getGuildById(guildId).getSelfMember();
        return songOwner;
    }

    public int getRand() {
        return rand;
    }

    public int getId() {
        return id;
    }

    public void setRand(int rand) {
        this.rand = rand;
    }

    public int randomize() {
        rand = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
        return rand;
    }

    public AudioTrackContext makeClone() {
        return new AudioTrackContext(track.makeClone(), getMember());
    }

    public long getEffectiveDuration() {
        return track.getDuration();
    }

    public long getEffectivePosition() {
        return PlayerRegistry.get(shard.getJda(), guildId).getPosition();
    }

    public void seekTo(long position) {
        PlayerRegistry.get(shard.getJda(), guildId).seekTo(position);
    }

    public String getEffectiveTitle() {
        return track.getInfo().title;
    }

    public long getStartPosition() {
        return 0;
    }

    @Override
    public int compareTo(AudioTrackContext atc) {
        if(rand > atc.getRand()) {
            return 1;
        } else if (rand < atc.getRand()) {
            return -1;
        } else {
            return 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AudioTrackContext)) return false;

        AudioTrackContext that = (AudioTrackContext) o;

        if (getRand() != that.getRand()) return false;
        if (!getTrack().equals(that.getTrack())) return false;
        if (!userId.equals(that.userId)) return false;
        return guildId.equals(that.guildId);

    }

    @Override
    public int hashCode() {
        int result = getTrack().hashCode();
        result = 31 * result + userId.hashCode();
        result = 31 * result + guildId.hashCode();
        result = 31 * result + getRand();
        return result;
    }

    public JDA getJda() {
        return shard.getJda();
    }
}
