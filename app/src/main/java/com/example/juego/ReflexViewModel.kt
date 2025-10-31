package com.example.juego

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.juego.data.GameStats // ¡NUEVO!
import com.example.juego.data.StatsRepository // ¡NUEVO!
import com.example.juego.ui.theme.GrisEspera
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted // ¡NUEVO!
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn // ¡NUEVO!
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

// ¡MODIFICADO! Añadir repositorio al constructor
class ReflexViewModel(
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // ¡NUEVO! Exponer las estadísticas a la UI
    val stats: StateFlow<GameStats> = statsRepository.stats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GameStats.default
    )

    // ... (el resto de tus variables: gameJob, metaPuntuacion, etc.)
    private var gameJob: Job? = null
    private val metaPuntuacion = 5
    private val roundTimeoutMs = 2500L
    private val processingDelayMs = 100L

    init {
        resetGame()
    }

    fun resetGame() {
        gameJob?.cancel()
        _uiState.value = GameUiState()
        startRound()
    }

    // ... (startRound y generateRoundColor se quedan igual)
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
                if (it.gameState == GamePhase.ESPERA) {
                    it.copy(gameState = GamePhase.GO, roundColor = generateRoundColor())
                } else {
                    it
                }
            }
            // 4. Iniciar temporizador de "timeout"
            delay(roundTimeoutMs)
            // 5. Lógica de timeout
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
                        gameState = GamePhase.PROCESANDO
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

                        // ¡NUEVO! Registrar la victoria
                        val winner = if (newScoreJ1 > newScoreJ2) 1 else 2
                        viewModelScope.launch {
                            statsRepository.recordGameWin(winner)
                        }

                        currentState.copy(
                            scoreJ1 = newScoreJ1,
                            scoreJ2 = newScoreJ2,
                            gameState = GamePhase.GAME_OVER,
                            winnerMessage = "¡GANA ${if (winner == 1) "Jugador 1" else "Jugador 2"}!"
                        )
                    } else {
                        shouldStartNextRound = true
                        currentState.copy(
                            scoreJ1 = newScoreJ1,
                            scoreJ2 = newScoreJ2,
                            gameState = GamePhase.PROCESANDO
                        )
                    }
                }

                GamePhase.PROCESANDO, GamePhase.GAME_OVER -> {
                    currentState // No hacer nada
                }
            }
        }

        if (shouldStartNextRound) {
            viewModelScope.launch {
                delay(processingDelayMs)
                startRound()
            }
        }
    }
}