package fredboat.command.music.control

import com.fredboat.sentinel.entities.AudioQueueRequest
import com.fredboat.sentinel.entities.AudioQueueRequestEnum
import com.fredboat.sentinel.entities.EditMessageRequest
import fredboat.audio.player.PlayerRegistry
import fredboat.audio.player.VideoSelectionCache
import fredboat.sentinel.GuildCache
import fredboat.testutil.IntegrationTest
import fredboat.testutil.RetryableTest
import fredboat.testutil.sentinel.*
import fredboat.testutil.util.cachedGuild
import fredboat.testutil.util.queue
import org.junit.Assert
import org.junit.Assert.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

internal class PlayCommandTest(
        private val players: PlayerRegistry,
        private val guildCache: GuildCache,
        private val selections: VideoSelectionCache
) : IntegrationTest(), RetryableTest {

    companion object {
        const val url = "https://www.youtube.com/watch?v=8EdW28B-In4"
        const val url2 = "https://www.youtube.com/watch?v=pqUuvRkFfLI"
    }

    @Test
    fun notInChannel() {
        testCommand(";;play $url") {
            assertReply("You must join a voice channel first.")
        }
    }
    
    @Test
    fun search(players: PlayerRegistry) = retryable {
        SentinelState.joinChannel()
        var editedMessage = -1L
        var selection: VideoSelectionCache.VideoSelection? = null
        testCommand(";;play demetori") {
            assertReply("Searching YouTube for `demetori`...")
            assertRequest { req: EditMessageRequest ->
                editedMessage = req.messageId
                req.message.startsWith("**Please select a track with the `;;play 1-5` command:**")
            }
            delayUntil { selections[member] != null }
            selection = selections[member]
            assertNotNull(selection)
            assertEquals(selection!!.message, editedMessage)
        }
        testCommand(";;play 5") {
            assertRequest { req: EditMessageRequest ->
                assertEquals(req.messageId, editedMessage)
                req.message.contains("Song **#5** has been selected")
            }
            Assert.assertNull(selections[member])
            delayUntil { players.getExisting(guild) != null }
            assertNotNull(players.getExisting(guild))
            assertEquals(selection!!.choices[4].identifier, players.getExisting(guild)!!.playingTrack?.track?.identifier)
            assertEquals(member, players.getExisting(guild)!!.playingTrack?.member)
        }
    }

    @Test
    fun playUrl(players: PlayerRegistry) {
        SentinelState.joinChannel(channel = Raws.musicChannel)
        testCommand(";;play $url") {
            assertRequest<AudioQueueRequest> { it.channel == Raws.musicChannel.id }
            assertReply { it.contains("Best of Demetori") && it.contains("will now play") }
            assertNotNull(players.getExisting(guild))

            delayUntil { players.getOrCreate(guild).block()!!.playingTrack != null }
            val track = players.getOrCreate(guild).block()?.playingTrack
            assertNotNull(track)
            assertEquals(url, track?.track?.info?.uri)
        }

        testCommand(";;play $url2") {
            assertRequest<AudioQueueRequest> { it.channel == Raws.musicChannel.id }
            assertReply { it.contains("BEGIERDE DES ZAUBERER") && it.contains("has been added to the queue") }

            delayUntil { players.getOrCreate(guild).block()!!.remainingTracks.size == 2 }

            val track = players.getOrCreate(guild).block()?.remainingTracks?.get(1)
            assertNotNull(track)
            assertEquals(url2, track?.track?.info?.uri)

            // Check that the first track is still playing
            assertEquals(url, players.getOrCreate(guild).block()?.playingTrack?.track?.info?.uri)
        }
    }

    @Test
    fun unpause(players: PlayerRegistry) {
        // Setup
        SentinelState.joinChannel()
        SentinelState.joinChannel(Raws.self)
        cachedGuild.queue(url).setPause(true)

        // We should unpause
        testCommand(";;play") {
            delayUntil { players.getExisting(guild)?.isPlaying == true }
            assertFalse("Assert unpaused", players.awaitPlayer(guild).isPaused)
            assertReply("The player will now play.")
        }
    }

    override fun beforeRetry() = beforeEach()

    @Suppress("MemberVisibilityCanBePrivate")
    @BeforeEach
    fun beforeEach() {
        val guild = guildCache.get(SentinelState.guild.id).block(Duration.ofSeconds(5))!!
        val link = guild.existingLink
        players.destroyPlayer(guild)
        SentinelState.reset()

        // Ignore any disconnect requests
        if (link == null) return
        val req = SentinelState.poll(AudioQueueRequest::class.java, timeoutMillis = 2000)
        req?.let { assertEquals(
                "Expected disconnect (if anything at all)",
                AudioQueueRequestEnum.QUEUE_DISCONNECT,
                it.type
        ) }
        selections.videoSelections.invalidateAll()
    }
}