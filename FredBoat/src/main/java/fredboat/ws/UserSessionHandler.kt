package fredboat.ws

import com.google.gson.Gson
import fredboat.db.api.GuildSettingsRepository
import fredboat.db.transfer.GuildSettings
import fredboat.sentinel.GuildCache
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import java.lang.Exception
import java.util.concurrent.ConcurrentHashMap

@Controller
class UserSessionHandler(
        val gson: Gson,
        val guildCache: GuildCache,
        val repository: GuildSettingsRepository
) : WebSocketHandler {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(UserSessionHandler::class.java)
    }

    private val sessions = ConcurrentHashMap<Long, MutableList<UserSession>>()

    override fun handle(rawSession: WebSocketSession): Mono<Void> {
        lateinit var interceptMono: Mono<GuildSettings>
        val session = UserSession(rawSession, guildCache, gson) { interceptMono.subscribe() }
        log.info("Established user connection for guild ${session.guildId}")

        interceptMono = repository.fetch(session.guildId)
                .defaultIfEmpty(GuildSettings(session.guildId))
                .doOnError { e ->
                    log.error("Exception while validating privacy setting", e)
                    session.close()
                }.doOnSuccess { settings ->
                    if (settings?.allowPublicPlayerInfo != true) {
                        log.info("Closing $session because webinfo is not enabled")
                        session.close()
                        return@doOnSuccess
                    }

                    log.info("Allowed $session to pass as anonymous view is allowed")
                    sessions.computeIfAbsent(session.guildId) { mutableListOf() }.add(session)
                    val info = session.player?.toPlayerInfo() ?: emptyPlayerInfo
                    session.sendJson(info)
                }

        return rawSession.send(session.sendStream)
                .and(session.receive().doOnNext { handleMessage(session, it) })
                .doFinally {
                    afterConnectionClosed(session)
                }
    }

    fun handleMessage(session: UserSession, msg: WebSocketMessage) {
        log.info("User session: {}", msg.payloadAsText)
    }

    operator fun get(guildId: Long): List<UserSession> = sessions[guildId] ?: emptyList()

    fun afterConnectionClosed(session: UserSession) {
        session.isOpen = false
        val id = session.guildId
        log.info("Disconnected user for guild $id")
        val list = sessions[id] ?: return
        if (list.size == 1) sessions.remove(id)
        else list.remove(session)
    }

    final fun sendLazy(guildId: Long, producer: () -> Any) {
        val sessions = this[guildId]
        if (sessions.isEmpty()) return

        val msg = gson.toJson(producer())
        sessions.forEach {
            log.info("Sending message to ${it.session.id}. Open: ${it.isOpen}")
            //if (it.isOpen) it.send(it.textMessage(msg))
            try {
                it.send(it.textMessage(msg))
            } catch(e: Exception) {
                log.error("Error sending WS message")
            }
        }
    }

}