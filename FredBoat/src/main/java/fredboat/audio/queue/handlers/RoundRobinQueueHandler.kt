package fredboat.audio.queue.handlers

import com.google.common.collect.Iterators
import fredboat.audio.player.GuildPlayer
import fredboat.audio.queue.AudioTrackContext
import java.util.concurrent.ConcurrentLinkedDeque

open class RoundRobinQueueHandler(private val player: GuildPlayer) : ShufflableQueueHandler(), IRoundRobinQueueHandler {

    private var _roundRobinQueue: ConcurrentLinkedDeque<AudioTrackContext> = ConcurrentLinkedDeque()
    override var roundRobinQueue: ConcurrentLinkedDeque<AudioTrackContext>
        get() {
            if (size != _roundRobinQueue.size) buildRoundRobin() // means we got out of sync. Redo
            return _roundRobinQueue
        }
        set(value) {
            _roundRobinQueue = value
        }

    private var _roundRobin: Boolean = false
    override var roundRobin: Boolean
        get() = _roundRobin
        set(value) {
            _roundRobin = value
            buildRoundRobin()
        }

    private fun buildRoundRobin() {
        // if Round Robin is disabled nothing to do here
        if (!roundRobin) {
            roundRobinQueue = shuffleQueue
            return
        }

        val userQueues = shuffleQueue.groupByTo(mutableMapOf()) { it.userId }
        val cache = ArrayList<AudioTrackContext>()

        var userIterator = Iterators.cycle(userQueues.keys)
        var index: Long? = null

        while (true) {
            val next = index ?: nextUser(userIterator, player.playingTrack?.userId ?: 0, userQueues.size)
            val userList = userQueues[next]
            if (userList != null && !userList.isEmpty()) {
                userList.firstOrNull()?.also { userList.remove(it); cache.add(it) }

                if (userList.isEmpty()) {
                    userQueues.remove(next)
                    userIterator = Iterators.cycle(userQueues.keys)
                }
            }

            if (userQueues.isEmpty()) break

            index = nextUser(userIterator, next, userQueues.size)
        }

        roundRobinQueue = ConcurrentLinkedDeque(cache)
    }

    private fun nextUser(iterator: Iterator<Long>, current: Long, maxRuns: Int): Long {
        var runs = 0

        var next: Long = 0
        while (true) {
            if (!iterator.hasNext())
                break

            next = iterator.next()

            if (next == current || ++runs >= maxRuns) {
                next = iterator.next()
                break
            }
        }

        return next
    }

    override fun take(silent: Boolean): AudioTrackContext? {
        if (!roundRobin)
            return super<ShufflableQueueHandler>.take(silent)

        return roundRobinQueue.poll()?.also { queue.remove(it); onTaken(it, silent) }
    }

    override fun getInRange(start: Int, end: Int): Collection<AudioTrackContext> {
        if (!roundRobin)
            return super<ShufflableQueueHandler>.getInRange(start, end)

        return roundRobinQueue.filterIndexed { i, _ -> i in start..end }
    }

    override fun onAdded(track: AudioTrackContext, silent: Boolean) {
        super.onAdded(track, silent)

        if (!silent) buildRoundRobin()
    }

    override fun onListAdded(tracks: Collection<AudioTrackContext>, silent: Boolean) {
        super.onListAdded(tracks, silent)

        if (!silent) buildRoundRobin()
    }

    override fun onRemove(track: AudioTrackContext, silent: Boolean) {
        super.onRemove(track, silent)

        if (!silent) buildRoundRobin()
    }

    override fun onListRemoved(tracks: Collection<AudioTrackContext>, silent: Boolean) {
        super.onListRemoved(tracks, silent)

        if (!silent) buildRoundRobin()
    }

    override fun clear() {
        super<ShufflableQueueHandler>.clear()

        roundRobinQueue.clear()
    }

    override fun reshuffle() {
        super.reshuffle()

        buildRoundRobin()
    }
}