/*
 * MIT License
 *
 * Copyright (c) 2016 Frederik Ar. Mikkelsen
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

    private final AudioTrack track;
    private final String userId;
    private final String guildId;
    private final JDA jda;
    private int rand;

    public AudioTrackContext(AudioTrack at, Member member) {
        this.track = at;
        this.userId = member.getUser().getId();
        this.guildId = member.getGuild().getId();
        this.jda = member.getJDA();
        this.rand = new Random().nextInt();
    }

    public AudioTrackContext(AudioTrack at, Member member, int chronologicalIndex) {
        this.track = at;
        this.userId = member.getUser().getId();
        this.guildId = member.getGuild().getId();
        this.jda = member.getJDA();
        this.rand = new Random().nextInt();
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

    public int randomize() {
        rand = new Random().nextInt();
        return rand;
    }

    public AudioTrackContext makeClone() {
        return new AudioTrackContext(track.makeClone(), getMember());
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
}
