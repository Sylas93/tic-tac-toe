package com.brack.memorygame.gameplay

import kotlinx.coroutines.*
import kotlin.math.absoluteValue

class GameBoard {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val cells: MutableList<CellOwner> = MutableList(9) { CellOwner.NONE }

    fun updateCell(index: Int, owner: CellOwner) =
        cells.firstOrNull { it == CellOwner.NONE }?.let {
            cells[index] = owner
            checkWinner()
        }

    /**
     * @return
     * * [CellOwner.NONE] if there is no winner yet
     * * [CellOwner.PLAYER_A] if player A won
     * * [CellOwner.PLAYER_B] if player B won
     * * `null` after game end
     */
    fun checkWinner() : CellOwner? = runBlocking {
            checkBoardHealth()
            with(coroutineScope) {
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

    private fun lineCheck(transform: Int.(Int)->Int) =
        cells.mapIndexed { index, cellOwner ->
            IndexedValue(index.transform(3), cellOwner)
        }
            .filter { it.value != CellOwner.NONE }
            .groupBy { it }
            .filterValues { it.size == 3 }
            .keys.also { check(it.size <= 1) { "Session corrupted: multiple winners" } }
            .firstOrNull()?.value

}
