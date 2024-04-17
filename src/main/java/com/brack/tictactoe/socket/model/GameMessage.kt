package com.brack.tictactoe.socket.model

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
    fun write(): String = mapper.writeValueAsString(this)

    @JsonIgnore
    fun isLast(): Boolean = this.type == MessageType.END

    companion object {
        private const val PLAY_AGAIN_TEXT = "<br><br>Tap here to play again!"

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
        val LOST_MESSAGE = GameMessage("You lost!$PLAY_AGAIN_TEXT", MessageType.END)
        val WIN_MESSAGE = GameMessage("You won!$PLAY_AGAIN_TEXT", MessageType.END)
        val TIE_MESSAGE = GameMessage("Tie!$PLAY_AGAIN_TEXT", MessageType.END)
        val WITHDRAWAL_MESSAGE = GameMessage(
            "Your opponent left the game!$PLAY_AGAIN_TEXT",
            MessageType.END
        )
        val X_FIGURE_MESSAGE = GameMessage("x-cell", MessageType.FIGURE)
        val O_FIGURE_MESSAGE = GameMessage("o-cell", MessageType.FIGURE)
    }
}

