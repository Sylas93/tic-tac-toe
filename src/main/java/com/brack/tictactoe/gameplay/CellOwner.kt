package com.brack.tictactoe.gameplay

enum class CellOwner {
    NONE,
    PLAYER_A,
    PLAYER_B;

    fun opponent() = when (this) {
        PLAYER_A -> PLAYER_B
        PLAYER_B -> PLAYER_A
        else -> NONE
    }
}
