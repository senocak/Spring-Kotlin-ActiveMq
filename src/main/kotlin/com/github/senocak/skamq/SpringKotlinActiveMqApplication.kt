package com.github.senocak.skamq

import jakarta.jms.BytesMessage
import jakarta.jms.ConnectionFactory
import jakarta.jms.Message
import jakarta.jms.MessageListener
import java.io.IOException
import java.util.UUID
import org.apache.activemq.ActiveMQConnectionFactory
import org.apache.activemq.command.ActiveMQTextMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.jms.annotation.JmsListener
import org.springframework.jms.config.DefaultJmsListenerContainerFactory
import org.springframework.jms.core.JmsTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

fun main(args: Array<String>) {
    runApplication<SpringKotlinActiveMqApplication>(*args)
}

@SpringBootApplication
@RestController
class SpringKotlinActiveMqApplication: MessageListener {
    private val log: Logger = LoggerFactory.getLogger(this::class.java.name)

    @Bean
    fun connectionFactory(): ConnectionFactory =
        ActiveMQConnectionFactory()
            .apply {
                this.brokerURL = brokerUrl
                this.trustedPackages = mutableListOf("com.github.senocak.skamq")
            }

    @Bean
    fun jmsTemplate(): JmsTemplate =
        JmsTemplate()
            .apply {
                this.connectionFactory = connectionFactory()
                this.isPubSubDomain = true // enable for Pub Sub to topic. Not Required for Queue.
            }

    @Bean
    fun jmsListenerContainerFactory(): DefaultJmsListenerContainerFactory =
        DefaultJmsListenerContainerFactory()
            .apply {
                this.setConnectionFactory(connectionFactory())
                this.setPubSubDomain(true)
            }

    private val map = hashMapOf<String, SseEmitter>()

    @GetMapping("/sse")
    fun getMessages(): SseEmitter {
        val randomUUID = "${UUID.randomUUID()}"
        val emitter = SseEmitter()
        map[randomUUID] = emitter
        emitter.onCompletion { map.remove(randomUUID) }
        emitter.onError { log.error("error: ${it.message}"); map.remove(randomUUID) }
        emitter.onTimeout { map.remove(randomUUID) }
        log.info("user [$randomUUID] connected")
        return emitter
    }

    @PostMapping("/jms")
    fun sendMessage(): String =
        try {
            val randomUUID = UUID.randomUUID()
            log.info("Sending message to Topic: $topic, randomUUID: $randomUUID")
            jmsTemplate().convertAndSend(topic, "$randomUUID")
            "$randomUUID"
        } catch (e: java.lang.Exception) {
            log.error("Recieved Exception during send Message: ${e.message}")
            "${e.message}"
        }

    @JmsListener(destination = topic)
    override fun onMessage(message: Message) {
        if (message is ActiveMQTextMessage) {
            val text = message.text
            log.info("Received ActiveMQTextMessage from Topic: $text")
            map.keys.forEach { map[it]?.send(text) }
        }
        else if (message is BytesMessage)
            log.info("Received BytesMessage from Topic: ${message.readBytes(ByteArray(message.bodyLength.toInt()))}")
    }

    companion object {
        private const val brokerUrl = "tcp://localhost:61616"
        private const val topic = "testtopic"
    }
}
