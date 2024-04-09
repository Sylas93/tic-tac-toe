package com.brack.memorygame.gameplay

import com.brack.memorygame.gameplay.CellOwner.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.springframework.util.ReflectionUtils
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

class GameSessionTest {

    @Test
    @DisplayName("Should return no winner when gameSession is new")
    fun checkWinnerForNewSession() {
        val gameSession = GameSession()
        assertEquals(NONE, gameSession.checkWinner())
    }

    @Test
    @DisplayName("Should throw exception when moves count is not balanced")
    fun checkMovesCount() {
        val gameSession = GameSession().given(
            mutableListOf(
                PLAYER_A, PLAYER_B, NONE,
                PLAYER_B, NONE, NONE,
                PLAYER_A, PLAYER_B, PLAYER_B
            )
        )
        assertThrows(IllegalStateException::class.java) { gameSession.checkWinner() }
    }

    @Test
    @DisplayName("Should throw exception when multiple winners")
    fun checkMultipleWinners() {
        val gameSession = GameSession().given(
            mutableListOf(
                PLAYER_A, PLAYER_B, NONE,
                PLAYER_A, PLAYER_B, NONE,
                PLAYER_A, PLAYER_B, PLAYER_B
            )
        )
        assertThrows(IllegalStateException::class.java) { gameSession.checkWinner() }
    }

    @Test
    @DisplayName("Should return a winner for column tris")
    fun checkWinnerForColumns() {
        val gameSession1 = GameSession().given(
            mutableListOf(
                PLAYER_A, PLAYER_B, NONE,
                PLAYER_A, PLAYER_B, NONE,
                PLAYER_A, NONE, PLAYER_B
            )
        )
        assertEquals(PLAYER_A, gameSession1.checkWinner())

        val gameSession2 =GameSession().given(
            mutableListOf(
                PLAYER_A, PLAYER_B, PLAYER_A,
                PLAYER_B, PLAYER_B, NONE,
                PLAYER_A, PLAYER_B, PLAYER_A
            )
        )
        assertEquals(PLAYER_B, gameSession2.checkWinner())
    }

    @Test
    @DisplayName("Should return a winner for row tris")
    fun checkWinnerForRows() {
        val gameSession1 = GameSession().given(
            mutableListOf(
                PLAYER_A, PLAYER_A, PLAYER_A,
                PLAYER_B, PLAYER_B, NONE,
                PLAYER_A, NONE, PLAYER_B
            )
        )
        assertEquals(PLAYER_A, gameSession1.checkWinner())

        val gameSession2 = GameSession().given(
            mutableListOf(
                NONE, PLAYER_A, PLAYER_A,
                PLAYER_B, PLAYER_B, PLAYER_B,
                PLAYER_A, NONE, PLAYER_B
            )
        )
        assertEquals(PLAYER_B, gameSession2.checkWinner())
    }

    @Test
    @DisplayName("Should return a winner for major diagonal")
    fun checkWinnerForMajorDiagonal() {
        val gameSession1 = GameSession().given(
            mutableListOf(
                PLAYER_A, NONE, NONE,
                PLAYER_B, PLAYER_A, NONE,
                PLAYER_B, NONE, PLAYER_A
            )
        )
        assertEquals(PLAYER_A, gameSession1.checkWinner())
    }

    @Test
    @DisplayName("Should return a winner for minor diagonal")
    fun checkWinnerForMinorDiagonal() {
        val gameSession1 = GameSession().given(
            mutableListOf(
                PLAYER_A, NONE, PLAYER_B,
                PLAYER_A, PLAYER_B, NONE,
                PLAYER_B, NONE, PLAYER_A
            )
        )
        assertEquals(PLAYER_B, gameSession1.checkWinner())
    }
}

private fun GameSession.given(board: MutableList<CellOwner>) = also {
    val boardField = GameSession::class.memberProperties.first { it.name == "cells" }
    boardField.javaField?.let {
        it.trySetAccessible()
        ReflectionUtils.setField(it, this, board)
    }
}
