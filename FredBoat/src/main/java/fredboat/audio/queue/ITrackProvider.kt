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

import org.bson.types.ObjectId

interface ITrackProvider {

    /**
     * @return a list of all tracks in the queue in regular (unshuffled) order
     */
    val asList: List<AudioTrackContext>

    /**
     * @return true if there are no tracks in the queue
     */
    val isEmpty: Boolean

    /**
     * @return duration of all tracks
     */
    val durationMillis: Long

    /**
     * @return the track that a call to provideAudioTrack() would return
     */
    fun peek(): AudioTrackContext?

    /**
     * @return the next track, or null if empty
     */
    fun provideAudioTrack(): AudioTrackContext?

    /**
     * Call this when the current track is skipped by the user to let the provider know about it
     */
    fun skipped()

    /**
     * When restoring a guild player this allows us to set a potentially currently playing track
     */
    fun setLastTrack(lastTrack: AudioTrackContext)

    /**
     * @return amount of tracks in the queue
     */
    fun size(): Int

    /**
     * @return amount of tracks in the queue filtered by userId
     */
    fun getCountByUser(userId: Long): Int

    /**
     * @param track add a track to the queue
     */
    fun add(track: AudioTrackContext)

    /**
     * @param tracks add several tracks to the queue
     */
    fun addAll(tracks: Collection<AudioTrackContext>)

    /**
     * empty the queue
     */
    fun clear()

    /**
     * remove a track from the queue
     *
     * @param atc the track to be removed
     * @return true if the track part of the queue, false if not
     */
    fun remove(atc: AudioTrackContext): Boolean

    /**
     * @param tracks tracks to be removed from the queue
     */
    fun removeAll(tracks: Collection<AudioTrackContext>)

    /**
     * @param trackIds tracks to be removed from the queue
     */
    fun removeAllById(trackIds: Collection<ObjectId>)

    /**
     * @param index the index of the requested track in playing order
     * @return the track at the given index
     */
    fun getTrack(index: Int): AudioTrackContext

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
    fun getTracksInRange(startIndex: Int, endIndex: Int): List<AudioTrackContext>

    /**
     * @return amount of live streams
     */
    fun streamsCount(): Int

    /**
     * @return false if any of the provided tracks was added by user that is not the provided userId
     */
    fun isUserTrackOwner(userId: Long, trackIds: Collection<ObjectId>): Boolean

}
