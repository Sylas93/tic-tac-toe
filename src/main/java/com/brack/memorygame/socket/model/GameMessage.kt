package com.brack.memorygame.socket.model

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val mapper = jacksonObjectMapper()
val logger: Logger = LoggerFactory.getLogger(GameMessage::class.java)

data class GameMessage(
    val text: String,
    val type: MessageType
) {
    fun write(): String = mapper.writeValueAsString(this)

    companion object {
        @JvmStatic
        fun of(msg: String): GameMessage =
            try {
                mapper.readValue(msg, GameMessage::class.java)
            } catch (e: Exception) {
                logger.error("Error parsing client message")
                GameMessage("Unexpected input", MessageType.ERROR)
            }
    }
}

