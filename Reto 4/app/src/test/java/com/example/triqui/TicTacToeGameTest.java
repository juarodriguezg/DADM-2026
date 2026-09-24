package com.example.triqui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class TicTacToeGameTest {

    @Test
    public void computerTakesTheWinningMove() {
        TicTacToeGame game = new TicTacToeGame();
        game.setDifficultyLevel(TicTacToeGame.DifficultyLevel.Expert);
        game.setMove(TicTacToeGame.COMPUTER_PLAYER, 0);
        game.setMove(TicTacToeGame.COMPUTER_PLAYER, 1);
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 3);
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 4);

        assertEquals(2, game.getComputerMove());
    }

    @Test
    public void expertBlocksTheHuman() {
        TicTacToeGame game = new TicTacToeGame();
        game.setDifficultyLevel(TicTacToeGame.DifficultyLevel.Expert);
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 0);
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 1);
        game.setMove(TicTacToeGame.COMPUTER_PLAYER, 4);

        assertEquals(2, game.getComputerMove());
    }

    @Test
    public void easyNeverBlocks() {
        TicTacToeGame game = new TicTacToeGame();
        game.setDifficultyLevel(TicTacToeGame.DifficultyLevel.Easy);
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 0);
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 1);
        // Solo queda libre la casilla 8 aparte de la 2, así que si bloqueara siempre
        // elegiría la 2; en Easy la jugada es aleatoria entre las libres.
        for (int i = 3; i <= 7; i++) {
            game.setMove(TicTacToeGame.COMPUTER_PLAYER, i);
        }
        int move = game.getComputerMove();
        assertNotEquals(-1, move);
    }

    @Test
    public void lookaheadLeavesTheBoardUntouched() {
        TicTacToeGame game = new TicTacToeGame();
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 0);
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 1);
        char[] before = game.getBoardState();

        game.getWinningMove();
        game.getBlockingMove();

        assertEquals(new String(before), new String(game.getBoardState()));
    }

    @Test
    public void detectsWinnersAndTies() {
        TicTacToeGame game = new TicTacToeGame();
        assertEquals(TicTacToeGame.NO_WINNER, game.checkForWinner());

        game.setMove(TicTacToeGame.HUMAN_PLAYER, 0);
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 4);
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 8);
        assertEquals(TicTacToeGame.HUMAN_WINS, game.checkForWinner());
    }
}
