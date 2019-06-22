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

package fredboat.audio.queue

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import fredboat.audio.player.GuildPlayer
import fredboat.sentinel.Member
import lavalink.client.player.TrackData

class SplitAudioTrackContext(
        at: AudioTrack,
        member: Member,
        override val startPosition: Long,
        val endPosition: Long,
        override val effectiveTitle: String
) : AudioPlaylistContext(at, member) {

    override val effectiveDuration: Long
        get() = endPosition - startPosition

    init {
        at.userData = TrackData(startPosition, endPosition)
    }

    override fun getEffectivePosition(guildPlayer: GuildPlayer): Long {
        return super.getEffectivePosition(guildPlayer) - startPosition
    }

    override fun makeClone(): AudioTrackContext {
        val track = track.makeClone()
        track.position = startPosition
        return SplitAudioTrackContext(track, member, startPosition, endPosition, effectiveTitle)
    }
}
