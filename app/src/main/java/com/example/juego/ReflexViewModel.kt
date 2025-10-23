package com.example.juego // Reemplaza con tu package name

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.juego.ui.theme.GrisEspera
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class ReflexViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var gameJob: Job? = null
    private val metaPuntuacion = 5
    private val roundTimeoutMs = 2500L

    // ¡NUEVO! Pequeño delay para asegurar que el estado PROCESANDO
    // sea visto por ambos toques simultáneos.
    private val processingDelayMs = 100L

    init {
        resetGame()
    }

    fun resetGame() {
        gameJob?.cancel()
        _uiState.value = GameUiState()
        startRound()
    }

    private fun startRound() {
        gameJob?.cancel()
        gameJob = viewModelScope.launch {
            // 1. Estado ESPERA
            _uiState.update {
                it.copy(
                    gameState = GamePhase.ESPERA,
                    targetColor = GameColor.getRandom(),
                    roundColor = GameColor("GRIS", GrisEspera)
                )
            }

            // 2. Delay aleatorio
            delay(Random.nextLong(2000, 5001))

            // 3. Estado GO
            _uiState.update {
                // Solo cambiar a GO si todavía estamos en ESPERA
                // (una Falsa Alarma podría haber cancelado esta ronda)
                if (it.gameState == GamePhase.ESPERA) {
                    it.copy(gameState = GamePhase.GO, roundColor = generateRoundColor())
                } else {
                    it
                }
            }

            // 4. Iniciar temporizador de "timeout"
            delay(roundTimeoutMs)

            // 5. ¡MODIFICADO! Lógica de timeout simplificada.
            // Si llegamos aquí, el job no fue cancelado por processTouch.
            // Verificamos si el estado sigue siendo GO y reiniciamos.
            if (_uiState.value.gameState == GamePhase.GO) {
                startRound()
            }
        }
    }

    private fun generateRoundColor(): GameColor {
        val currentTarget = _uiState.value.targetColor
        if (Random.nextBoolean()) {
            return currentTarget
        } else {
            var newColor: GameColor
            do {
                newColor = GameColor.getRandom()
            } while (newColor == currentTarget)
            return newColor
        }
    }

    fun processTouch(player: Int) {

        var shouldStartNextRound = false

        _uiState.update { currentState ->

            when (currentState.gameState) {

                GamePhase.ESPERA -> {
                    gameJob?.cancel()
                    shouldStartNextRound = true

                    val newScoreJ1 = if (player == 1) (currentState.scoreJ1 - 1).coerceAtLeast(0) else currentState.scoreJ1
                    val newScoreJ2 = if (player == 2) (currentState.scoreJ2 - 1).coerceAtLeast(0) else currentState.scoreJ2

                    currentState.copy(
                        scoreJ1 = newScoreJ1,
                        scoreJ2 = newScoreJ2,
                        gameState = GamePhase.PROCESANDO // ¡Bloquear!
                    )
                }

                GamePhase.GO -> {
                    gameJob?.cancel()

                    val isCorrect = currentState.roundColor == currentState.targetColor
                    var newScoreJ1 = currentState.scoreJ1
                    var newScoreJ2 = currentState.scoreJ2

                    if (isCorrect) {
                        if (player == 1) newScoreJ1++ else newScoreJ2++
                    } else {
                        if (player == 1) newScoreJ2++ else newScoreJ1++
                    }

                    if (newScoreJ1 >= metaPuntuacion || newScoreJ2 >= metaPuntuacion) {
                        val winner = if (newScoreJ1 > newScoreJ2) "Jugador 1" else "Jugador 2"
                        currentState.copy(
                            scoreJ1 = newScoreJ1,
                            scoreJ2 = newScoreJ2,
                            gameState = GamePhase.GAME_OVER,
                            winnerMessage = "¡GANA $winner!"
                        )
                    } else {
                        shouldStartNextRound = true
                        currentState.copy(
                            scoreJ1 = newScoreJ1,
                            scoreJ2 = newScoreJ2,
                            gameState = GamePhase.PROCESANDO // ¡Bloquear!
                        )
                    }
                }

                // Si el estado es PROCESANDO o GAME_OVER, ignorar este toque
                GamePhase.PROCESANDO, GamePhase.GAME_OVER -> {
                    currentState // No hacer nada
                }
            }
        }

        // ¡MODIFICADO!
        if (shouldStartNextRound) {
            // Iniciar la siguiente ronda después de una breve pausa
            // para permitir que los toques simultáneos (T2)
            // sean ignorados por el estado PROCESANDO.
            viewModelScope.launch {
                delay(processingDelayMs) // Espera 100ms
                startRound()
            }
        }
    }
}