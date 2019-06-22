package fredboat.audio.queue.handlers

import fredboat.audio.player.GuildPlayer
import fredboat.audio.queue.AudioTrackContext
import fredboat.definitions.RepeatMode

open class RepeatableQueueHandler(player: GuildPlayer) : RoundRobinQueueHandler(player), IRepeatableQueueHandler {

    override var lastTrack: AudioTrackContext? = null

    override var repeat: RepeatMode = RepeatMode.OFF

    override fun take(silent: Boolean): AudioTrackContext? {
        if (repeat == RepeatMode.SINGLE && lastTrack != null)
            return lastTrack

        val track = super<RoundRobinQueueHandler>.take(silent)
        if (repeat == RepeatMode.ALL && track != null) {
            track.isPriority = false
            add(track, true)
        }
        lastTrack = track

        return track
    }

    override fun clear() {
        super<RoundRobinQueueHandler>.clear()

        lastTrack = null
    }

    override fun onSkipped() {
        super.onSkipped()

        lastTrack = null
    }
}