package fredboat.audio.queue.handlers

import fredboat.audio.queue.AudioTrackContext
import org.bson.types.ObjectId
import java.util.concurrent.ConcurrentLinkedDeque

interface IQueueHandler {

    val queue: ConcurrentLinkedDeque<AudioTrackContext>

    val isEmpty: Boolean get() = queue.isEmpty()

    val size: Int get() = queue.size

    val totalDuration: Long get() = queue.fold(0L) { acc, v -> acc + v.effectiveDuration }

    val streamCount: Int get() = queue.count { it.track.info.isStream }

    fun peek(): AudioTrackContext? = queue.peek()

    fun take(silent: Boolean = false): AudioTrackContext? = queue.poll()?.also { onTaken(it, silent) }

    fun onTaken(track: AudioTrackContext, silent: Boolean)

    fun getInRange(start: Int, end: Int): Collection<AudioTrackContext> = queue.filterIndexed { i, _ -> i in start..end }

    fun add(track: AudioTrackContext, silent: Boolean = false) {
        if (track.isPriority) queue.addFirst(track) else queue.add(track)

        onAdded(track, silent)
    }

    fun onAdded(track: AudioTrackContext, silent: Boolean)

    fun addAll(tracks: Collection<AudioTrackContext>, silent: Boolean = false) {
        if (tracks.all { it.isPriority }) queue.addAllFirst(tracks) else queue.addAll(tracks)

        onListAdded(tracks, silent)
    }

    fun onListAdded(tracks: Collection<AudioTrackContext>, silent: Boolean)

    fun remove(track: AudioTrackContext, silent: Boolean = false) = queue.remove(track).also { onRemove(track, silent) }

    fun onRemove(track: AudioTrackContext, silent: Boolean)

    fun removeAll(tracks: Collection<AudioTrackContext>, silent: Boolean = false) = queue.removeAll(tracks).also { onListRemoved(tracks, silent) }

    fun removeById(trackIds: Collection<ObjectId>, silent: Boolean = false) = queue.removeAll(queue.filter { trackIds.contains(it.trackId) }.also { onListRemoved(it, silent) })

    fun onListRemoved(tracks: Collection<AudioTrackContext>, silent: Boolean)

    fun clear() = queue.clear()

    fun onSkipped()

    fun isUserTrackOwner(userId: Long, trackIds: Collection<ObjectId>): Boolean = queue.filter { it.userId == userId }.all { trackIds.contains(it.trackId) }
}

fun ConcurrentLinkedDeque<AudioTrackContext>.addAllFirst(tracks: Collection<AudioTrackContext>) {
    tracks.reversed().forEach { addFirst(it) }
}

