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

import fredboat.audio.queue.RepeatMode;
import fredboat.db.entity.IEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by napster on 05.05.17.
 * <p>
 * Persist the state of a guild player
 */

@Entity
@Table(name = "guild_player")
public class GuildPlayerData implements IEntity<Long> {

    @Id
    @Column(name = "guild_id", nullable = false)
    private long guildId;

    @Column(name = "voicechannel_id", nullable = false)
    private long voiceChannelId;

    @Column(name = "active_textchannel_id", nullable = false)
    private long activeTextChannelId;

    @Column(name = "is_paused", nullable = false)
    private boolean isPaused;

    @Column(name = "volume", nullable = false)
    private float volume;

    @Column(name = "repeat_mode", nullable = false)
    private String repeatMode;

    @Column(name = "is_shuffled", nullable = false)
    private boolean isShuffled;

    @Column(name = "playing_track_id", nullable = false)
    private long playingTrackId;

    @Column(name = "playing_track_position", nullable = false)
    private long playingTrackPosition;

    //timestamp to keep track when this a guild has used their playlist the last time
    //over time we could end up with hundreds of thousands stale and unused guild players being persisted/loaded over time
    //this helps us make the decision whether to discard some of these after a some time
    @Column(name = "last_changed")
    private long lastChanged;


    public GuildPlayerData(long guildId, long vcId, long tcId, boolean isPaused, float volume, RepeatMode repeatMode,
                           boolean isShuffled, long playingTrackId, long playingTrackPosition) {

        this.guildId = guildId;
        this.voiceChannelId = vcId;
        this.activeTextChannelId = tcId;
        this.isPaused = isPaused;
        this.volume = volume;
        this.repeatMode = repeatMode.name();
        this.isShuffled = isShuffled;
        this.playingTrackId = playingTrackId;
        this.playingTrackPosition = playingTrackPosition;
        this.lastChanged = System.currentTimeMillis();
    }

    public boolean isDifferent(GuildPlayerData other) {
        return !(guildId == other.guildId
                && voiceChannelId == other.voiceChannelId
                && activeTextChannelId == other.activeTextChannelId
                && isPaused == other.isPaused
                && volume == other.volume
                && repeatMode.equalsIgnoreCase(other.repeatMode)
                && isShuffled == other.isShuffled
                && playingTrackId == other.playingTrackId
                && playingTrackPosition == other.playingTrackPosition);
    }

    // boilerplate code below this

    //for jpa and IEntity
    public GuildPlayerData() {
    }

    @Override
    public void setId(Long id) {
        this.guildId = id;
    }

    @Override
    public Long getId() {
        return guildId;
    }

    public long getGuildId() {
        return guildId;
    }

    public void setGuildId(long guildId) {
        this.guildId = guildId;
    }

    public long getVoiceChannelId() {
        return voiceChannelId;
    }

    public void setVoiceChannelId(long voiceChannelId) {
        this.voiceChannelId = voiceChannelId;
    }

    public long getActiveTextChannelId() {
        return activeTextChannelId;
    }

    public void setActiveTextChannelId(long activeTextChannelId) {
        this.activeTextChannelId = activeTextChannelId;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        isPaused = paused;
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public RepeatMode getRepeatMode() {
        return RepeatMode.valueOf(repeatMode);
    }

    public void setRepeatMode(RepeatMode repeatMode) {
        this.repeatMode = repeatMode.name();
    }

    public boolean isShuffled() {
        return isShuffled;
    }

    public void setShuffled(boolean shuffled) {
        isShuffled = shuffled;
    }

    public long getPlayingTrackId() {
        return playingTrackId;
    }

    public void setPlayingTrackId(long playingTrackId) {
        this.playingTrackId = playingTrackId;
    }

    public long getPlayingTrackPosition() {
        return playingTrackPosition;
    }

    public void setPlayingTrackPosition(long playingTrackPosition) {
        this.playingTrackPosition = playingTrackPosition;
    }

    public long getLastChanged() {
        return lastChanged;
    }

    public void setLastChanged(long lastChanged) {
        this.lastChanged = lastChanged;
    }
}
