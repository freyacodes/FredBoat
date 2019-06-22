package fredboat.audio.queue.handlers

import fredboat.audio.queue.AudioTrackContext
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ThreadLocalRandom

open class ShufflableQueueHandler : SimpleQueueHandler(), IShufflableQueueHandler {

    private var _shuffleQueue: ConcurrentLinkedDeque<AudioTrackContext> = ConcurrentLinkedDeque()
    override var shuffleQueue: ConcurrentLinkedDeque<AudioTrackContext>
        get() {
            if (size != _shuffleQueue.size) reshuffle()
            return _shuffleQueue
        }
        set(value) {
            _shuffleQueue = value
        }

    private var _shuffle = false
    override var shuffle: Boolean
        get() = _shuffle
        set(value) {
            _shuffle = value
            reshuffle()
        }

    /**
     * reshuffle all the rand values of the [queue] and pass them to [shuffleQueue] in sorted order
     */
    override fun reshuffle() {
        // if shuffle is disabled do nothing
        if (!shuffle) {
            shuffleQueue = queue
            return
        }

        // Get a list of the original queue
        val cached = ArrayList<AudioTrackContext>()
        cached.addAll(queue.toList())

        // for each entry randomize 'rand' (This will update the track directly thus updating both lists)
        for (track in cached) {
            if (!track.isPriority) track.rand = ThreadLocalRandom.current().nextInt()
        }

        // sort our locally cached list by the shuffled rand values and update our cachedQueue
        cached.sort()
        shuffleQueue = ConcurrentLinkedDeque(cached)
    }

    override fun take(silent: Boolean): AudioTrackContext? {
        if (!shuffle)
            return super<SimpleQueueHandler>.take(silent)

        return shuffleQueue.poll()?.also { super.queue.remove(it); onTaken(it, silent) }
    }

    override fun getInRange(start: Int, end: Int): Collection<AudioTrackContext> {
        if (!shuffle)
            return super<SimpleQueueHandler>.getInRange(start, end)

        return shuffleQueue.filterIndexed { i, _ -> i in start..end }
    }

    override fun onAdded(track: AudioTrackContext, silent: Boolean) {
        super.onAdded(track, silent)

        if (!silent) reshuffle()
    }

    override fun onListAdded(tracks: Collection<AudioTrackContext>, silent: Boolean) {
        super.onListAdded(tracks, silent)

        if (!silent) reshuffle()
    }

    override fun onRemove(track: AudioTrackContext, silent: Boolean) {
        super.onRemove(track, silent)

        if (!silent) reshuffle()
    }

    override fun onListRemoved(tracks: Collection<AudioTrackContext>, silent: Boolean) {
        super.onListRemoved(tracks, silent)

        if (!silent) reshuffle()
    }

    override fun clear() {
        super<SimpleQueueHandler>.clear()

        shuffleQueue.clear()
    }
}