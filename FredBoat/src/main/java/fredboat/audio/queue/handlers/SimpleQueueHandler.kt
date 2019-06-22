package fredboat.audio.queue.handlers

import fredboat.audio.queue.AudioTrackContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedDeque

open class SimpleQueueHandler : IQueueHandler {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SimpleQueueHandler::class.java)
    }

    final override val queue: ConcurrentLinkedDeque<AudioTrackContext> = ConcurrentLinkedDeque()

    override fun onTaken(track: AudioTrackContext, silent: Boolean) {
        log.debug("Track {} was taken from the queue", track.effectiveTitle)
    }

    override fun onAdded(track: AudioTrackContext, silent: Boolean) {
        log.debug("Track {} was added to the queue", track.effectiveTitle)
    }

    override fun onListAdded(tracks: Collection<AudioTrackContext>, silent: Boolean) {
        log.debug("{} tracks were added to the queue", tracks.size)
    }

    override fun onRemove(track: AudioTrackContext, silent: Boolean) {
        log.debug("Track {} was removed from the queue", track.effectiveTitle)
    }

    override fun onListRemoved(tracks: Collection<AudioTrackContext>, silent: Boolean) {
        log.debug("{} tracks were removed from the queue", tracks.size)
    }

    override fun onSkipped() {
        log.debug("The currently playing track was skipped")
    }
}