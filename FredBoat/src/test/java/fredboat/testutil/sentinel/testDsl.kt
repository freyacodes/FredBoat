package fredboat.testutil.sentinel

import com.fredboat.sentinel.SentinelExchanges
import com.fredboat.sentinel.entities.MessageReceivedEvent
import com.fredboat.sentinel.entities.SendMessageRequest
import fredboat.commandmeta.CommandContextParser
import fredboat.commandmeta.abs.CommandContext
import fredboat.sentinel.RawGuild
import fredboat.sentinel.RawMember
import fredboat.sentinel.RawTextChannel
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.lang.Thread.sleep
import java.util.concurrent.TimeoutException
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.reflect

private lateinit var rabbit: RabbitTemplate

@Service
class CommandTester(private val commandContextParser: CommandContextParser, template: RabbitTemplate) {

    init {
        rabbit = template
    }

    fun testCommand(
            message: String,
            guild: RawGuild = Raws.guild,
            channel: RawTextChannel = Raws.generalChannel,
            invoker: RawMember = Raws.owner,
            block: suspend CommandContext.() -> Unit
    ) {
        doTest(parseContext(
                message, guild, channel, invoker
        ), block)
    }

    private fun doTest(context: CommandContext, block: suspend CommandContext.() -> Unit) {
        runBlocking {
            try {
                context.command(context)
                context.apply { block() }
            } catch (e: Exception) {
                throw e
            }
        }
    }

    fun parseContext(
            message: String,
            guild: RawGuild = Raws.guild,
            channel: RawTextChannel = Raws.generalChannel,
            invoker: RawMember = Raws.owner
    ): CommandContext = runBlocking {
        val event = MessageReceivedEvent(
                Math.random().toLong(),
                guild.id,
                channel.id,
                channel.ourEffectivePermissions,
                message,
                invoker.id,
                false,
                emptyList()
        )
        return@runBlocking commandContextParser.parse(event) ?: throw IllegalArgumentException("Unknown command: $message")
    }
}

fun assertReply(testMsg: String = "Assert outgoing message to equal. Message: »{}«", assertion: (String) -> Boolean) {
    val message = SentinelState.poll(SendMessageRequest::class.java)
            ?: throw TimeoutException("Command failed to send message")
    Assert.assertTrue(testMsg.replace("{}", message.toString()), assertion(message.message))
}

fun assertReply(expected: String, testMsg: String = "Assert outgoing message to equal »{}«") {
    val message = (SentinelState.poll(SendMessageRequest::class.java)
            ?: throw TimeoutException("Command failed to send message"))
    Assert.assertEquals(testMsg.replace("{}", expected), expected, message.message)
}

fun assertReplyContains(expected: String, testMsg: String = "Assert outgoing message contains »{}«") {
    val message = (SentinelState.poll(SendMessageRequest::class.java)
            ?: throw TimeoutException("Command failed to send message"))
    Assert.assertTrue(testMsg.replace("{}", expected), message.message.contains(expected))
}

fun <T> assertRequest(testMsg: String = "Failed to assert outgoing request {}", assertion: (T) -> Boolean) {
    val className = assertion.reflect()!!.parameters[0].type.jvmErasure
    val message = (SentinelState.poll(className.java)
            ?: throw TimeoutException("Command failed to send " + className.simpleName))
    @Suppress("UNCHECKED_CAST")
    Assert.assertTrue(testMsg.replace("{}", message.toString()), assertion(message as T))
}

fun CommandContext.invokerSend(message: String) {
    rabbit.convertSendAndReceive(SentinelExchanges.EVENTS, MessageReceivedEvent(
            Math.random().toLong(), // shrug
            guild.id,
            textChannel.id,
            textChannel.ourEffectivePermissions.raw,
            message,
            member.id,
            false,
            emptyList()
    ))
}

fun delayUntil(timeout: Long = 2000, block: () -> Boolean) {
    var timeSpent = 0
    while (!block()) {
        sleep(200)
        timeSpent += 200
        if (timeSpent >= timeout) break
    }
}

fun delayedAssertEquals(
        timeout: Long = 2000,
        message: String = "",
        expected: Any,
        actual: () -> Any
) {
    delayUntil(timeout) { expected == actual() }
    assertEquals(message, expected, actual())
}