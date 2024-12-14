package com.mesh.kabbitMq.builders

import com.mesh.kabbitMq.dsl.KabbitMQDslMarker
import com.mesh.kabbitMq.delegator.Delegator
import com.mesh.kabbitMq.delegator.Delegator.Companion.initialized
import com.mesh.kabbitMq.delegator.Delegator.Companion.stateTrace
import com.mesh.kabbitMq.delegator.Delegator.Companion.withThisRef
import com.rabbitmq.client.*
import kotlinx.serialization.json.Json

@KabbitMQDslMarker
class KabbitMQBasicConsumeBuilder(
    private val channel: Channel,
) {

    var noLocal: Boolean by Delegator()
    var exclusive: Boolean by Delegator()
    var arguments: Map<String, Any> by Delegator()

    var autoAck: Boolean by Delegator()
    var queue: String by Delegator()
    var consumerTag: String by Delegator()

    var deliverCallback: DeliverCallback by Delegator()
    private var cancelCallback: CancelCallback by Delegator()
    private var shutdownSignalCallback: ConsumerShutdownSignalCallback by Delegator()


    init {
        noLocal = false
        exclusive = false
        arguments = emptyMap()

        cancelCallback { tag ->
            println("Consumer with tag: $tag cancelled")
        }
    }

    @KabbitMQDslMarker
    inline fun <reified T> deliverCallback(crossinline callback: (tag: Long, message: T) -> Unit) {
        deliverCallback = DeliverCallback { _, delivery ->
            callback(
                delivery.envelope.deliveryTag,
                Json.decodeFromString<T>(
                    delivery.body.toString(Charsets.UTF_8)
                )
            )
        }
    }

    @KabbitMQDslMarker
    fun cancelCallback(callback: (tag: String) -> Unit) {
        cancelCallback = CancelCallback { consumerTag ->
            callback(consumerTag)
        }
    }

    @KabbitMQDslMarker
    fun shutdownSignalCallback(callback: (tag: String, sig: ShutdownSignalException) -> Unit) {
        shutdownSignalCallback = ConsumerShutdownSignalCallback { consumerTag, sig ->
            callback(consumerTag, sig)
        }
    }

    fun build(): String = withThisRef(this@KabbitMQBasicConsumeBuilder) {
        return@withThisRef when {
            initialized(::consumerTag, ::deliverCallback, ::cancelCallback) -> {
                channel.basicConsume(
                    queue,
                    autoAck,
                    consumerTag,
                    deliverCallback,
                    cancelCallback
                )
            }

            initialized(::consumerTag, ::deliverCallback, ::cancelCallback, ::shutdownSignalCallback) -> {
                channel.basicConsume(
                    queue,
                    autoAck,
                    consumerTag,
                    noLocal,
                    exclusive,
                    arguments,
                    deliverCallback,
                    cancelCallback,
                    shutdownSignalCallback
                )
            }

            initialized(::deliverCallback, ::cancelCallback, ::shutdownSignalCallback) -> {
                channel.basicConsume(
                    queue,
                    autoAck,
                    arguments,
                    deliverCallback,
                    cancelCallback,
                    shutdownSignalCallback
                )
            }

            initialized(::consumerTag, ::deliverCallback, ::shutdownSignalCallback) -> {
                channel.basicConsume(
                    queue,
                    autoAck,
                    consumerTag,
                    deliverCallback,
                    shutdownSignalCallback
                )
            }

            initialized(::deliverCallback, ::shutdownSignalCallback) -> {
                channel.basicConsume(
                    queue,
                    autoAck,
                    arguments,
                    deliverCallback,
                    shutdownSignalCallback
                )
            }

            initialized(::deliverCallback, ::shutdownSignalCallback) -> {
                channel.basicConsume(
                    queue,
                    autoAck,
                    deliverCallback,
                    shutdownSignalCallback
                )
            }

            initialized(::deliverCallback, ::cancelCallback) -> {
                channel.basicConsume(
                    queue,
                    autoAck,
                    deliverCallback,
                    cancelCallback
                )
            }

            initialized(::deliverCallback, ::cancelCallback) -> {
                 channel.basicConsume(
                    queue,
                    autoAck,
                    deliverCallback,
                    cancelCallback
                )
            }

            initialized(::deliverCallback, ::cancelCallback) -> {
                channel.basicConsume(
                    queue,
                    autoAck,
                    arguments,
                    deliverCallback,
                    cancelCallback
                )
            }


            else -> {
                stateTrace().forEach { println(it) }
                error("Unsupported combination of parameters for basicConsume.")
            }
        }
    }
}
