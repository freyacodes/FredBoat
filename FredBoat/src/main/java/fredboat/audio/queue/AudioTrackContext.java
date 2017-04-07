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
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Member;

import java.util.Random;

public class AudioTrackContext implements Comparable<AudioTrackContext> {

    protected final AudioTrack track;
    private final String userId;
    private final String guildId;
    protected final JDA jda;
    private int rand;
    private final int id; //used to identify this track even when the track gets cloned and the rand reranded

    public AudioTrackContext(AudioTrack at, Member member) {
        this.track = at;
        this.userId = member.getUser().getId();
        this.guildId = member.getGuild().getId();
        this.jda = member.getJDA();
        this.rand = new Random().nextInt();
        this.id = new Random().nextInt();
    }

    public AudioTrackContext(AudioTrack at, Member member, int chronologicalIndex) {
        this.track = at;
        this.userId = member.getUser().getId();
        this.guildId = member.getGuild().getId();
        this.jda = member.getJDA();
        this.rand = new Random().nextInt();
        this.id = new Random().nextInt();
    }

    public AudioTrack getTrack() {
        return track;
    }

    public Member getMember() {
        return jda.getGuildById(guildId).getMember(jda.getUserById(userId));
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
        rand = new Random().nextInt();
        return rand;
    }

    public AudioTrackContext makeClone() {
        return new AudioTrackContext(track.makeClone(), getMember());
    }

    public long getEffectiveDuration() {
        return track.getDuration();
    }

    public long getEffectivePosition() {
        return track.getPosition();
    }

    public void setEffectivePosition(long position) {
        track.setPosition(position);
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
}
