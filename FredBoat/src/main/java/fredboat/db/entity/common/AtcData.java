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

import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fredboat.audio.player.AbstractPlayer;
import fredboat.audio.queue.AudioTrackContext;
import fredboat.audio.queue.SplitAudioTrackContext;
import fredboat.db.entity.IEntity;
import org.apache.commons.codec.binary.Base64;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by napster on 05.05.17.
 * <p>
 * A serializable representation of AudioTrackContext
 * This table holds all tracks of all guilds
 */
@Entity
@Table(name = "audio_tracks")
public class AtcData implements IEntity<Long> {

    private static final String trackIdSeqName = "track_id_seq";
    @Id
    @SequenceGenerator(name = trackIdSeqName, sequenceName = trackIdSeqName, allocationSize = 100)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = trackIdSeqName)
    @Column(name = "track_id", nullable = false, updatable = false)
    private long trackId;

    @Column(name = "user_id", nullable = false)
    private long userId;

    @Column(name = "guild_id", nullable = false)
    private long guildId;

    @Column(name = "added", nullable = false)
    private long added;

    @Column(name = "encoded", nullable = false)
    private byte[] encoded;

    @Column(name = "stream", nullable = false)
    private boolean isStream;

    @Column(name = "duration")
    private long duration;

    @Column(name = "is_split_track", nullable = false)
    private boolean isSplitTrack = false;

    @Column(name = "split_title")
    private String title;

    @Column(name = "split_start_position")
    private long startPos;

    @Column(name = "split_end_position")
    private long endPos;


    //may return null if there were issues decoding it
    public AudioTrackContext restoreTrack() throws IOException {
        byte[] decoded = Base64.decodeBase64(encoded);
        ByteArrayInputStream bais = new ByteArrayInputStream(decoded);
        AudioTrack at = AbstractPlayer.getPlayerManager().decodeTrack(new MessageInput(bais)).decodedTrack;
        if (at == null) {
            throw new IOException("Decoding track with trackId " + trackId + "returned null.");
        }

        if (!isSplitTrack) {
            //regular track
            return AudioTrackContext.restore(at, userId, guildId, added, trackId);
        } else {
            //split track
            return SplitAudioTrackContext.restore(at, userId, guildId, added, trackId,
                    startPos, endPos, title);
        }
    }

    public AtcData(AudioTrackContext atc) throws IOException {
        userId = atc.getUserId();
        guildId = atc.getGuildId();
        added = atc.getAdded();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        AbstractPlayer.getPlayerManager().encodeTrack(new MessageOutput(baos), atc.getTrack());
        encoded = Base64.encodeBase64(baos.toByteArray());

        isStream = atc.getTrack().getInfo().isStream;
        if (isStream) duration = 0; //dont ask streams for their duration, they will report Long.MAX_VALUE
        else duration = atc.getTrack().getDuration();

        isSplitTrack = false;
        if (atc instanceof SplitAudioTrackContext) {
            isSplitTrack = true;
            SplitAudioTrackContext c = (SplitAudioTrackContext) atc;
            title = c.getEffectiveTitle();
            startPos = c.getStartPosition();
            endPos = c.getStartPosition() + c.getEffectiveDuration();
            duration = c.getEffectiveDuration();
        }
    }


    // boilerplate code below this
    public AtcData() {
    }

    @Override
    public void setId(Long id) {
        throw new UnsupportedOperationException("AudioTrackContextData's trackId may not be set/created in this way");
    }

    @Override
    public Long getId() {
        return trackId;
    }

    public long getTrackId() {
        return trackId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getGuildId() {
        return guildId;
    }

    public void setGuildId(long guildId) {
        this.guildId = guildId;
    }

    public long getAdded() {
        return added;
    }

    public byte[] getEncoded() {
        return encoded;
    }

    public void setEncoded(byte[] encoded) {
        this.encoded = encoded;
    }

    public boolean isStream() {
        return isStream;
    }

    public long getDuration() {
        return duration;
    }

    public boolean isSplitTrack() {
        return isSplitTrack;
    }

    public void setSplitTrack(boolean splitTrack) {
        isSplitTrack = splitTrack;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getStartPos() {
        return startPos;
    }

    public void setStartPos(long startPos) {
        this.startPos = startPos;
    }

    public long getEndPos() {
        return endPos;
    }

    public void setEndPos(long endPos) {
        this.endPos = endPos;
    }
}
