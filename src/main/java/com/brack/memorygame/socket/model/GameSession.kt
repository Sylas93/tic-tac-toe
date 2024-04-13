package com.brack.memorygame.socket.model

import com.brack.memorygame.gameplay.CellOwner
import com.brack.memorygame.gameplay.GameBoard
import com.brack.memorygame.socket.model.GameMessage.Companion.LOST_MESSAGE
import com.brack.memorygame.socket.model.GameMessage.Companion.OPPONENT_TURN_MESSAGE
import com.brack.memorygame.socket.model.GameMessage.Companion.TIE_MESSAGE
import com.brack.memorygame.socket.model.GameMessage.Companion.WIN_MESSAGE
import com.brack.memorygame.socket.model.GameMessage.Companion.YOUR_TURN_MESSAGE
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

class GameSession {
    private var turn = CellOwner.PLAYER_A
    private val gameBoard = GameBoard()
    private val sinkA: Sinks.Many<GameMessage> = Sinks.many().unicast().onBackpressureBuffer()
    private val sinkB: Sinks.Many<GameMessage> = Sinks.many().unicast().onBackpressureBuffer()
    private var state: GameSessionState = GameSessionState.OPEN

    fun isGameOpen() = state == GameSessionState.OPEN

    fun startGame() {
        state = GameSessionState.PLAYING
    }

    fun getOpponentSink(player: CellOwner) =
        if (player == CellOwner.PLAYER_A) sinkB else sinkA

    fun handleMessage(player: CellOwner, gameMessage: GameMessage) : Flux<GameMessage> =
        if (turn == player && state == GameSessionState.PLAYING && gameMessage.type == MessageType.CLIENT_CLICK) {
            val showFigureMessage = GameMessage(gameMessage.text, MessageType.SHOW_FIGURE)
            when(gameBoard.updateCell(gameMessage.text.toInt(), player)) {
                CellOwner.NONE -> {
                    with(getSink(player)){
                        tryEmitNext(showFigureMessage)
                        tryEmitNext(YOUR_TURN_MESSAGE)
                    }
                    turn = turn.opponent()
                    Flux.just(showFigureMessage, OPPONENT_TURN_MESSAGE)
                }
                player -> {
                    state = GameSessionState.CLOSED
                    with(getSink(player)){
                        tryEmitNext(showFigureMessage)
                        tryEmitNext(LOST_MESSAGE)
                    }
                    Flux.just(showFigureMessage, WIN_MESSAGE)
                }
                null -> {
                    state = GameSessionState.CLOSED
                    with(getSink(player)){
                        tryEmitNext(showFigureMessage)
                        tryEmitNext(TIE_MESSAGE)
                    }
                    Flux.just(showFigureMessage, TIE_MESSAGE)
                }
                else -> throw IllegalStateException("Player cannot make opponent win")
            }
        } else Flux.empty()

    private fun getSink(player: CellOwner) =
        if (player == CellOwner.PLAYER_A) sinkA else sinkB

    enum class GameSessionState {
        OPEN,
        PLAYING,
        CLOSED
    }
}
