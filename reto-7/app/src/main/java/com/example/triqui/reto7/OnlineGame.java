package com.example.triqui.reto7;

/**
 * Partida online tal como se guarda en Firebase Realtime Database, en /games/{gameId}.
 * Firebase la convierte a/desde JSON, por eso los campos son públicos y hay un constructor vacío.
 * El que crea el juego (host) es X y el que se une (invitado) es O.
 */
public class OnlineGame {

    // Valores de status
    public static final String WAITING = "waiting";     // creado, esperando rival
    public static final String PLAYING = "playing";     // los dos jugadores están dentro
    public static final String FINISHED = "finished";   // alguien ganó o hubo empate
    public static final String ABANDONED = "abandoned"; // uno de los dos salió

    public static final String X = String.valueOf(TicTacToeGame.HUMAN_PLAYER);
    public static final String O = String.valueOf(TicTacToeGame.COMPUTER_PLAYER);

    private static final String EMPTY_BOARD = "         "; // 9 casillas libres

    public String hostId;
    public String hostName;
    public String guestId;
    public String guestName;

    /** 9 caracteres: 'X', 'O' o ' '. */
    public String board;
    /** "X" u "O": a quién le toca. */
    public String turn;
    /** Quién empezó la partida actual; en la revancha empieza el otro. */
    public String starter;
    public String status;
    /** Mismos códigos que TicTacToeGame.checkForWinner(). */
    public int winner;
    /** Quién abandonó ("X" u "O"), solo si status == ABANDONED. */
    public String abandonedBy;

    // Marcador acumulado de las revanchas
    public int xWins;
    public int oWins;
    public int ties;

    public long createdAt;

    public OnlineGame() {
        // Requerido por Firebase
    }

    public static OnlineGame create(String hostId, String hostName) {
        OnlineGame game = new OnlineGame();
        game.hostId = hostId;
        game.hostName = hostName;
        game.board = EMPTY_BOARD;
        game.turn = X;
        game.starter = X;
        game.status = WAITING;
        game.winner = TicTacToeGame.NO_WINNER;
        game.createdAt = System.currentTimeMillis();
        return game;
    }

    /** Aplica la jugada si es válida: es el turno de symbol y la casilla está libre. */
    public boolean applyMove(String symbol, int location) {
        if (!PLAYING.equals(status) || !symbol.equals(turn)
                || board.charAt(location) != TicTacToeGame.OPEN_SPOT) {
            return false;
        }

        TicTacToeGame logic = new TicTacToeGame();
        logic.setBoardState(board.toCharArray());
        logic.setMove(symbol.charAt(0), location);
        board = new String(logic.getBoardState());

        winner = logic.checkForWinner();
        if (winner == TicTacToeGame.NO_WINNER) {
            turn = X.equals(turn) ? O : X;
        } else {
            status = FINISHED;
            if (winner == TicTacToeGame.TIE) {
                ties++;
            } else if (winner == TicTacToeGame.HUMAN_WINS) {
                xWins++;
            } else {
                oWins++;
            }
        }
        return true;
    }

    /** Reinicia el tablero para otra partida entre los mismos jugadores. */
    public boolean rematch() {
        if (!FINISHED.equals(status)) {
            return false;
        }
        board = EMPTY_BOARD;
        starter = X.equals(starter) ? O : X;
        turn = starter;
        winner = TicTacToeGame.NO_WINNER;
        status = PLAYING;
        return true;
    }
}
