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
    private val metaPuntuacion = 5 // Objetivo para ganar
    private val roundTimeoutMs = 2500L // 2.5 segundos

    init {
        resetGame()
    }

    // Función para reiniciar el juego
    fun resetGame() {
        gameJob?.cancel()
        _uiState.value = GameUiState()
        startRound()
    }

    // Inicia una nueva ronda
    private fun startRound() {
        gameJob?.cancel() // Cancela cualquier ronda anterior
        gameJob = viewModelScope.launch {
            // 1. Estado ESPERA
            _uiState.update {
                it.copy(
                    gameState = GamePhase.ESPERA,
                    targetColor = GameColor.getRandom(),
                    roundColor = GameColor("GRIS", GrisEspera)
                )
            }

            // 2. Delay aleatorio (2-5 segundos)
            delay(Random.nextLong(2000, 5001))

            // 3. Estado GO
            _uiState.update {
                it.copy(
                    gameState = GamePhase.GO,
                    roundColor = generateRoundColor()
                )
            }

            // 4. Iniciar temporizador de "timeout" para la ronda
            delay(roundTimeoutMs)

            // 5. Verificar si nadie ha tocado
            if (_uiState.value.gameState == GamePhase.GO) {
                startRound()
            }
        }
    }

    // Genera el color de la ronda
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

    // Núcleo de validación, llamado desde la UI
    fun processTouch(player: Int) {
        if (_uiState.value.gameState != GamePhase.GO && _uiState.value.gameState != GamePhase.ESPERA) {
            return
        }

        when (_uiState.value.gameState) {
            // Falsa Alarma
            GamePhase.ESPERA -> {
                // ¡MODIFICADO! Llama a la función con el nombre nuevo
                applyFalsaAlarmaPenalty(player)
                startRound()
            }
            // Toque durante GO
            GamePhase.GO -> {
                val state = _uiState.value
                val isCorrect = state.roundColor == state.targetColor

                if (isCorrect) {
                    // Toque Válido
                    applyScore(player, 1) //
                } else {
                    // ¡MODIFICADO! Fallo por Color
                    // Se da +1 punto al oponente
                    val opponent = if (player == 1) 2 else 1
                    applyScore(opponent, 1)
                }

                if (!checkWinCondition()) {
                    startRound()
                }
            }
            GamePhase.GAME_OVER -> { /* No hacer nada */ }
        }
    }

    private fun applyScore(player: Int, points: Int) {
        _uiState.update {
            if (player == 1) {
                it.copy(scoreJ1 = it.scoreJ1 + points)
            } else {
                it.copy(scoreJ2 = it.scoreJ2 + points)
            }
        }
    }

    // ¡MODIFICADO! Renombrada de applyPenalty a applyFalsaAlarmaPenalty
    private fun applyFalsaAlarmaPenalty(player: Int) {
        // Aplicar penalización de -1 punto
        _uiState.update {
            if (player == 1) {
                it.copy(scoreJ1 = (it.scoreJ1 - 1).coerceAtLeast(0))
            } else {
                it.copy(scoreJ2 = (it.scoreJ2 - 1).coerceAtLeast(0))
            }
        }
    }

    // Detección de Victoria
    private fun checkWinCondition(): Boolean {
        val score1 = _uiState.value.scoreJ1
        val score2 = _uiState.value.scoreJ2

        if (score1 >= metaPuntuacion || score2 >= metaPuntuacion) { //
            val winner = if (score1 > score2) "Jugador 1" else "Jugador 2"
            _uiState.update {
                it.copy(
                    gameState = GamePhase.GAME_OVER, //
                    winnerMessage = "¡GANA $winner!" //
                )
            }
            gameJob?.cancel()
            return true
        }
        return false
    }
}