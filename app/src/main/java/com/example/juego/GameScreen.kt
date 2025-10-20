package com.example.juego // Reemplaza con tu package name

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.juego.ui.theme.GrisFondo

// Composable principal que organiza la pantalla [cite: 46]
@Composable
fun GameScreen(viewModel: ReflexViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GrisFondo), // [cite: 46]
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Área de Toque Jugador 1 [cite: 46]
        TouchArea(
            player = 1,
            backgroundColor = uiState.roundColor.color,
            onPlayerTap = { viewModel.processTouch(1) },
            modifier = Modifier
                .weight(1f) // [cite: 46]
                .fillMaxWidth()
        )

        // Área Central (Objetivo y Puntuación) [cite: 46]
        TargetDisplay(
            state = uiState
        )

        // Área de Toque Jugador 2 [cite: 46]
        TouchArea(
            player = 2,
            backgroundColor = uiState.roundColor.color,
            onPlayerTap = { viewModel.processTouch(2) },
            modifier = Modifier
                .weight(1f) // [cite: 46]
                .fillMaxWidth()
        )

        // Botón de Reinicio [cite: 46]
        ResetButton(
            isVisible = uiState.gameState == GamePhase.GAME_OVER,
            onReset = { viewModel.resetGame() } // [cite: 46]
        )
    }
}

@Composable
fun TouchArea(
    player: Int,
    backgroundColor: Color,
    onPlayerTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Animación de color para la transición [cite: 46]
    val animatedColor by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = tween(150), // Transición suave
        label = "TouchAreaColor"
    )

    Box(
        modifier = modifier
            .background(animatedColor)
            .clickable { onPlayerTap(player) }, // [cite: 46]
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "JUGADOR $player",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun TargetDisplay(state: GameUiState) {
    // Usar AnimatedContent para transiciones de texto [cite: 46]
    AnimatedContent(
        targetState = state.gameState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
        label = "TargetMessage"
    ) { targetState ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when (targetState) {
                GamePhase.GAME_OVER -> {
                    // Mensaje de Victoria [cite: 18]
                    Text(
                        text = state.winnerMessage,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                else -> {
                    // Anuncio de Color Objetivo [cite: 7]
                    Text(
                        text = "¡Presiona el ${state.targetColor.nombre}!",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = state.targetColor.color
                    )
                }
            }
            // Mostrar puntuación siempre, excepto si es GAME_OVER y ya se muestra el ganador
            if (targetState != GamePhase.GAME_OVER) {
                Text(
                    text = "${state.scoreJ1} - ${state.scoreJ2}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}


@Composable
fun ResetButton(
    isVisible: Boolean,
    onReset: () -> Unit
) {
    if (isVisible) { // [cite: 46]
        Button(
            onClick = onReset,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = "Nueva Partida", fontSize = 18.sp) // [cite: 46]
        }
    }
}