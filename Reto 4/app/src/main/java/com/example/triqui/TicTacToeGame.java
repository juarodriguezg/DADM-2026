package com.example.triqui;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Lógica del triqui: tablero, jugadas del Android y detección de ganador. */
public class TicTacToeGame {

    public static final char HUMAN_PLAYER = 'X';
    public static final char COMPUTER_PLAYER = 'O';
    public static final char OPEN_SPOT = ' ';
    public static final int BOARD_SIZE = 9;

    // Resultados de checkForWinner()
    public static final int NO_WINNER = 0;
    public static final int TIE = 1;
    public static final int HUMAN_WINS = 2;
    public static final int COMPUTER_WINS = 3;

    /** Niveles de dificultad del Android. */
    public enum DifficultyLevel { Easy, Harder, Expert }

    private static final int[][] WINNING_LINES = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
            {0, 4, 8}, {2, 4, 6}
    };

    private final char[] mBoard = new char[BOARD_SIZE];
    private final Random mRandom = new Random();

    // Nivel de dificultad actual
    private DifficultyLevel mDifficultyLevel = DifficultyLevel.Expert;

    public TicTacToeGame() {
        clearBoard();
    }

    public DifficultyLevel getDifficultyLevel() {
        return mDifficultyLevel;
    }

    public void setDifficultyLevel(DifficultyLevel difficultyLevel) {
        mDifficultyLevel = difficultyLevel;
    }

    public void clearBoard() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            mBoard[i] = OPEN_SPOT;
        }
    }

    public char getBoardOccupant(int location) {
        return mBoard[location];
    }

    public char[] getBoardState() {
        return mBoard.clone();
    }

    public void setBoardState(char[] board) {
        System.arraycopy(board, 0, mBoard, 0, BOARD_SIZE);
    }

    public void setMove(char player, int location) {
        if (mBoard[location] == OPEN_SPOT) {
            mBoard[location] = player;
        }
    }

    /** Devuelve la casilla donde el Android debe jugar, según la dificultad. */
    public int getComputerMove() {
        int move = -1;

        if (mDifficultyLevel == DifficultyLevel.Easy) {
            move = getRandomMove();
        } else if (mDifficultyLevel == DifficultyLevel.Harder) {
            move = getWinningMove();
            if (move == -1) {
                move = getRandomMove();
            }
        } else if (mDifficultyLevel == DifficultyLevel.Expert) {
            // Intenta ganar; si no puede, bloquea; si tampoco, juega al azar.
            move = getWinningMove();
            if (move == -1) {
                move = getBlockingMove();
            }
            if (move == -1) {
                move = getRandomMove();
            }
        }

        return move;
    }

    /** Casilla libre al azar, o -1 si el tablero está lleno. */
    public int getRandomMove() {
        List<Integer> open = new ArrayList<>();
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (mBoard[i] == OPEN_SPOT) {
                open.add(i);
            }
        }
        if (open.isEmpty()) {
            return -1;
        }
        return open.get(mRandom.nextInt(open.size()));
    }

    /** Casilla con la que el Android gana de inmediato, o -1. */
    public int getWinningMove() {
        return findMoveFor(COMPUTER_PLAYER, COMPUTER_WINS);
    }

    /** Casilla con la que el humano ganaría (hay que bloquearla), o -1. */
    public int getBlockingMove() {
        return findMoveFor(HUMAN_PLAYER, HUMAN_WINS);
    }

    /**
     * Prueba cada casilla libre con la ficha indicada y devuelve la primera que produce
     * el resultado buscado. El tablero siempre queda como estaba.
     */
    private int findMoveFor(char player, int winningResult) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (mBoard[i] == OPEN_SPOT) {
                mBoard[i] = player;
                boolean wins = checkForWinner() == winningResult;
                mBoard[i] = OPEN_SPOT;
                if (wins) {
                    return i;
                }
            }
        }
        return -1;
    }

    /** 0 = sin ganador aún, 1 = empate, 2 = ganó el humano (X), 3 = ganó el Android (O). */
    public int checkForWinner() {
        for (int[] line : WINNING_LINES) {
            char a = mBoard[line[0]];
            if (a != OPEN_SPOT && a == mBoard[line[1]] && a == mBoard[line[2]]) {
                return (a == HUMAN_PLAYER) ? HUMAN_WINS : COMPUTER_WINS;
            }
        }
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (mBoard[i] == OPEN_SPOT) {
                return NO_WINNER;
            }
        }
        return TIE;
    }
}
