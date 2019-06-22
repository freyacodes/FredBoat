package fredboat.testutil.util

import fredboat.db.api.GuildSettingsRepository
import fredboat.db.transfer.GuildSettings
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
@Primary
class MockGuildPermsService : GuildSettingsRepository {

    override fun default(id: Long): GuildSettings {
        return GuildSettings(id)
    }

    final val default: (guild: Long) -> GuildSettings = { GuildSettings(it) }
    var factory = default

    override fun fetch(id: Long): Mono<GuildSettings> {
        return Mono.just(factory(id))
    }

    override fun update(mono: Mono<GuildSettings>): Mono<GuildSettings> {
        return mono.flatMap { Mono.just(factory(it.id)) }
    }

    override fun update(target: GuildSettings): Mono<GuildSettings> {
        return Mono.just(target)
    }

    override fun remove(id: Long): Mono<Void> {
        return Mono.empty()
    }
}