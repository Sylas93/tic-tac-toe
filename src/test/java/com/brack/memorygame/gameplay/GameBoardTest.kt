package com.brack.memorygame.gameplay

import com.brack.memorygame.gameplay.CellOwner.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.springframework.util.ReflectionUtils
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

class GameBoardTest {

    @Test
    @DisplayName("Should return no winner when gameSession is new")
    fun checkWinnerForNewSession() {
        val gameBoard = GameBoard()
        assertEquals(NONE, gameBoard.checkWinnerBlocking())
    }

    @Test
    @DisplayName("Should return no winner when game is in progress")
    fun checkWinnerForInProgress() {
        val gameBoard = GameBoard().given(
            mutableListOf(
                PLAYER_A, NONE, PLAYER_B,
                PLAYER_B, NONE, NONE,
                PLAYER_A, NONE, PLAYER_B
            )
        )
        assertEquals(NONE, gameBoard.checkWinnerBlocking())
    }

    @Test
    @DisplayName("Should return null when it is a tie")
    fun checkWinnerForTie() {
        val gameBoard = GameBoard().given(
            mutableListOf(
                PLAYER_A, PLAYER_B, PLAYER_B,
                PLAYER_B, PLAYER_A, PLAYER_A,
                PLAYER_A, PLAYER_B, PLAYER_B
            )
        )
        assertNull(gameBoard.checkWinnerBlocking())
    }

    @Test
    @DisplayName("Should throw exception when moves count is not balanced")
    fun checkMovesCount() {
        val gameBoard = GameBoard().given(
            mutableListOf(
                PLAYER_A, PLAYER_B, NONE,
                PLAYER_B, NONE, NONE,
                PLAYER_A, PLAYER_B, PLAYER_B
            )
        )
        assertThrows(IllegalStateException::class.java) { gameBoard.checkWinnerBlocking() }
    }

    @Test
    @DisplayName("Should throw exception when multiple winners")
    fun checkMultipleWinners() {
        val gameBoard = GameBoard().given(
            mutableListOf(
                PLAYER_A, PLAYER_B, NONE,
                PLAYER_A, PLAYER_B, NONE,
                PLAYER_A, PLAYER_B, PLAYER_B
            )
        )
        assertThrows(IllegalStateException::class.java) { gameBoard.checkWinnerBlocking() }
    }

    @Test
    @DisplayName("Should return a winner for column tris")
    fun checkWinnerForColumns() {
        val gameBoard1 = GameBoard().given(
            mutableListOf(
                PLAYER_A, PLAYER_B, NONE,
                PLAYER_A, PLAYER_B, NONE,
                PLAYER_A, NONE, PLAYER_B
            )
        )
        assertEquals(PLAYER_A, gameBoard1.checkWinnerBlocking())

        val gameBoard2 =GameBoard().given(
            mutableListOf(
                PLAYER_A, PLAYER_B, PLAYER_A,
                PLAYER_B, PLAYER_B, NONE,
                PLAYER_A, PLAYER_B, PLAYER_A
            )
        )
        assertEquals(PLAYER_B, gameBoard2.checkWinnerBlocking())
    }

    @Test
    @DisplayName("Should return a winner for row tris")
    fun checkWinnerForRows() {
        val gameBoard1 = GameBoard().given(
            mutableListOf(
                PLAYER_A, PLAYER_A, PLAYER_A,
                PLAYER_B, PLAYER_B, NONE,
                PLAYER_A, NONE, PLAYER_B
            )
        )
        assertEquals(PLAYER_A, gameBoard1.checkWinnerBlocking())

        val gameBoard2 = GameBoard().given(
            mutableListOf(
                NONE, PLAYER_A, PLAYER_A,
                PLAYER_B, PLAYER_B, PLAYER_B,
                PLAYER_A, NONE, PLAYER_B
            )
        )
        assertEquals(PLAYER_B, gameBoard2.checkWinnerBlocking())
    }

    @Test
    @DisplayName("Should return a winner for major diagonal")
    fun checkWinnerForMajorDiagonal() {
        val gameBoard1 = GameBoard().given(
            mutableListOf(
                PLAYER_A, NONE, NONE,
                PLAYER_B, PLAYER_A, NONE,
                PLAYER_B, NONE, PLAYER_A
            )
        )
        assertEquals(PLAYER_A, gameBoard1.checkWinnerBlocking())
    }

    @Test
    @DisplayName("Should return a winner for minor diagonal")
    fun checkWinnerForMinorDiagonal() {
        val gameBoard1 = GameBoard().given(
            mutableListOf(
                PLAYER_A, NONE, PLAYER_B,
                PLAYER_A, PLAYER_B, NONE,
                PLAYER_B, NONE, PLAYER_A
            )
        )
        assertEquals(PLAYER_B, gameBoard1.checkWinnerBlocking())
    }
}

private fun GameBoard.checkWinnerBlocking() = runBlocking { checkWinner() }

private fun GameBoard.given(board: MutableList<CellOwner>) = also {
    val boardField = GameBoard::class.memberProperties.first { it.name == "cells" }
    boardField.javaField?.let {
        it.trySetAccessible()
        ReflectionUtils.setField(it, this, board)
    }
}
