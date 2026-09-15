package com.example.triqui

import kotlin.random.Random

class TicTacToeGame {
    companion object {
        const val HUMAN_PLAYER = 'X'
        const val COMPUTER_PLAYER = 'O'
        const val OPEN_SPOT = ' '
        const val BOARD_SIZE = 9
    }

    private val board = CharArray(BOARD_SIZE) { OPEN_SPOT }
    private val random = Random.Default

    fun clearBoard() {
        for (i in board.indices) board[i] = OPEN_SPOT
    }

    fun getBoard(): CharArray = board.copyOf()

    fun setMove(player: Char, location: Int) {
        if (board[location] == OPEN_SPOT) {
            board[location] = player
        }
    }

    fun getComputerMove(): Int {
        // Try to win
        for (i in 0 until BOARD_SIZE) {
            if (board[i] == OPEN_SPOT) {
                board[i] = COMPUTER_PLAYER
                if (checkForWinner() == 3) { board[i] = OPEN_SPOT; return i }
                board[i] = OPEN_SPOT
            }
        }
        // Block human from winning
        for (i in 0 until BOARD_SIZE) {
            if (board[i] == OPEN_SPOT) {
                board[i] = HUMAN_PLAYER
                if (checkForWinner() == 2) { board[i] = OPEN_SPOT; return i }
                board[i] = OPEN_SPOT
            }
        }
        // Random open spot
        val open = (0 until BOARD_SIZE).filter { board[it] == OPEN_SPOT }
        return open[random.nextInt(open.size)]
    }

    // Returns: 0 = no winner yet, 1 = tie, 2 = human (X) won, 3 = computer (O) won
    fun checkForWinner(): Int {
        val lines = arrayOf(
            intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8),
            intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8),
            intArrayOf(0, 4, 8), intArrayOf(2, 4, 6)
        )
        for (line in lines) {
            val a = board[line[0]]
            if (a != OPEN_SPOT && a == board[line[1]] && a == board[line[2]]) {
                return if (a == HUMAN_PLAYER) 2 else 3
            }
        }
        return if (board.none { it == OPEN_SPOT }) 1 else 0
    }
}
