package com.example.triqui.reto5;

import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    /** Modos de juego disponibles. */
    private enum GameMode { VS_ANDROID, VS_PLAYER }

    private static final long ANDROID_MOVE_DELAY_MS = 1000L;

    // Claves de onSaveInstanceState
    private static final String KEY_BOARD = "board";
    private static final String KEY_GAME_OVER = "gameOver";
    private static final String KEY_INFO = "info";
    private static final String KEY_P1_SCORE = "p1Score";
    private static final String KEY_P2_SCORE = "p2Score";
    private static final String KEY_TIES = "ties";
    private static final String KEY_MODE = "mode";
    private static final String KEY_P1_TURN = "p1Turn";
    private static final String KEY_GO_FIRST_P1 = "goFirstP1";
    private static final String KEY_DIFFICULTY = "difficulty";

    private TicTacToeGame mGame;
    private BoardView mBoardView;

    private TextView mInfoTextView;
    private TextView mScoreP1TextView;
    private TextView mScoreTiesTextView;
    private TextView mScoreP2TextView;
    private android.widget.Button mModeAndroidButton;
    private android.widget.Button mModePlayerButton;

    private boolean mGameOver;
    private boolean mP1Turn = true;
    private boolean mGoFirstP1 = true;
    private boolean mAndroidThinking;
    private GameMode mMode = GameMode.VS_ANDROID;

    private int mP1Score;
    private int mP2Score;
    private int mTies;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mAndroidMoveRunnable;

    private MediaPlayer mHumanMediaPlayer;
    private MediaPlayer mComputerMediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGame = new TicTacToeGame();

        mInfoTextView = findViewById(R.id.text_info);
        mScoreP1TextView = findViewById(R.id.text_score_p1);
        mScoreTiesTextView = findViewById(R.id.text_score_ties);
        mScoreP2TextView = findViewById(R.id.text_score_p2);

        mBoardView = findViewById(R.id.board);
        mBoardView.setGame(mGame);
        mBoardView.setOnTouchListener(mTouchListener);

        mModeAndroidButton = findViewById(R.id.button_mode_android);
        mModePlayerButton = findViewById(R.id.button_mode_player);
        mModeAndroidButton.setOnClickListener(v -> changeMode(GameMode.VS_ANDROID));
        mModePlayerButton.setOnClickListener(v -> changeMode(GameMode.VS_PLAYER));

        findViewById(R.id.button_new_game).setOnClickListener(v -> startNewGame());
        findViewById(R.id.button_menu).setOnClickListener(this::showOptionsMenu);

        if (savedInstanceState == null) {
            startNewGame();
        } else {
            restoreState(savedInstanceState);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHumanMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.human_move);
        mComputerMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.computer_move);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mHumanMediaPlayer != null) {
            mHumanMediaPlayer.release();
            mHumanMediaPlayer = null;
        }
        if (mComputerMediaPlayer != null) {
            mComputerMediaPlayer.release();
            mComputerMediaPlayer = null;
        }
    }

    // -------------------------------------------------------- Tacto en el tablero

    private final View.OnTouchListener mTouchListener = (v, event) -> {
        if (event.getAction() != MotionEvent.ACTION_DOWN) {
            return false;
        }

        int col = (int) event.getX() / mBoardView.getBoardCellWidth();
        int row = (int) event.getY() / mBoardView.getBoardCellHeight();
        int pos = row * 3 + col;

        if (col < 0 || col > 2 || row < 0 || row > 2) {
            return false;
        }

        handleMove(pos);

        // No necesitamos eventos de movimiento/levantar el dedo
        return false;
    };

    // ---------------------------------------------------------------- Menú

    /** El menú de opciones cuelga del botón de tres puntos, no de una barra de acción. */
    private void showOptionsMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.options_menu, popup.getMenu());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            popup.setForceShowIcon(true);
        }
        popup.setOnMenuItemClickListener(this::onOptionsItemSelected);
        popup.show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.new_game) {
            startNewGame();
            return true;
        } else if (id == R.id.ai_difficulty) {
            showDifficultyDialog();
            return true;
        } else if (id == R.id.about) {
            showAboutDialog();
            return true;
        } else if (id == R.id.quit) {
            showQuitDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ------------------------------------------------------------- Diálogos

    private void showDifficultyDialog() {
        final CharSequence[] levels = {
                getString(R.string.difficulty_easy),
                getString(R.string.difficulty_harder),
                getString(R.string.difficulty_expert)
        };

        // El radio button marcado es el nivel de dificultad vigente.
        int selected = mGame.getDifficultyLevel().ordinal();

        new AlertDialog.Builder(this)
                .setTitle(R.string.difficulty_choose)
                .setSingleChoiceItems(levels, selected, (dialog, item) -> {
                    dialog.dismiss();

                    mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.values()[item]);

                    Toast.makeText(getApplicationContext(), levels[item], Toast.LENGTH_SHORT)
                            .show();
                })
                .show();
    }

    private void showQuitDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.quit_question)
                .setCancelable(false)
                .setPositiveButton(R.string.yes,
                        (DialogInterface dialog, int id) -> MainActivity.this.finish())
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void showAboutDialog() {
        View layout = LayoutInflater.from(this).inflate(R.layout.about_dialog, null);
        new AlertDialog.Builder(this)
                .setView(layout)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    // ----------------------------------------------------------- Partida

    private void startNewGame() {
        cancelPendingAndroidMove();
        mGame.clearBoard();
        mGameOver = false;

        if (mMode == GameMode.VS_PLAYER) {
            // En 2 jugadores siempre empieza el Jugador 1.
            mP1Turn = true;
            updateInfoForTurn();
        } else {
            // Contra el Android se alterna quién empieza cada partida.
            mP1Turn = mGoFirstP1;
            mGoFirstP1 = !mGoFirstP1;
            updateInfoForTurn();
            if (!mP1Turn) {
                scheduleAndroidMove();
            }
        }

        updateScoreboard();
        updateModeButtons();
        mBoardView.invalidate(); // Redibuja el tablero
    }

    private void changeMode(GameMode newMode) {
        if (mMode == newMode) {
            return;
        }
        cancelPendingAndroidMove();
        mMode = newMode;
        // Cambiar de modo reinicia también el marcador.
        mP1Score = 0;
        mP2Score = 0;
        mTies = 0;
        mGoFirstP1 = true;
        startNewGame();
    }

    /** Jugada de la persona que toca en una casilla libre. */
    private void handleMove(int location) {
        if (mGameOver || mAndroidThinking) {
            return;
        }
        if (mGame.getBoardOccupant(location) != TicTacToeGame.OPEN_SPOT) {
            return;
        }

        char player = mP1Turn ? TicTacToeGame.HUMAN_PLAYER : TicTacToeGame.COMPUTER_PLAYER;
        setMove(player, location);

        int winner = mGame.checkForWinner();
        if (winner != TicTacToeGame.NO_WINNER) {
            endGame(winner);
            return;
        }

        mP1Turn = !mP1Turn;
        updateInfoForTurn();

        if (mMode == GameMode.VS_ANDROID && !mP1Turn) {
            scheduleAndroidMove();
        }
    }

    private void scheduleAndroidMove() {
        mAndroidThinking = true;
        mInfoTextView.setText(R.string.turn_android);

        mAndroidMoveRunnable = () -> {
            mAndroidMoveRunnable = null;
            mAndroidThinking = false;
            if (mGameOver) {
                return;
            }

            int move = mGame.getComputerMove();
            if (move != -1) {
                setMove(TicTacToeGame.COMPUTER_PLAYER, move);
            }

            int winner = mGame.checkForWinner();
            if (winner != TicTacToeGame.NO_WINNER) {
                endGame(winner);
                return;
            }

            mP1Turn = true;
            updateInfoForTurn();
        };
        mHandler.postDelayed(mAndroidMoveRunnable, ANDROID_MOVE_DELAY_MS);
    }

    private void cancelPendingAndroidMove() {
        if (mAndroidMoveRunnable != null) {
            mHandler.removeCallbacks(mAndroidMoveRunnable);
            mAndroidMoveRunnable = null;
        }
        mAndroidThinking = false;
    }

    private void setMove(char player, int location) {
        mGame.setMove(player, location);
        mBoardView.invalidate(); // Redibuja el tablero

        if (player == TicTacToeGame.HUMAN_PLAYER) {
            if (mHumanMediaPlayer != null) {
                mHumanMediaPlayer.start();
            }
        } else {
            if (mComputerMediaPlayer != null) {
                mComputerMediaPlayer.start();
            }
        }
    }

    private void endGame(int winner) {
        mGameOver = true;

        if (winner == TicTacToeGame.TIE) {
            mTies++;
            mInfoTextView.setText(R.string.result_tie);
        } else if (winner == TicTacToeGame.HUMAN_WINS) {
            mP1Score++;
            mInfoTextView.setText(mMode == GameMode.VS_ANDROID
                    ? R.string.result_human_wins : R.string.result_player1_wins);
        } else {
            mP2Score++;
            mInfoTextView.setText(mMode == GameMode.VS_ANDROID
                    ? R.string.result_android_wins : R.string.result_player2_wins);
        }

        updateScoreboard();
    }

    // -------------------------------------------------------------- Vistas

    private void updateInfoForTurn() {
        if (mMode == GameMode.VS_ANDROID) {
            mInfoTextView.setText(mP1Turn ? R.string.turn_human : R.string.turn_android);
        } else {
            mInfoTextView.setText(mP1Turn ? R.string.turn_player1 : R.string.turn_player2);
        }
    }

    private void updateScoreboard() {
        String p1Label = getString(mMode == GameMode.VS_ANDROID
                ? R.string.score_human : R.string.score_player1);
        String p2Label = getString(mMode == GameMode.VS_ANDROID
                ? R.string.score_android : R.string.score_player2);

        mScoreP1TextView.setText(p1Label + ": " + mP1Score);
        mScoreTiesTextView.setText(getString(R.string.score_ties) + ": " + mTies);
        mScoreP2TextView.setText(p2Label + ": " + mP2Score);
    }

    private void updateModeButtons() {
        styleModeButton(mModeAndroidButton, mMode == GameMode.VS_ANDROID);
        styleModeButton(mModePlayerButton, mMode == GameMode.VS_PLAYER);
    }

    private void styleModeButton(android.widget.Button button, boolean selected) {
        int background = selected ? R.color.mode_selected : R.color.mode_unselected;
        int text = selected ? R.color.mode_selected_text : R.color.mode_unselected_text;
        button.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, background));
        button.setTextColor(androidx.core.content.ContextCompat.getColor(this, text));
    }

    // ---------------------------------------------------- Ciclo de vida

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharArray(KEY_BOARD, mGame.getBoardState());
        outState.putBoolean(KEY_GAME_OVER, mGameOver);
        outState.putCharSequence(KEY_INFO, mInfoTextView.getText());
        outState.putInt(KEY_P1_SCORE, mP1Score);
        outState.putInt(KEY_P2_SCORE, mP2Score);
        outState.putInt(KEY_TIES, mTies);
        outState.putInt(KEY_MODE, mMode.ordinal());
        outState.putBoolean(KEY_P1_TURN, mP1Turn);
        outState.putBoolean(KEY_GO_FIRST_P1, mGoFirstP1);
        outState.putInt(KEY_DIFFICULTY, mGame.getDifficultyLevel().ordinal());
    }

    private void restoreState(@NonNull Bundle savedInstanceState) {
        char[] board = savedInstanceState.getCharArray(KEY_BOARD);
        if (board != null) {
            mGame.setBoardState(board);
        }
        mGameOver = savedInstanceState.getBoolean(KEY_GAME_OVER);
        mP1Score = savedInstanceState.getInt(KEY_P1_SCORE);
        mP2Score = savedInstanceState.getInt(KEY_P2_SCORE);
        mTies = savedInstanceState.getInt(KEY_TIES);
        mMode = GameMode.values()[savedInstanceState.getInt(KEY_MODE)];
        mP1Turn = savedInstanceState.getBoolean(KEY_P1_TURN);
        mGoFirstP1 = savedInstanceState.getBoolean(KEY_GO_FIRST_P1);
        mGame.setDifficultyLevel(
                TicTacToeGame.DifficultyLevel.values()[savedInstanceState.getInt(KEY_DIFFICULTY)]);

        mBoardView.invalidate();
        updateScoreboard();
        updateModeButtons();
        mInfoTextView.setText(savedInstanceState.getCharSequence(KEY_INFO));

        // Si al girar le tocaba al Android, vuelve a programar su jugada.
        if (!mGameOver && mMode == GameMode.VS_ANDROID && !mP1Turn) {
            scheduleAndroidMove();
        }
    }

    @Override
    protected void onDestroy() {
        cancelPendingAndroidMove();
        super.onDestroy();
    }
}
