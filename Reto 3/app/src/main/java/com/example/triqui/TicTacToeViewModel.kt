package com.example.triqui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class GameState(
    val board: CharArray = CharArray(9) { TicTacToeGame.OPEN_SPOT },
    val status: GameStatus = GameStatus.PLAYER1_TURN,
    val mode: GameMode = GameMode.VS_ANDROID,
    val gameOver: Boolean = false,
    val player1Wins: Int = 0,
    val player2Wins: Int = 0,
    val ties: Int = 0,
    val humanGoesFirstNext: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameState) return false
        return board.contentEquals(other.board) &&
            status == other.status &&
            mode == other.mode &&
            gameOver == other.gameOver &&
            player1Wins == other.player1Wins &&
            player2Wins == other.player2Wins &&
            ties == other.ties &&
            humanGoesFirstNext == other.humanGoesFirstNext
    }

    override fun hashCode(): Int {
        var result = board.contentHashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + mode.hashCode()
        result = 31 * result + gameOver.hashCode()
        result = 31 * result + player1Wins
        result = 31 * result + player2Wins
        result = 31 * result + ties
        result = 31 * result + humanGoesFirstNext.hashCode()
        return result
    }
}

class TicTacToeViewModel : ViewModel() {
    private val game = TicTacToeGame()
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    fun handleClick(location: Int) {
        val s = _state.value
        if (s.gameOver || s.board[location] != TicTacToeGame.OPEN_SPOT) return
        if (s.mode == GameMode.VS_ANDROID && s.status == GameStatus.PLAYER2_TURN) return

        val playerChar = if (s.status == GameStatus.PLAYER1_TURN) TicTacToeGame.HUMAN_PLAYER
                         else TicTacToeGame.COMPUTER_PLAYER
        game.setMove(playerChar, location)
        val newBoard = game.getBoard()

        val winner = game.checkForWinner()
        if (winner != 0) {
            _state.update { it.copy(
                board = newBoard,
                gameOver = true,
                player1Wins = if (winner == 2) it.player1Wins + 1 else it.player1Wins,
                player2Wins = if (winner == 3) it.player2Wins + 1 else it.player2Wins,
                ties = if (winner == 1) it.ties + 1 else it.ties,
                status = when (winner) {
                    1 -> GameStatus.TIE
                    2 -> GameStatus.PLAYER1_WINS
                    else -> GameStatus.PLAYER2_WINS
                }
            ) }
            return
        }

        val nextStatus = if (s.status == GameStatus.PLAYER1_TURN) GameStatus.PLAYER2_TURN
                         else GameStatus.PLAYER1_TURN
        _state.update { it.copy(board = newBoard, status = nextStatus) }

        if (s.mode == GameMode.VS_ANDROID && nextStatus == GameStatus.PLAYER2_TURN) {
            triggerAndroidMove()
        }
    }

    private fun triggerAndroidMove() {
        viewModelScope.launch {
            delay(700L)
            val s = _state.value
            if (s.gameOver || s.status != GameStatus.PLAYER2_TURN) return@launch
            val move = game.getComputerMove()
            game.setMove(TicTacToeGame.COMPUTER_PLAYER, move)
            val newBoard = game.getBoard()
            val winner = game.checkForWinner()
            _state.update { it.copy(
                board = newBoard,
                gameOver = winner != 0,
                player1Wins = if (winner == 2) it.player1Wins + 1 else it.player1Wins,
                player2Wins = if (winner == 3) it.player2Wins + 1 else it.player2Wins,
                ties = if (winner == 1) it.ties + 1 else it.ties,
                status = when (winner) {
                    0 -> GameStatus.PLAYER1_TURN
                    1 -> GameStatus.TIE
                    2 -> GameStatus.PLAYER1_WINS
                    else -> GameStatus.PLAYER2_WINS
                }
            ) }
        }
    }

    fun startNewGame() {
        val s = _state.value
        if (s.mode == GameMode.VS_PLAYER) {
            resetGame(GameStatus.PLAYER1_TURN, s, humanGoesFirstNext = s.humanGoesFirstNext)
        } else {
            val humanFirst = s.humanGoesFirstNext
            resetGame(
                firstStatus = if (humanFirst) GameStatus.PLAYER1_TURN else GameStatus.PLAYER2_TURN,
                s = s,
                humanGoesFirstNext = !humanFirst
            )
        }
    }

    private fun resetGame(firstStatus: GameStatus, s: GameState, humanGoesFirstNext: Boolean) {
        game.clearBoard()
        _state.update { it.copy(
            board = game.getBoard(),
            gameOver = false,
            status = firstStatus,
            humanGoesFirstNext = humanGoesFirstNext
        ) }
        if (s.mode == GameMode.VS_ANDROID && firstStatus == GameStatus.PLAYER2_TURN) {
            triggerAndroidMove()
        }
    }

    fun changeMode(newMode: GameMode) {
        game.clearBoard()
        _state.value = GameState(
            board = game.getBoard(),
            mode = newMode
        )
    }
}
