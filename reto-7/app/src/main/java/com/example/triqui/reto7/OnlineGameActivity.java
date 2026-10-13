package com.example.triqui.reto7;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Partida online entre dos celulares. El estado vive en Firebase (/games/{gameId}):
 * cada jugada se escribe allá y los dos dispositivos redibujan el tablero cuando cambia.
 */
public class OnlineGameActivity extends AppCompatActivity {

    public static final String EXTRA_GAME_ID = "gameId";

    private DatabaseReference mGameRef;
    private String mPlayerId;

    /** Último estado recibido de Firebase (null hasta el primer evento). */
    private OnlineGame mCurrent;
    private String mMySymbol;

    private boolean mRemoveOnDisconnect;
    private boolean mLeaving;

    // Solo para dibujar: BoardView lee las fichas de un TicTacToeGame
    private final TicTacToeGame mGame = new TicTacToeGame();
    private BoardView mBoardView;

    private TextView mPlayersTextView;
    private TextView mInfoTextView;
    private TextView mScoreXTextView;
    private TextView mScoreTiesTextView;
    private TextView mScoreOTextView;
    private View mRematchButton;

    private MediaPlayer mHumanMediaPlayer;
    private MediaPlayer mComputerMediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_game);

        String gameId = getIntent().getStringExtra(EXTRA_GAME_ID);
        mGameRef = FirebaseDatabase.getInstance().getReference("games").child(gameId);
        mPlayerId = getSharedPreferences(LobbyActivity.PREFS_NAME, MODE_PRIVATE)
                .getString(LobbyActivity.PREF_PLAYER_ID, "");

        mPlayersTextView = findViewById(R.id.text_players);
        mInfoTextView = findViewById(R.id.text_info);
        mScoreXTextView = findViewById(R.id.text_score_x);
        mScoreTiesTextView = findViewById(R.id.text_score_ties);
        mScoreOTextView = findViewById(R.id.text_score_o);

        mBoardView = findViewById(R.id.board);
        mBoardView.setGame(mGame);
        mBoardView.setOnTouchListener(mTouchListener);

        mRematchButton = findViewById(R.id.button_rematch);
        mRematchButton.setOnClickListener(v -> requestRematch());
        findViewById(R.id.button_leave).setOnClickListener(v -> leaveGame());

        // Volver atrás también es salir de la partida (girar la pantalla no).
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                leaveGame();
            }
        });

        mInfoTextView.setText(R.string.online_waiting_rival);
        mRematchButton.setVisibility(View.GONE);
        mGameRef.addValueEventListener(mGameListener);
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

    @Override
    protected void onDestroy() {
        mGameRef.removeEventListener(mGameListener);
        super.onDestroy();
    }

    // ------------------------------------------------- Cambios desde Firebase

    private final ValueEventListener mGameListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            if (mLeaving) {
                return;
            }
            OnlineGame game = snapshot.getValue(OnlineGame.class);
            if (game == null) {
                Toast.makeText(OnlineGameActivity.this, R.string.online_game_deleted,
                        Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            OnlineGame previous = mCurrent;
            mCurrent = game;
            mMySymbol = mPlayerId.equals(game.hostId) ? OnlineGame.X : OnlineGame.O;

            updateOnDisconnect(game);
            playSoundForNewMove(previous, game);

            mGame.setBoardState(game.board.toCharArray());
            mBoardView.invalidate(); // Redibuja el tablero

            updateViews(game);
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            Toast.makeText(OnlineGameActivity.this,
                    getString(R.string.lobby_firebase_error, error.getMessage()),
                    Toast.LENGTH_LONG).show();
        }
    };

    /**
     * Mientras el juego espera rival, si el creador pierde la conexión (cierra la app)
     * Firebase lo borra solo, para que no queden juegos fantasma en la lista.
     */
    private void updateOnDisconnect(OnlineGame game) {
        boolean shouldRemove = OnlineGame.WAITING.equals(game.status)
                && OnlineGame.X.equals(mMySymbol);
        if (shouldRemove && !mRemoveOnDisconnect) {
            mGameRef.onDisconnect().removeValue();
        } else if (!shouldRemove && mRemoveOnDisconnect) {
            mGameRef.onDisconnect().cancel();
        }
        mRemoveOnDisconnect = shouldRemove;
    }

    /** Suena la ficha que acaba de aparecer: la mía con un sonido, la del rival con otro. */
    private void playSoundForNewMove(@Nullable OnlineGame previous, OnlineGame game) {
        if (previous == null) {
            return;
        }
        for (int i = 0; i < TicTacToeGame.BOARD_SIZE; i++) {
            char before = previous.board.charAt(i);
            char now = game.board.charAt(i);
            if (before == TicTacToeGame.OPEN_SPOT && now != TicTacToeGame.OPEN_SPOT) {
                MediaPlayer player = (now == mMySymbol.charAt(0))
                        ? mHumanMediaPlayer : mComputerMediaPlayer;
                if (player != null) {
                    player.start();
                }
                return;
            }
        }
    }

    // ------------------------------------------------------------ Jugadas

    private final View.OnTouchListener mTouchListener = (v, event) -> {
        if (event.getAction() != MotionEvent.ACTION_DOWN) {
            return false;
        }

        int col = (int) event.getX() / mBoardView.getBoardCellWidth();
        int row = (int) event.getY() / mBoardView.getBoardCellHeight();
        if (col < 0 || col > 2 || row < 0 || row > 2) {
            return false;
        }

        makeMove(row * 3 + col);

        // No necesitamos eventos de movimiento/levantar el dedo
        return false;
    };

    /**
     * La jugada va en una transacción: Firebase la aplica sobre el estado más reciente del
     * servidor y OnlineGame.applyMove la rechaza si no es mi turno o la casilla está ocupada.
     */
    private void makeMove(int location) {
        if (mCurrent == null || !OnlineGame.PLAYING.equals(mCurrent.status)
                || !mMySymbol.equals(mCurrent.turn)) {
            return;
        }
        final String symbol = mMySymbol;
        mGameRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                OnlineGame game = currentData.getValue(OnlineGame.class);
                if (game == null) {
                    return Transaction.success(currentData);
                }
                if (!game.applyMove(symbol, location)) {
                    return Transaction.abort();
                }
                currentData.setValue(game);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed,
                                   @Nullable DataSnapshot snapshot) {
                // El listener redibuja; aquí no hay nada más que hacer.
            }
        });
    }

    private void requestRematch() {
        mGameRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                OnlineGame game = currentData.getValue(OnlineGame.class);
                if (game == null) {
                    return Transaction.success(currentData);
                }
                if (!game.rematch()) {
                    return Transaction.abort();
                }
                currentData.setValue(game);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed,
                                   @Nullable DataSnapshot snapshot) {
            }
        });
    }

    /**
     * Si nadie se había unido o el rival ya se fue, el juego se borra.
     * Si el rival sigue dentro, se marca como abandonado para avisarle.
     */
    private void leaveGame() {
        mLeaving = true;
        mGameRef.removeEventListener(mGameListener);

        if (mCurrent != null) {
            if (OnlineGame.WAITING.equals(mCurrent.status)
                    || OnlineGame.ABANDONED.equals(mCurrent.status)) {
                mGameRef.removeValue();
            } else {
                Map<String, Object> update = new HashMap<>();
                update.put("status", OnlineGame.ABANDONED);
                update.put("abandonedBy", mMySymbol);
                mGameRef.updateChildren(update);
            }
        }
        if (mRemoveOnDisconnect) {
            mGameRef.onDisconnect().cancel();
        }
        finish();
    }

    // -------------------------------------------------------------- Vistas

    private void updateViews(OnlineGame game) {
        boolean iAmX = OnlineGame.X.equals(mMySymbol);
        String myName = iAmX ? game.hostName : game.guestName;
        String rivalName = iAmX ? game.guestName : game.hostName;
        String rivalSymbol = iAmX ? OnlineGame.O : OnlineGame.X;
        if (rivalName == null) {
            rivalName = getString(R.string.online_rival_pending);
        }

        mPlayersTextView.setText(getString(R.string.online_players,
                myName, mMySymbol, rivalName, rivalSymbol));

        String xName = game.hostName;
        String oName = game.guestName != null
                ? game.guestName : getString(R.string.online_rival_pending);
        mScoreXTextView.setText(getString(R.string.online_score, xName + " (X)", game.xWins));
        mScoreTiesTextView.setText(getString(R.string.online_score,
                getString(R.string.score_ties), game.ties));
        mScoreOTextView.setText(getString(R.string.online_score, oName + " (O)", game.oWins));

        if (OnlineGame.WAITING.equals(game.status)) {
            mInfoTextView.setText(R.string.online_waiting_rival);
        } else if (OnlineGame.PLAYING.equals(game.status)) {
            mInfoTextView.setText(mMySymbol.equals(game.turn)
                    ? getString(R.string.online_your_turn)
                    : getString(R.string.online_rival_turn, rivalName));
        } else if (OnlineGame.FINISHED.equals(game.status)) {
            if (game.winner == TicTacToeGame.TIE) {
                mInfoTextView.setText(R.string.result_tie);
            } else {
                String winnerSymbol = game.winner == TicTacToeGame.HUMAN_WINS
                        ? OnlineGame.X : OnlineGame.O;
                mInfoTextView.setText(mMySymbol.equals(winnerSymbol)
                        ? getString(R.string.online_you_win)
                        : getString(R.string.online_rival_wins, rivalName));
            }
        } else if (OnlineGame.ABANDONED.equals(game.status)) {
            mInfoTextView.setText(getString(R.string.online_rival_left, rivalName));
        }

        mRematchButton.setVisibility(OnlineGame.FINISHED.equals(game.status)
                ? View.VISIBLE : View.GONE);
    }
}
