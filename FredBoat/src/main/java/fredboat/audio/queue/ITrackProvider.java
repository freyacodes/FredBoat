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

import java.util.Collection;
import java.util.List;

public interface ITrackProvider {

    /**
     * @return the track that a call to provideAudioTrack() would return
     */
    AudioTrackContext peek();

    /**
     * @return the next track
     */
    AudioTrackContext provideAudioTrack();

    /**
     * Call this when the current track is skipped by the user to let the provider know about it
     */
    void skipped();

    /**
     * When restoring a guild player this allows us to set a potentially currently playing track
     */
    void setLastTrack(AudioTrackContext lastTrack);

    /**
     * @return a list of all tracks in the queue in regular (unshuffled) order
     */
    List<AudioTrackContext> getAsList();

    /**
     * @return true if there are no tracks in the queue
     */
    boolean isEmpty();

    /**
     * @return amount of tracks in the queue
     */
    int size();

    /**
     * @param track add a track to the queue
     */
    void add(AudioTrackContext track);

    /**
     * @param tracks add several tracks to the queue
     */
    void addAll(Collection<AudioTrackContext> tracks);

    /**
     * empty the queue
     */
    void clear();

    /**
     * remove a track from the queue
     *
     * @param atc the track to be removed
     * @return true if the track part of the queue, false if not
     */
    boolean remove(AudioTrackContext atc);

    /**
     * @param tracks tracks to be removed from the queue
     */
    void removeAll(Collection<AudioTrackContext> tracks);

    /**
     * @param trackIds tracks to be removed from the queue
     */
    void removeAllById(Collection<Long> trackIds);

    /**
     * @param index the index of the requested track in playing order
     * @return the track at the given index
     */
    AudioTrackContext getTrack(int index);

    /**
     * Returns all songs from one index till another in a non-bitching way.
     * That means we will look from the inclusive lower one of the provided two indices to the exclusive higher one.
     * If an index is lower 0 the range will start at 0, and if an index is over the max size of the track list
     * the range will end at the max size of the track list
     *
     * @param startIndex inclusive starting index
     * @param endIndex   exclusive ending index
     * @return the tracks in the given range
     */
    List<AudioTrackContext> getTracksInRange(int startIndex, int endIndex);

    /**
     * @return duration of all tracks
     */
    long getDurationMillis();

    /**
     * @return amount of live streams
     */
    int streamsCount();

    /**
     * @return false if any of the provided tracks was added by user that is not the provided userId
     */
    boolean isUserTrackOwner(long userId, Collection<Long> trackIds);

}
