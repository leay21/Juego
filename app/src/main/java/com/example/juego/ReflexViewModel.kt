package com.example.juego

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.juego.data.GameSaveRepository // ¡NUEVO!
import com.example.juego.data.GameStats // ¡NUEVO!
import com.example.juego.data.SaveFormat // ¡NUEVO!
import com.example.juego.data.StatsRepository // ¡NUEVO!
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

// ¡MODIFICADO! Añadir repositorios al constructor
class ReflexViewModel(
    private val statsRepository: StatsRepository,
    private val gameSaveRepository: GameSaveRepository // ¡NUEVO!
) : ViewModel() {

    // ¡MODIFICADO! Usamos el constructor secundario
    private val _uiState = MutableStateFlow(
        GameUiState(
            targetColor = GameColor.ROJO,
            roundColor = GameColor("GRIS", GameColor.fromName("GRIS").color)
        )
    )
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // ¡NUEVO! Exponer las estadísticas a la UI
    val stats: StateFlow<GameStats> = statsRepository.stats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GameStats.default
    )

    // ... (el resto de tus variables: gameJob, metaPuntuacion, etc.)
    private var gameJob: Job? = null
    private var timerJob: Job? = null // ¡NUEVO! Job para el cronómetro
    private val metaPuntuacion = 5
    private val roundTimeoutMs = 2500L
    private val processingDelayMs = 100L

    init {
        resetGame()
    }

    fun resetGame() {
        gameJob?.cancel()
        timerJob?.cancel() // ¡NUEVO!
        // ¡MODIFICADO! Usamos el constructor secundario
        _uiState.value = GameUiState(
            targetColor = GameColor.ROJO,
            roundColor = GameColor("GRIS", GameColor.fromName("GRIS").color)
        )
        startRound()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L) // Cada segundo
                _uiState.update {
                    it.copy(timeElapsed = it.timeElapsed + 1)
                }
            }
        }
    }

    private fun startRound() {
        gameJob?.cancel()
        // ¡NUEVO! El timer corre solo durante la ronda
        startTimer()

        gameJob = viewModelScope.launch {
            // 1. Estado ESPERA
            _uiState.update {
                // Usamos "targetColorName" y "roundColorName"
                it.copy(
                    gameState = GamePhase.ESPERA,
                    targetColorName = GameColor.getRandom().nombre,
                    roundColorName = GameColor.fromName("GRIS").nombre
                )
            }
            // 2. Delay aleatorio
            delay(Random.nextLong(2000, 5001))
            // 3. Estado GO
            _uiState.update {
                if (it.gameState == GamePhase.ESPERA) {
                    // Usamos "roundColorName"
                    it.copy(
                        gameState = GamePhase.GO,
                        roundColorName = generateRoundColor().nombre
                    )
                } else {
                    it
                }
            }
            // 4. Iniciar temporizador de "timeout"
            delay(roundTimeoutMs)
            // 5. Lógica de timeout
            if (_uiState.value.gameState == GamePhase.GO) {
                // Usamos 0 para el timeout
                processTouch(0)
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
        if (_uiState.value.gameState == GamePhase.PROCESANDO || _uiState.value.gameState == GamePhase.GAME_OVER) {
            return // Evitar dobles toques
        }

        // ¡NUEVO! Pausar timers
        gameJob?.cancel()
        timerJob?.cancel()

        var shouldStartNextRound = false

        // ¡NUEVO! Registrar movimiento
        val moveLog = "P$player tocó en ${System.currentTimeMillis()}"

        _uiState.update { currentState ->
            // ¡NUEVO! Añadir movimiento al historial
            val newMoveHistory = currentState.moveHistory + moveLog

            when (currentState.gameState) {
                GamePhase.ESPERA -> {
                    shouldStartNextRound = true
                    val newScoreJ1 = if (player == 1) (currentState.scoreJ1 - 1).coerceAtLeast(0) else currentState.scoreJ1
                    val newScoreJ2 = if (player == 2) (currentState.scoreJ2 - 1).coerceAtLeast(0) else currentState.scoreJ2
                    currentState.copy(
                        scoreJ1 = newScoreJ1,
                        scoreJ2 = newScoreJ2,
                        gameState = GamePhase.PROCESANDO,
                        moveHistory = newMoveHistory // Actualizar historial
                    )
                }

                GamePhase.GO -> {
                    val isCorrect = currentState.roundColor == currentState.targetColor
                    var newScoreJ1 = currentState.scoreJ1
                    var newScoreJ2 = currentState.scoreJ2

                    // Timeout (Jugador 0)
                    if (player == 0) {
                        newScoreJ1 = (newScoreJ1 - 1).coerceAtLeast(0)
                        newScoreJ2 = (newScoreJ2 - 1).coerceAtLeast(0)
                    } else if (isCorrect) {
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
                            winnerMessage = "¡GANA ${if (winner == 1) "Jugador 1" else "Jugador 2"}!",
                            moveHistory = newMoveHistory // Actualizar historial
                        )
                    } else {
                        shouldStartNextRound = true
                        currentState.copy(
                            scoreJ1 = newScoreJ1,
                            scoreJ2 = newScoreJ2,
                            gameState = GamePhase.PROCESANDO,
                            moveHistory = newMoveHistory // Actualizar historial
                        )
                    }
                }

                // Estados ya cubiertos al inicio
                GamePhase.PROCESANDO, GamePhase.GAME_OVER, GamePhase.PAUSED -> currentState
            }
        }

        if (shouldStartNextRound) {
            viewModelScope.launch {
                delay(processingDelayMs)
                startRound()
            }
        }
    }

    // --- ¡NUEVAS FUNCIONES DE GUARDADO/CARGA! ---

    fun saveCurrentGame(fileName: String, format: SaveFormat) {
        // Pausamos el juego antes de guardar
        gameJob?.cancel()
        timerJob?.cancel()
        _uiState.update { it.copy(gameState = GamePhase.PAUSED) }

        viewModelScope.launch {
            gameSaveRepository.saveGame(_uiState.value, fileName, format)
            Log.i("ReflexViewModel", "Partida guardada: $fileName")
        }
    }

    fun loadGame(fileName: String) {
        viewModelScope.launch {
            val loadedState = gameSaveRepository.loadGame(fileName)
            if (loadedState != null) {
                // Estado cargado, pausamos los timers
                gameJob?.cancel()
                timerJob?.cancel()
                _uiState.value = loadedState.copy(gameState = GamePhase.PAUSED)
            } else {
                Log.w("ReflexViewModel", "No se pudo cargar la partida: $fileName")
            }
        }
    }

    // Función para reanudar el juego si está en pausa
    fun resumeGame() {
        if (_uiState.value.gameState == GamePhase.PAUSED) {
            startRound()
        }
    }
}