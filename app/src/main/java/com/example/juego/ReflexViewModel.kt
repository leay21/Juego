package com.example.juego

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.juego.data.GameSaveRepository
import com.example.juego.data.GameStats
import com.example.juego.data.SaveFormat
import com.example.juego.data.StatsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class ReflexViewModel(
    private val statsRepository: StatsRepository,
    private val gameSaveRepository: GameSaveRepository
) : ViewModel() {

    // ¡CORREGIDO! Usamos el constructor por defecto
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    val stats: StateFlow<GameStats> = statsRepository.stats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GameStats.default
    )

    private var gameJob: Job? = null
    private var timerJob: Job? = null
    private val metaPuntuacion = 5
    private val roundTimeoutMs = 2500L
    private val processingDelayMs = 100L
    private val timeAttackDuration = 60 // ¡CORREGIDO! Valor añadido

    // ¡CORREGIDO! El init ahora inicia el modo clásico por defecto
    init {
        startGame(GameMode.CLASSIC)
    }

    // ¡NUEVO! Función pública para iniciar el juego desde la UI
    fun startGame(mode: GameMode) {
        gameJob?.cancel()
        timerJob?.cancel()

        // Reseteamos el estado con el modo de juego seleccionado
        _uiState.value = GameUiState(
            gameMode = mode,
            remainingTime = timeAttackDuration
        )

        startRound()
    }

    // ¡CORREGIDO! Eliminada la función 'startGame()' duplicada y sin parámetros

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L) // Cada segundo

                // Lógica del timer depende del modo de juego
                val currentMode = _uiState.value.gameMode
                if (currentMode == GameMode.TIME_ATTACK) {
                    var newTime = 0
                    _uiState.update {
                        newTime = it.remainingTime - 1
                        it.copy(remainingTime = newTime)
                    }

                    if (newTime <= 0) {
                        // Si el tiempo se acaba, finaliza el juego
                        // (Se llama fuera del update)
                        endGameTimeAttack()
                    }
                } else { // CLASSIC y CONFUSION
                    _uiState.update {
                        it.copy(timeElapsed = it.timeElapsed + 1)
                    }
                }
            }
        }
    }

    private fun startRound() {
        gameJob?.cancel()
        startTimer()

        gameJob = viewModelScope.launch {
            // 1. Estado ESPERA
            _uiState.update { currentState ->

                val newTargetColor = GameColor.getRandom()
                var newTargetTextColor: GameColor? = null

                // Lógica para modo Confusión
                if (currentState.gameMode == GameMode.CONFUSION) {
                    do {
                        newTargetTextColor = GameColor.getRandom()
                    } while (newTargetTextColor == newTargetColor) // Asegura que el color del texto sea diferente
                }

                currentState.copy(
                    gameState = GamePhase.ESPERA,
                    targetColorName = newTargetColor.nombre,
                    roundColorName = GameColor.fromName("GRIS").nombre,
                    targetTextColorName = newTargetTextColor?.nombre // Será null si no es modo Confusión
                )
            }
            // 2. Delay aleatorio
            delay(Random.nextLong(2000, 5001))
            // 3. Estado GO
            _uiState.update {
                if (it.gameState == GamePhase.ESPERA) {
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
        // Evitar dobles toques
        if (_uiState.value.gameState == GamePhase.PROCESANDO || _uiState.value.gameState == GamePhase.GAME_OVER) {
            return
        }

        gameJob?.cancel()
        timerJob?.cancel()

        val moveLog = "P$player tocó en ${System.currentTimeMillis()}"
        var shouldStartNextRound = false

        // ¡CORREGIDO! La lógica de fin de juego se maneja fuera del update
        var nextState: GameUiState? = null

        _uiState.update { currentState ->
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
                        moveHistory = newMoveHistory
                    )
                }

                GamePhase.GO -> {
                    val isCorrect = currentState.roundColor == currentState.targetColor
                    var newScoreJ1 = currentState.scoreJ1
                    var newScoreJ2 = currentState.scoreJ2

                    // Timeout (Jugador 0)
                    if (player == 0) {
                        if (isCorrect) {
                            // SÍ era el color correcto, pero nadie presionó. ¡Penalización!
                            newScoreJ1 = (newScoreJ1 - 1).coerceAtLeast(0)
                            newScoreJ2 = (newScoreJ2 - 1).coerceAtLeast(0)
                        }
                    } else if (isCorrect) { // Un jugador (1 o 2) presionó y era el color CORRECTO
                        if (player == 1) newScoreJ1++ else newScoreJ2++
                    } else { // Un jugador (1 o 2) presionó y era el color INCORRECTO
                        if (player == 1) newScoreJ2++ else newScoreJ1++
                    }

                    // Comprobar si el juego termina (para CLASSIC/CONFUSION)
                    if (currentState.gameMode != GameMode.TIME_ATTACK) {
                        if (newScoreJ1 >= metaPuntuacion || newScoreJ2 >= metaPuntuacion) {
                            val winner = if (newScoreJ1 > newScoreJ2) 1 else 2
                            // Preparamos el estado de fin de juego
                            nextState = endGameClassic(newScoreJ1, newScoreJ2, winner, newMoveHistory)
                            return@update nextState!! // Salimos del update con el estado final
                        }
                    }

                    // Si el juego no termina, pasa a PROCESANDO
                    shouldStartNextRound = true
                    currentState.copy(
                        scoreJ1 = newScoreJ1,
                        scoreJ2 = newScoreJ2,
                        gameState = GamePhase.PROCESANDO,
                        moveHistory = newMoveHistory
                    )
                }

                // Estados ya cubiertos
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

    // ¡CORREGIDO! Esta función ahora DEVUELVE el estado de GAME_OVER, no actualiza la UI
    private fun endGameClassic(scoreJ1: Int, scoreJ2: Int, winner: Int, history: List<String>): GameUiState {
        timerJob?.cancel()
        viewModelScope.launch {
            statsRepository.recordGameWin(winner)
        }

        return _uiState.value.copy(
            scoreJ1 = scoreJ1,
            scoreJ2 = scoreJ2,
            gameState = GamePhase.GAME_OVER,
            winnerMessage = "¡GANA ${if (winner == 1) "Jugador 1" else "Jugador 2"}!",
            moveHistory = history
        )
    }

    // ¡CORREGIDO! Esta función SÍ actualiza la UI porque se llama desde un Job
    private fun endGameTimeAttack() {
        gameJob?.cancel()
        timerJob?.cancel()

        _uiState.update { currentState ->
            val winner = when {
                currentState.scoreJ1 > currentState.scoreJ2 -> 1
                currentState.scoreJ2 > currentState.scoreJ1 -> 2
                else -> 0 // Empate
            }
            if(winner != 0) {
                viewModelScope.launch { statsRepository.recordGameWin(winner) }
            }

            val winnerMsg = when (winner) {
                1 -> "¡Gana Jugador 1 por puntos!"
                2 -> "¡Gana Jugador 2 por puntos!"
                else -> "¡EMPATE!"
            }

            currentState.copy(
                gameState = GamePhase.GAME_OVER,
                winnerMessage = winnerMsg,
                remainingTime = 0
            )
        }
    }

    // Función para el botón "Reiniciar"
    fun resetCurrentGame() {
        // Reinicia el juego con el modo actual
        startGame(_uiState.value.gameMode)
    }

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
                // Nos aseguramos de que el estado esté en PAUSA
                _uiState.value = loadedState.copy(gameState = GamePhase.PAUSED)
            } else {
                Log.w("ReflexViewModel", "No se pudo cargar la partida: $fileName")
            }
        }
    }

    // Función para reanudar el juego si está en pausa
    fun resumeGame() {
        if (_uiState.value.gameState == GamePhase.PAUSED) {
            // No reinicia la ronda, solo los timers
            startTimer()
            startRound() // Re-lanza la ronda (podríamos guardar más estado para evitar esto, pero así es más simple)
        }
    }
}