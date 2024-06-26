package com.brack.tictactoe.socket

import com.brack.tictactoe.gameplay.CellOwner
import com.brack.tictactoe.gameplay.GameBoard
import com.brack.tictactoe.socket.model.GameMessage
import com.brack.tictactoe.socket.model.GameMessage.Companion.LOST_MESSAGE
import com.brack.tictactoe.socket.model.GameMessage.Companion.OPPONENT_TURN_MESSAGE
import com.brack.tictactoe.socket.model.GameMessage.Companion.O_FIGURE_MESSAGE
import com.brack.tictactoe.socket.model.GameMessage.Companion.TIE_MESSAGE
import com.brack.tictactoe.socket.model.GameMessage.Companion.WAITING_MESSAGE
import com.brack.tictactoe.socket.model.GameMessage.Companion.WIN_MESSAGE
import com.brack.tictactoe.socket.model.GameMessage.Companion.WITHDRAWAL_MESSAGE
import com.brack.tictactoe.socket.model.GameMessage.Companion.X_FIGURE_MESSAGE
import com.brack.tictactoe.socket.model.GameMessage.Companion.YOUR_TURN_MESSAGE
import com.brack.tictactoe.socket.model.MessageType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.asFlux
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

private val logger: Logger = LoggerFactory.getLogger(GameSession::class.java)

class GameSession {
    private var turn = CellOwner.PLAYER_A
    private val gameBoard = GameBoard()
    private val sinkA: Sinks.Many<GameMessage> = Sinks.many().unicast().onBackpressureBuffer()
    private val sinkB: Sinks.Many<GameMessage> = Sinks.many().unicast().onBackpressureBuffer()
    private var phase: GameSessionPhase = GameSessionPhase.LOBBY

    init {
        sinkA.tryEmitNext(WAITING_MESSAGE)
    }

    companion object Manager {
        private val gameSessions = mutableListOf<GameSession>()

        init {
            CoroutineScope(Dispatchers.Default).launch {
                while (true) {
                    delay(10000)
                    gameSessions.removeAll {
                        it.phase == GameSessionPhase.CLOSED
                    }
                    logger.info("Active games: ${gameSessions.size}")
                }
            }
        }

        @JvmStatic
        fun feedback(playerInput: Flux<GameMessage>): Flux<GameMessage> {
            return gameSessions.firstOrNull { it.phase == GameSessionPhase.LOBBY }
                ?.also { it.startGame() }
                ?.playerFeedback(CellOwner.PLAYER_B, playerInput)
                ?: GameSession()
                    .also { gameSessions.add(it) }
                    .playerFeedback(CellOwner.PLAYER_A, playerInput)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun playerFeedback(player: CellOwner, playerInput: Flux<GameMessage>): Flux<GameMessage> =
        playerInput.asFlow().flatMapConcat { gameMessage ->
            if (updateBoard(player, gameMessage)) {
                val figureMessage = if (player == CellOwner.PLAYER_A) X_FIGURE_MESSAGE else O_FIGURE_MESSAGE
                val showMessage = GameMessage(gameMessage.text, MessageType.SHOW)
                when (gameBoard.checkWinner()) {
                    CellOwner.NONE -> {
                        turn = turn.opponent()
                        arrayOf(figureMessage, showMessage)
                            .also { opponentSink(player).emit(*it, YOUR_TURN_MESSAGE) }
                            .let { flowOf(*it, OPPONENT_TURN_MESSAGE) }
                    }

                    player -> {
                        phase = GameSessionPhase.CLOSED
                        arrayOf(figureMessage, showMessage)
                            .also { opponentSink(player).emit(*it, LOST_MESSAGE) }
                            .let { flowOf(*it, WIN_MESSAGE) }
                    }

                    null -> {
                        phase = GameSessionPhase.CLOSED
                        arrayOf(figureMessage, showMessage, TIE_MESSAGE)
                            .also { opponentSink(player).emit(*it) }
                            .let { flowOf(*it) }
                    }

                    else -> throw IllegalStateException("Player cannot make opponent win")
                }
            } else {
                emptyFlow()
            }
        }.asFlux()
            .mergeWith(sink(player).asFlux())
            .doOnCancel {
                if (phase == GameSessionPhase.CLOSED) {
                    logger.info("server flux cancelled")
                } else {
                    logger.warn("Player let game before end")
                    phase = GameSessionPhase.CLOSED
                    opponentSink(player).emit(WITHDRAWAL_MESSAGE)
                }
            }
            .doOnError { logger.error("Error: ${it.message}") }
            .takeUntil(GameMessage::isLast)

    private fun updateBoard(player: CellOwner, gameMessage: GameMessage) =
        phase == GameSessionPhase.PLAYING &&
            turn == player &&
            gameMessage.type == MessageType.CLIENT_CLICK &&
            gameBoard.updateCell(gameMessage.text.toInt(), player)

    private fun sink(player: CellOwner) =
        if (player == CellOwner.PLAYER_A) {
            sinkA
        } else {
            sinkB
        }

    private fun opponentSink(player: CellOwner) =
        if (player == CellOwner.PLAYER_A) sinkB else sinkA

    private fun startGame() {
        phase = GameSessionPhase.PLAYING
        sinkA.tryEmitNext(YOUR_TURN_MESSAGE)
        sinkB.tryEmitNext(OPPONENT_TURN_MESSAGE)
    }

    private enum class GameSessionPhase {
        LOBBY,
        PLAYING,
        CLOSED
    }
}

fun Sinks.Many<GameMessage>.emit(vararg message: GameMessage) =
    message.map { tryEmitNext(it) }
        .filterNot { it == Sinks.EmitResult.OK }
        .forEach { logger.error("Error emitting from sink: $it") }
