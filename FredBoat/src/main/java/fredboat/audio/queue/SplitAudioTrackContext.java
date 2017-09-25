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
import lavalink.client.player.TrackData;
import net.dv8tion.jda.core.entities.Member;

public class SplitAudioTrackContext extends AudioTrackContext {

    private final long startPos;
    private final long endPos;
    private final String title;

    public SplitAudioTrackContext(AudioTrack at, Member member, long startPos, long endPos, String title) {
        this(at, member.getUser().getIdLong(), member.getGuild().getIdLong(), startPos, endPos, title);
    }

    public SplitAudioTrackContext(AudioTrack at, long userId, long guildId, long startPos, long endPos, String title) {
        super(at, userId, guildId);
        this.startPos = startPos;
        this.endPos = endPos;
        this.title = title;

        at.setUserData(new TrackData(startPos, endPos));
    }

    @Override
    public long getEffectiveDuration() {
        return endPos - startPos;
    }

    @Override
    public long getEffectivePosition() {
        return super.getEffectivePosition() - startPos;
    }

    @Override
    public String getEffectiveTitle() {
        return title;
    }

    @Override
    public long getStartPosition() {
        return startPos;
    }

    @Override
    public AudioTrackContext makeClone() {
        AudioTrack track = getTrack().makeClone();
        track.setPosition(startPos);
        return new SplitAudioTrackContext(track, getUserId(), getGuildId(), startPos, endPos, title);
    }
}
