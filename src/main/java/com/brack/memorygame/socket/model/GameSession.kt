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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.asFlux
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks

private val logger: Logger = LoggerFactory.getLogger(GameSession::class.java)

class GameSession {
    private var turn = CellOwner.PLAYER_A
    private val gameBoard = GameBoard()
    private val sinkA: Sinks.Many<GameMessage> = Sinks.many().unicast().onBackpressureBuffer()
    private val sinkB: Sinks.Many<GameMessage> = Sinks.many().unicast().onBackpressureBuffer()
    private var phase: GameSessionPhase = GameSessionPhase.LOBBY

    init { sinkA.tryEmitNext(WAITING_MESSAGE) }

    fun isLobbyOpen() = phase == GameSessionPhase.LOBBY

    fun startGame() {
        phase = GameSessionPhase.PLAYING
        sinkA.tryEmitNext(YOUR_TURN_MESSAGE)
        sinkB.tryEmitNext(OPPONENT_TURN_MESSAGE)
    }

    fun getSink(player: CellOwner) =
        if (player == CellOwner.PLAYER_A) sinkA else sinkB

    @OptIn(ExperimentalCoroutinesApi::class)
    fun handleMessage(player: CellOwner, gameMessage: GameMessage) : Flux<GameMessage> =
        if (boardUpdate(player, gameMessage)) {
            val figureMessage = if (player == CellOwner.PLAYER_A) X_FIGURE_MESSAGE else O_FIGURE_MESSAGE
            val showMessage = GameMessage(gameMessage.text, MessageType.SHOW)
            Mono.just(gameBoard).asFlow().flatMapConcat {
                when(it.checkWinner()) {
                    CellOwner.NONE -> {
                        with(getOpponentSink(player)){
                            tryEmitNext(figureMessage)
                            tryEmitNext(showMessage)
                            tryEmitNext(YOUR_TURN_MESSAGE)
                        }
                        turn = turn.opponent()
                        flowOf(figureMessage, showMessage, OPPONENT_TURN_MESSAGE)
                    }
                    player -> {
                        phase = GameSessionPhase.CLOSED
                        with(getOpponentSink(player)){
                            tryEmitNext(figureMessage)
                            tryEmitNext(showMessage)
                            tryEmitNext(LOST_MESSAGE)
                        }
                        flowOf(figureMessage, showMessage, WIN_MESSAGE)
                    }
                    null -> {
                        phase = GameSessionPhase.CLOSED
                        with(getOpponentSink(player)){
                            tryEmitNext(figureMessage)
                            tryEmitNext(showMessage)
                            tryEmitNext(TIE_MESSAGE)
                        }
                        flowOf(figureMessage, showMessage, TIE_MESSAGE)
                    }
                    else -> throw IllegalStateException("Player cannot make opponent win")
                }
            }.asFlux()
        } else Flux.empty()

    private fun boardUpdate(player: CellOwner, gameMessage: GameMessage) =
        phase == GameSessionPhase.PLAYING &&
        turn == player &&
        gameMessage.type == MessageType.CLIENT_CLICK &&
        gameBoard.updateCell(gameMessage.text.toInt(), player)

    private fun getOpponentSink(player: CellOwner) =
        if (player == CellOwner.PLAYER_A) sinkB else sinkA

    enum class GameSessionPhase {
        LOBBY,
        PLAYING,
        CLOSED
    }

    companion object {
        private val gameSessions = mutableListOf<GameSession>()

        init {
            CoroutineScope(Dispatchers.Default).launch {
                while (true) {
                    delay(1000)
                    gameSessions.removeAll {
                        it.phase == GameSessionPhase.CLOSED
                    }
                    logger.info("Active games: ${gameSessions.size}")
                }
            }
        }

        @JvmStatic
        fun getSession() = gameSessions
            .firstOrNull(GameSession::isLobbyOpen)
            ?.let {
                it.startGame()
                it
            } ?: (GameSession().also { gameSessions.add(it) })
    }
}
