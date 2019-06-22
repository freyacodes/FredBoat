package fredboat.audio

import fredboat.audio.player.GuildPlayer
import fredboat.audio.player.PlayerRegistry
import fredboat.sentinel.getGuildMono
import fredboat.testutil.IntegrationTest
import fredboat.testutil.sentinel.Raws
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.Mockito
import reactor.test.StepVerifier
import java.util.concurrent.ConcurrentHashMap

class PlayerRegistryTest : IntegrationTest() {

    private val PlayerRegistry.backingRegistry: ConcurrentHashMap<Long, GuildPlayer>
        get() {
            val field = javaClass.getDeclaredField("registry")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            return field.get(this) as ConcurrentHashMap<Long, GuildPlayer>
        }

    @Test
    fun testLazyMono(playerRegistry: PlayerRegistry) {
        val guild = runBlocking {
            getGuildMono(Raws.guild.id).awaitFirst()!!
        }
        val mock = Mockito.mock(GuildPlayer::class.java)
        playerRegistry.backingRegistry[Raws.guild.id] = mock
        StepVerifier.create(playerRegistry.getOrCreate(guild))
                .expectNext(mock)
                .expectComplete()
                .verify()
    }

}