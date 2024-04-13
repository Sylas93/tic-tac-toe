package com.brack.memorygame.socket.model

import com.brack.memorygame.gameplay.CellOwner
import com.brack.memorygame.gameplay.GameBoard
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

class GameSession(/*val playerSessionA : String*/) {
    private val gameBoard = GameBoard()
    val sinkA: Sinks.Many<GameMessage> = Sinks.many().unicast().onBackpressureBuffer()
    val sinkB: Sinks.Many<GameMessage> = Sinks.many().unicast().onBackpressureBuffer()
    var gameStarted : Boolean = false

    fun getOpponentSink(player: CellOwner) =
        if (player == CellOwner.PLAYER_A) sinkB else sinkA

    fun getSink(player: CellOwner) =
        if (player == CellOwner.PLAYER_A) sinkA else sinkB

    fun handleMessage(player: CellOwner, gameMessage: GameMessage) : Flux<GameMessage> =
        if (gameStarted && gameMessage.type == MessageType.CLIENT_CLICK) {
            val showFigureMessage = GameMessage(gameMessage.text, MessageType.SHOW_FIGURE)
            gameBoard.updateCell(gameMessage.text.toInt(), player)
            getSink(player).tryEmitNext(showFigureMessage)
            Flux.just(showFigureMessage)
        } else Flux.empty()
    //private lateinit var playerSessionB : String
/*
    fun accept(sessionId: String) =
        if (!this::playerSessionB.isInitialized) {
            playerSessionB = sessionId
            true
        } else {
            false
        }
 */
}
