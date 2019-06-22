package fredboat.config

import fredboat.ws.UserSessionHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerAdapter
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import java.util.*



@Configuration
class WebSocketConfig(
        private val userSessionHandler: UserSessionHandler
) {

    @Bean
    fun webSocketHandlerMapping(): HandlerMapping {
        val map = HashMap<String, WebSocketHandler>()
        map["/playerinfo/*"] = userSessionHandler

        val handlerMapping = SimpleUrlHandlerMapping()
        handlerMapping.order = 1
        handlerMapping.urlMap = map
        return handlerMapping
    }

    @Bean
    fun handlerAdapter(): HandlerAdapter {
        return WebSocketHandlerAdapter()
    }
}
