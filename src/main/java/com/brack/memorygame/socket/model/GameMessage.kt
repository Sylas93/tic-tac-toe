package com.brack.memorygame.socket.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val mapper = jacksonObjectMapper()
private val logger: Logger = LoggerFactory.getLogger(GameMessage::class.java)

data class GameMessage(
    val text: String,
    val type: MessageType
) {
    fun write() : String = mapper.writeValueAsString(this)
    @JsonIgnore
    fun isLast() : Boolean = this == WIN_MESSAGE || this == LOST_MESSAGE || this == TIE_MESSAGE

    companion object {
        @JvmStatic
        fun of(msg: String): GameMessage =
            try {
                mapper.readValue(msg, GameMessage::class.java)
            } catch (e: Exception) {
                logger.error("Error parsing client message")
                GameMessage("Unexpected input", MessageType.ERROR)
            }
        val YOUR_TURN_MESSAGE = GameMessage("Your turn!", MessageType.INFO)
        val OPPONENT_TURN_MESSAGE = GameMessage("Opponent turn!", MessageType.INFO)
        val WAITING_MESSAGE = GameMessage("Waiting for opponent", MessageType.INFO)
        val LOST_MESSAGE = GameMessage("You lost!", MessageType.INFO)
        val WIN_MESSAGE = GameMessage("You won!", MessageType.INFO)
        val TIE_MESSAGE = GameMessage("Tie", MessageType.INFO)
        val X_FIGURE_MESSAGE = GameMessage("x-cell", MessageType.FIGURE)
        val O_FIGURE_MESSAGE = GameMessage("o-cell", MessageType.FIGURE)
    }
}

