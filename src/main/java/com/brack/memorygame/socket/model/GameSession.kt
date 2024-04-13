package com.brack.memorygame.socket.model

import com.brack.memorygame.gameplay.CellOwner
import com.brack.memorygame.gameplay.GameBoard
import com.brack.memorygame.socket.model.GameMessage.Companion.LOST_MESSAGE
import com.brack.memorygame.socket.model.GameMessage.Companion.OPPONENT_TURN_MESSAGE
import com.brack.memorygame.socket.model.GameMessage.Companion.O_FIGURE_MESSAGE
import com.brack.memorygame.socket.model.GameMessage.Companion.TIE_MESSAGE
import com.brack.memorygame.socket.model.GameMessage.Companion.WAITING_MESSAGE
import com.brack.memorygame.socket.model.GameMessage.Companion.WIN_MESSAGE
import com.brack.memorygame.socket.model.GameMessage.Companion.X_FIGURE_MESSAGE
import com.brack.memorygame.socket.model.GameMessage.Companion.YOUR_TURN_MESSAGE
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

class GameSession {
    private var turn = CellOwner.PLAYER_A
    private val gameBoard = GameBoard()
    private val sinkA: Sinks.Many<GameMessage> = Sinks.many().unicast().onBackpressureBuffer()
    private val sinkB: Sinks.Many<GameMessage> = Sinks.many().unicast().onBackpressureBuffer()
    private var state: GameSessionState = GameSessionState.OPEN

    init {
        with(sinkA) {
            tryEmitNext(WAITING_MESSAGE)
        }
    }

    fun isGameOpen() = state == GameSessionState.OPEN

    fun startGame() {
        state = GameSessionState.PLAYING
        sinkA.tryEmitNext(YOUR_TURN_MESSAGE)
        sinkB.tryEmitNext(OPPONENT_TURN_MESSAGE)
    }

    fun getSink(player: CellOwner) =
        if (player == CellOwner.PLAYER_A) sinkA else sinkB

    fun handleMessage(player: CellOwner, gameMessage: GameMessage) : Flux<GameMessage> =
        if (boardUpdate(player, gameMessage)) {
            val figureMessage = if (player == CellOwner.PLAYER_A) X_FIGURE_MESSAGE else O_FIGURE_MESSAGE
            val showMessage = GameMessage(gameMessage.text, MessageType.SHOW)
            when(gameBoard.checkWinner()) {
                CellOwner.NONE -> {
                    with(getOpponentSink(player)){
                        tryEmitNext(figureMessage)
                        tryEmitNext(showMessage)
                        tryEmitNext(YOUR_TURN_MESSAGE)
                    }
                    turn = turn.opponent()
                    Flux.just(figureMessage, showMessage, OPPONENT_TURN_MESSAGE)
                }
                player -> {
                    state = GameSessionState.CLOSED
                    with(getOpponentSink(player)){
                        tryEmitNext(figureMessage)
                        tryEmitNext(showMessage)
                        tryEmitNext(LOST_MESSAGE)
                    }
                    Flux.just(figureMessage, showMessage, WIN_MESSAGE)
                }
                null -> {
                    state = GameSessionState.CLOSED
                    with(getOpponentSink(player)){
                        tryEmitNext(figureMessage)
                        tryEmitNext(showMessage)
                        tryEmitNext(TIE_MESSAGE)
                    }
                    Flux.just(figureMessage, showMessage, TIE_MESSAGE)
                }
                else -> throw IllegalStateException("Player cannot make opponent win")
            }
        } else Flux.empty()

    private fun boardUpdate(player: CellOwner, gameMessage: GameMessage) =
        state == GameSessionState.PLAYING &&
        turn == player &&
        gameMessage.type == MessageType.CLIENT_CLICK &&
        gameBoard.updateCell(gameMessage.text.toInt(), player)

    private fun getOpponentSink(player: CellOwner) =
        if (player == CellOwner.PLAYER_A) sinkB else sinkA

    enum class GameSessionState {
        OPEN,
        PLAYING,
        CLOSED
    }
}
