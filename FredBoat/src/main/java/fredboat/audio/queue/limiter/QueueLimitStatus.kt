package fredboat.audio.queue.limiter

import fredboat.audio.queue.AudioTrackContext

class QueueLimitStatus(
        val canQueue: Boolean,
        val atc: AudioTrackContext,
        var errorCode: QueueLimiterEnum = QueueLimiterEnum.SUCCESS,
        var errorMessage: String = "Successful"
)

val List<QueueLimitStatus>.successful get() = filter { it.canQueue }
val List<QueueLimitStatus>.errored get() = filter { !it.canQueue }

val List<QueueLimitStatus>.isPlaylistDisabledError get() = any { it.errorCode == QueueLimiterEnum.PLAYLIST_DISABLED }
val List<QueueLimitStatus>.playlistDisabledError get() = first { it.errorCode == QueueLimiterEnum.PLAYLIST_DISABLED }.errorCode.i18n

// TODO: Naming
enum class QueueLimiterEnum(val i18n: String) {
    PLAYLIST_DISABLED("loadPlaylistDisabled"),
    USER_TRACK_LIMIT_EXCEEDED("loadMaxUserTracksExceeded"),
    TRACK_LIMIT_EXCEEDED("loadMaxTracksExceeded"),
    TRACK_LENGTH_EXCEEDED("loadMaxTrackLengthExceeded"),
    UNKNOWN("loadLimitedUnknown"),
    SUCCESS("")
}