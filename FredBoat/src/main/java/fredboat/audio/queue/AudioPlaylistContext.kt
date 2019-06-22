package fredboat.audio.queue

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import fredboat.sentinel.Member

open class AudioPlaylistContext(track: AudioTrack, member: Member, priority: Boolean = false) : AudioTrackContext(track, member, priority)