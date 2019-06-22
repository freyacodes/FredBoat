package fredboat.sentinel

import com.fredboat.sentinel.SentinelExchanges
import com.fredboat.sentinel.entities.FredBoatHello
import com.fredboat.sentinel.entities.SentinelHello
import fredboat.agent.FredBoatAgent
import fredboat.agent.HelloSender
import fredboat.config.property.AppConfig
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.AmqpConnectException
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/** Class that tracks Sentinels and their routing keys */
@Service
class SentinelTracker(
        private val appConfig: AppConfig,
        private val rabbit: RabbitTemplate,
        helloSender: HelloSender
) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(SentinelTracker::class.java)
    }

    /** Shard id mapped to [SentinelHello] */
    private val map: ConcurrentHashMap<Int, SentinelHello> = ConcurrentHashMap()
    val sentinels: Set<SentinelHello>
        get() = map.values.toSet()
    private val awaitingMonos = ConcurrentLinkedQueue<Pair<Int, MonoSink<Void>>>()

    init {
        val time = SimpleDateFormat("dd-MM-yyyy-HH:mm:ss").format(Date.from(Instant.now()))
        val id = "FredBoat@$time"

        val task = GlobalScope.launch {
            log.info("Sending FredBoat hello")
            hello(id)
            delay(5000)
            while (map.isEmpty()) {
                log.info("Still haven't received hello from any sentinel. Resending hello...")
                hello(id)
                delay(5000)
            }
        }
        task.invokeOnCompletion {
            helloSender.id = id
            FredBoatAgent.start(helloSender)
        }
    }

    private suspend fun hello(id: String) {
        // This loop finishes only if we are connected
        while (true) {
            try {
                rabbit.convertAndSend(SentinelExchanges.FANOUT, "", FredBoatHello(id, appConfig.status))
                break
            } catch (e: AmqpConnectException) {
                delay(1000)
            }
        }
    }

    fun onHello(hello: SentinelHello) = hello.run {
        log.info("Received hello from $key with shards [$shardStart;$shardEnd] \uD83D\uDC4B")

        if (shardCount != appConfig.shardCount) {
            throw IllegalStateException("Received SentinelHello from $key with shard count $shardCount shards, " +
                    "but we are configured for ${appConfig.shardCount}!")
        }

        asRange.forEach {
            map[it] = hello
        }

        awaitingMonos.removeIf { pair ->
            if (!asRange.contains(pair.first)) return@removeIf false
            pair.second.success()
            true
        }
        Unit
    }

    val SentinelHello.asRange get() = shardStart..shardEnd
    fun getHello(shardId: Int) = map[shardId]
    fun getKey(shardId: Int): String {
        val hello = getHello(shardId)
                ?: throw IllegalStateException("Attempted to access routing key of $shardId," +
                        " but we haven't received hello from it.")
        return hello.key
    }

    fun awaitHello(shardId: Int): Mono<Void> {
        if (map.containsKey(shardId)) return Mono.empty<Void>()
        return Mono.create { sink ->
            // Check again at subscription time just to be sure
            if (map.containsKey(shardId)) sink.success()
            else awaitingMonos.add(shardId to sink)
        }
    }
}