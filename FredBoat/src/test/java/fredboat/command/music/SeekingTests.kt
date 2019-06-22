package fredboat.command.music

import fredboat.audio.player.GuildPlayer
import fredboat.audio.player.PlayerRegistry
import fredboat.command.music.control.PlayCommandTest
import fredboat.testutil.IntegrationTest
import fredboat.testutil.RetryableTest
import fredboat.testutil.sentinel.SentinelState.joinChannel
import fredboat.testutil.sentinel.delayUntil
import fredboat.testutil.sentinel.delayedAssertEquals
import fredboat.testutil.util.cachedGuild
import fredboat.testutil.util.queue
import org.junit.Assert.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SeekingTests(private val players: PlayerRegistry) : IntegrationTest(), RetryableTest {
    private lateinit var player: GuildPlayer

    private var position: Int // Int because of assertions errors
        get() = player.position.toInt()
        set(pos) {
            player.seekTo(pos.toLong())
            delayUntil { position == pos }
        }

    override fun beforeRetry() = beforeEach()

    @Suppress("MemberVisibilityCanBePrivate")
    @BeforeEach
    fun beforeEach() {
        joinChannel()
        players.destroyPlayer(cachedGuild)
        player = players.getOrCreate(cachedGuild).block()!!
        cachedGuild.queue(PlayCommandTest.url)
        delayUntil { player.playingTrack != null }
        assertNotNull(player.playingTrack)
        player.setPause(true)
        position = 0
    }

    @Test
    fun forward() = retryable {
        testCommand(";;forward 5") { delayedAssertEquals(expected = 5000) { position } }
        testCommand(";;forward 5:6") { delayedAssertEquals(expected = 5000 + 6000 + 60000 * 5) { position } }
        position = 0
        testCommand(";;forward 1:0:0") { delayedAssertEquals(expected = 60 * 60 * 1000) { position } }
    }

    @Test
    fun restart() = retryable {
        position = 1000
        testCommand(";;restart") { delayedAssertEquals(expected = 0) { position } }
    }

    @Test
    fun rewind() = retryable {
        AssertionError("")
        position = 60000
        testCommand(";;rewind 5") { delayedAssertEquals(expected = 55000) { position } }
        testCommand(";;rewind 60") { delayedAssertEquals(expected = 0) { position } }
        position = 60*60*1000
        testCommand(";;rewind 5:0") { delayedAssertEquals(expected = 55*60*1000) { position } }
        position = 60*60*1000 + 5000
        testCommand(";;rewind 1:0:4") { delayedAssertEquals(expected = 1000) { position } }
    }

    @Test
    fun seek() = retryable {
        val tests = mapOf(
                "5" to 5 * 1000,
                "5:5" to (5*60+5) * 1000,
                "50:50" to (50*60+50) * 1000,
                "1:50:50" to (50*60+50) * 1000 + 60*60*1000
        )

        tests.forEach { arg, exp ->
            testCommand(";;seek $arg") { delayedAssertEquals(expected = exp) { position } }
        }
    }

}