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

import com.sedmelluq.discord.lavaplayer.track.TrackMarkerHandler;
import fredboat.audio.player.AbstractPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrackEndMarkerHandler implements TrackMarkerHandler {

    private static final Logger log = LoggerFactory.getLogger(TrackEndMarkerHandler.class);

    private final AbstractPlayer player;
    private final AudioTrackContext track;

    public TrackEndMarkerHandler(AbstractPlayer player, AudioTrackContext track) {
        this.player = player;
        this.track = track;
    }

    @Override
    public void handle(MarkerState state) {
        log.info("Stopping track " + track.getEffectiveTitle() + " because of end state: " + state);
        if (player.getPlayingTrack() != null && player.getPlayingTrack().getId() == track.getId()) {
            //if this was ended because the track finished instead of skipped, we need to transfer that info
            //state == STOPPED if the user skips it
            //state == REACHED if the tracks runs out by itself
            if (state.equals(MarkerState.REACHED))
                player.splitTrackEnded();
            else
                player.skip();
        }
    }
}
