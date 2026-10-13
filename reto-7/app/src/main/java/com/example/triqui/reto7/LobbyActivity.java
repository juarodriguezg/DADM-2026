package com.example.triqui.reto7;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Pantalla inicial: muestra los juegos que esperan rival y permite crear uno o unirse. */
public class LobbyActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "ttt_online_prefs";
    public static final String PREF_PLAYER_ID = "playerId";
    public static final String PREF_PLAYER_NAME = "playerName";

    private DatabaseReference mGamesRef;
    private Query mWaitingGamesQuery;

    private SharedPreferences mPrefs;
    private String mPlayerId;
    private String mPlayerName;

    private TextView mGreetingTextView;
    private TextView mEmptyTextView;

    // Ids y textos de la lista, en el mismo orden
    private final List<String> mGameIds = new ArrayList<>();
    private final List<String> mGameLabels = new ArrayList<>();
    private ArrayAdapter<String> mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        mGamesRef = FirebaseDatabase.getInstance().getReference("games");
        mWaitingGamesQuery = mGamesRef.orderByChild("status").equalTo(OnlineGame.WAITING);

        // Sin login: cada instalación se identifica con un id aleatorio que se guarda.
        mPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        mPlayerId = mPrefs.getString(PREF_PLAYER_ID, null);
        if (mPlayerId == null) {
            mPlayerId = UUID.randomUUID().toString();
            mPrefs.edit().putString(PREF_PLAYER_ID, mPlayerId).apply();
        }
        mPlayerName = mPrefs.getString(PREF_PLAYER_NAME, null);

        mGreetingTextView = findViewById(R.id.text_greeting);
        mGreetingTextView.setOnClickListener(v -> showNameDialog());
        mEmptyTextView = findViewById(R.id.text_empty);

        ListView list = findViewById(R.id.list_games);
        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mGameLabels);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener((parent, view, position, id) ->
                joinGame(mGameIds.get(position)));

        findViewById(R.id.button_create_game).setOnClickListener(v -> createGame());
        findViewById(R.id.button_offline).setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));
        findViewById(R.id.button_about).setOnClickListener(v -> showAboutDialog());

        if (mPlayerName == null) {
            showNameDialog();
        }
        updateGreeting();
        updateEmptyView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // La lista se actualiza sola mientras la pantalla está visible.
        mWaitingGamesQuery.addValueEventListener(mWaitingGamesListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mWaitingGamesQuery.removeEventListener(mWaitingGamesListener);
    }

    private final ValueEventListener mWaitingGamesListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            mGameIds.clear();
            mGameLabels.clear();
            for (DataSnapshot child : snapshot.getChildren()) {
                OnlineGame game = child.getValue(OnlineGame.class);
                // Los juegos que creé yo no aparecen: no puedo jugar contra mí mismo.
                if (game == null || mPlayerId.equals(game.hostId)) {
                    continue;
                }
                mGameIds.add(child.getKey());
                mGameLabels.add(getString(R.string.lobby_game_item, game.hostName));
            }
            mAdapter.notifyDataSetChanged();
            updateEmptyView();
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            showFirebaseError(error);
        }
    };

    // ------------------------------------------------------------ Juegos

    private void createGame() {
        DatabaseReference gameRef = mGamesRef.push();
        gameRef.setValue(OnlineGame.create(mPlayerId, getPlayerName()))
                .addOnSuccessListener(unused -> openGame(gameRef.getKey()))
                .addOnFailureListener(e -> Toast.makeText(this,
                        getString(R.string.lobby_firebase_error, e.getMessage()),
                        Toast.LENGTH_LONG).show());
    }

    /**
     * Unirse va en una transacción: si dos personas tocan el mismo juego a la vez,
     * solo una lo consigue y a la otra se le avisa que ya no está disponible.
     */
    private void joinGame(String gameId) {
        final String playerName = getPlayerName();
        mGamesRef.child(gameId).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                OnlineGame game = currentData.getValue(OnlineGame.class);
                if (game == null) {
                    // Firebase aún no tiene el valor local; reintentará con el del servidor.
                    return Transaction.success(currentData);
                }
                if (!OnlineGame.WAITING.equals(game.status)) {
                    return Transaction.abort();
                }
                game.guestId = mPlayerId;
                game.guestName = playerName;
                game.status = OnlineGame.PLAYING;
                currentData.setValue(game);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed,
                                   @Nullable DataSnapshot snapshot) {
                OnlineGame game = snapshot != null ? snapshot.getValue(OnlineGame.class) : null;
                if (committed && game != null && mPlayerId.equals(game.guestId)) {
                    openGame(gameId);
                } else if (error != null) {
                    showFirebaseError(error);
                } else {
                    Toast.makeText(LobbyActivity.this, R.string.lobby_join_failed,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void openGame(String gameId) {
        Intent intent = new Intent(this, OnlineGameActivity.class);
        intent.putExtra(OnlineGameActivity.EXTRA_GAME_ID, gameId);
        startActivity(intent);
    }

    // ------------------------------------------------------------ Vistas

    private void updateGreeting() {
        mGreetingTextView.setText(getString(R.string.lobby_greeting, getPlayerName()));
    }

    private void updateEmptyView() {
        mEmptyTextView.setVisibility(mGameIds.isEmpty()
                ? View.VISIBLE : View.GONE);
    }

    private String getPlayerName() {
        return mPlayerName != null ? mPlayerName : getString(R.string.default_player_name);
    }

    private void showNameDialog() {
        EditText input = new EditText(this);
        input.setHint(R.string.name_dialog_hint);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setSingleLine();
        if (mPlayerName != null) {
            input.setText(mPlayerName);
            input.setSelection(mPlayerName.length());
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.name_dialog_title)
                .setView(input)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        mPlayerName = name;
                        mPrefs.edit().putString(PREF_PLAYER_NAME, name).apply();
                    }
                    updateGreeting();
                })
                .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setView(LayoutInflater.from(this).inflate(R.layout.about_dialog, null))
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private void showFirebaseError(@NonNull DatabaseError error) {
        Toast.makeText(this, getString(R.string.lobby_firebase_error, error.getMessage()),
                Toast.LENGTH_LONG).show();
    }
}
