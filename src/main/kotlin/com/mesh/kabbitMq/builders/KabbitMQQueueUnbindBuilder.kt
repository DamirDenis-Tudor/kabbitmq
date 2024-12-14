package com.mesh.kabbitMq.builders

import com.mesh.kabbitMq.dsl.KabbitMQDslMarker
import com.mesh.kabbitMq.delegator.Delegator
import com.mesh.kabbitMq.delegator.Delegator.Companion.initialized
import com.mesh.kabbitMq.delegator.Delegator.Companion.withThisRef
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel

@KabbitMQDslMarker
class KabbitMQQueueUnbindBuilder(private val channel: Channel) {
    var queue: String by Delegator()
    var exchange: String by Delegator()
    var routingKey: String by Delegator()
    var arguments: Map<String, Any> by Delegator()

    fun build(): AMQP.Queue.BindOk = withThisRef(this@KabbitMQQueueUnbindBuilder) {
        return@withThisRef when {
            initialized(::arguments) -> {
                channel.queueBind(queue, exchange, routingKey, arguments)
            }

            else -> {
                channel.queueBind(queue, exchange, routingKey)
            }
        }
    }
}