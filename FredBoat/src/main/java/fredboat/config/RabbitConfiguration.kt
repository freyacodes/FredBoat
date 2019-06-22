package fredboat.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fredboat.sentinel.SentinelExchanges
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.AmqpRejectAndDontRequeueException
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.AsyncRabbitTemplate
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.interceptor.RetryInterceptorBuilder

@Configuration
class RabbitConfiguration {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RabbitConfiguration::class.java)
    }

    @Bean
    fun jsonMessageConverter(): MessageConverter {
        // We must register this Kotlin module to get deserialization to work with data classes
        val mapper = ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .registerKotlinModule()
        return Jackson2JsonMessageConverter(mapper)
    }

    @Bean
    fun asyncTemplate(rabbitTemplate: RabbitTemplate): AsyncRabbitTemplate {
        return AsyncRabbitTemplate(rabbitTemplate)
    }

    @Bean
    fun eventExchange() = DirectExchange(SentinelExchanges.EVENTS)

    @Bean
    fun eventQueue() = Queue(SentinelExchanges.EVENTS, false)

    @Bean
    fun eventBinding(
            @Qualifier("eventExchange") eventExchange: DirectExchange,
            @Qualifier("eventQueue") eventQueue: Queue
    ): Binding {
        return BindingBuilder.bind(eventQueue).to(eventExchange).with("")
    }

    @Bean
    fun rabbitListenerErrorHandler() = RabbitListenerErrorHandler {
        _, _, t ->
        log.error("Consumer caught exception", t)
        throw AmqpRejectAndDontRequeueException(t)
    }

    /* Don't retry ad infinitum */
    @Bean
    fun retryOperationsInterceptor() = RetryInterceptorBuilder
            .stateful()
            .maxAttempts(3)
            .build()!!
}