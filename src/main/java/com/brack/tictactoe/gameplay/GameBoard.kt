package com.brack.tictactoe.gameplay

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

class GameBoard {
    private val cells: MutableList<CellOwner> = MutableList(9) { CellOwner.NONE }

    /**
     * @param index the cell to update
     * @param owner the [player][CellOwner] making the play
     *
     * @return true for success update when the cell has [no previous owner][CellOwner.NONE], false otherwise
     */
    fun updateCell(index: Int, owner: CellOwner) =
        cells[index].takeIf { it == CellOwner.NONE }?.let {
            cells[index] = owner
            true
        } ?: false

    /**
     * @return
     * * [CellOwner.NONE] if there is no winner yet
     * * [CellOwner.PLAYER_A] if player A won
     * * [CellOwner.PLAYER_B] if player B won
     * * `null` after game end with a tie
     */
    suspend fun checkWinner(): CellOwner? =
        withContext(Dispatchers.Default) {
            checkBoardHealth()
            listOf(
                async { lineCheck(Int::rem) }, // columns
                async { lineCheck(Int::div) }, // rows
                async { // major diagonal
                    diagonalCheck { index, value ->
                        index % 4 == 0 && value != CellOwner.NONE
                    }
                },
                async { // minor diagonal
                    diagonalCheck { index, value ->
                        index != 0 && index != 8 && value != CellOwner.NONE && index % 2 == 0
                    }
                }
            ).awaitAll()
        }
            .filterNotNull()
            .distinct().also { check(it.size <= 1) { "Session corrupted: multiple winners" } }
            .firstOrNull() ?: cells.firstOrNull { it == CellOwner.NONE }

    private fun checkBoardHealth() {
        val movesCountDiff = cells.count { it == CellOwner.PLAYER_A } -
            cells.count { it == CellOwner.PLAYER_B }
        check(movesCountDiff.absoluteValue < 2) { "Session corrupted: to many moves from one player" }
    }

    private fun diagonalCheck(filter: (Int, CellOwner) -> Boolean) =
        cells.filterIndexed(filter)
            .groupBy { it }
            .filter { it.value.size == 3 }
            .keys.firstOrNull()

    private fun lineCheck(transform: Int.(Int) -> Int) =
        cells.mapIndexed { index, cellOwner ->
            IndexedValue(index.transform(3), cellOwner)
        }
            .filter { it.value != CellOwner.NONE }
            .groupBy { it }
            .filterValues { it.size == 3 }
            .keys.also { check(it.size <= 1) { "Session corrupted: multiple winners" } }
            .firstOrNull()?.value

}
