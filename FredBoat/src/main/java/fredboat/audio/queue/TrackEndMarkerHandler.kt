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

import com.sedmelluq.discord.lavaplayer.track.TrackMarkerHandler
import fredboat.audio.player.GuildPlayer
import org.slf4j.LoggerFactory

class TrackEndMarkerHandler(
        private val player: GuildPlayer,
        private val track: AudioTrackContext
) : TrackMarkerHandler {

    companion object {
        private val log = LoggerFactory.getLogger(TrackEndMarkerHandler::class.java)
    }

    override fun handle(state: TrackMarkerHandler.MarkerState) {
        log.info("Stopping track " + track.effectiveTitle + " because of end state: " + state)
        if (player.playingTrack != null && player.playingTrack!!.trackId == track.trackId) {
            //if this was ended because the track finished instead of skipped, we need to transfer that info
            //state == STOPPED after we already called skip on it, so it may be ignored safely
            //state == REACHED if the tracks runs out by itself
            //state == BYPASSED if the track was forwarded over its length
            if ((state == TrackMarkerHandler.MarkerState.REACHED) or (state == TrackMarkerHandler.MarkerState.BYPASSED))
                player.stopTrack()
        }
    }
}
