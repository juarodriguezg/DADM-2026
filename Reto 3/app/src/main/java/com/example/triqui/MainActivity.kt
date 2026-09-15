package com.example.triqui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.triqui.ui.theme.TriquiTheme

enum class GameStatus { PLAYER1_TURN, PLAYER2_TURN, TIE, PLAYER1_WINS, PLAYER2_WINS }
enum class GameMode { VS_ANDROID, VS_PLAYER }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TriquiTheme {
                TicTacToeScreen()
            }
        }
    }
}

@Composable
fun TicTacToeScreen(vm: TicTacToeViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    val statusMessage = when (state.status) {
        GameStatus.PLAYER1_TURN -> if (state.mode == GameMode.VS_ANDROID) stringResource(R.string.turn_human)
                                   else stringResource(R.string.turn_player1)
        GameStatus.PLAYER2_TURN -> if (state.mode == GameMode.VS_ANDROID) stringResource(R.string.turn_android)
                                   else stringResource(R.string.turn_player2)
        GameStatus.TIE -> stringResource(R.string.result_tie)
        GameStatus.PLAYER1_WINS -> if (state.mode == GameMode.VS_ANDROID) stringResource(R.string.result_human_wins)
                                   else stringResource(R.string.result_player1_wins)
        GameStatus.PLAYER2_WINS -> if (state.mode == GameMode.VS_ANDROID) stringResource(R.string.result_android_wins)
                                   else stringResource(R.string.result_player2_wins)
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val screenHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .heightIn(min = screenHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Triqui",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Selector de modo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeButton(
                        label = stringResource(R.string.mode_vs_android),
                        selected = state.mode == GameMode.VS_ANDROID,
                        modifier = Modifier.weight(1f),
                        onClick = { if (state.mode != GameMode.VS_ANDROID) vm.changeMode(GameMode.VS_ANDROID) }
                    )
                    ModeButton(
                        label = stringResource(R.string.mode_vs_player),
                        selected = state.mode == GameMode.VS_PLAYER,
                        modifier = Modifier.weight(1f),
                        onClick = { if (state.mode != GameMode.VS_PLAYER) vm.changeMode(GameMode.VS_PLAYER) }
                    )
                }

                // Marcador
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val p1Label = if (state.mode == GameMode.VS_ANDROID) stringResource(R.string.score_human)
                                  else stringResource(R.string.score_player1)
                    val p2Label = if (state.mode == GameMode.VS_ANDROID) stringResource(R.string.score_android)
                                  else stringResource(R.string.score_player2)
                    Text("$p1Label: ${state.player1Wins}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("${stringResource(R.string.score_ties)}: ${state.ties}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("$p2Label: ${state.player2Wins}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                // Cuadrícula 3×3
                val androidThinking = state.mode == GameMode.VS_ANDROID && state.status == GameStatus.PLAYER2_TURN
                for (row in 0..2) {
                    Row {
                        for (col in 0..2) {
                            val index = row * 3 + col
                            val cell = state.board[index]
                            val isOpen = cell == TicTacToeGame.OPEN_SPOT
                            Button(
                                onClick = { vm.handleClick(index) },
                                enabled = !state.gameOver && isOpen && !androidThinking,
                                modifier = Modifier
                                    .size(100.dp)
                                    .padding(4.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF757575),
                                    disabledContainerColor = if (isOpen) Color(0xFF424242) else Color(0xFF616161)
                                )
                            ) {
                                Text(
                                    text = if (isOpen) "" else cell.toString(),
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (cell) {
                                        TicTacToeGame.HUMAN_PLAYER -> Color(0xFF00C800)
                                        TicTacToeGame.COMPUTER_PLAYER -> Color(0xFFC80000)
                                        else -> Color.Transparent
                                    }
                                )
                            }
                        }
                    }
                }

                Text(
                    text = statusMessage,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
                )

                Button(
                    onClick = { vm.startNewGame() },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(stringResource(R.string.new_game), fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                          else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}
