package com.example.juego

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.juego.data.GameSaveRepository
import com.example.juego.data.GameStats
import com.example.juego.data.SaveFormat
import com.example.juego.data.SoundManager
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
import com.example.juego.data.SavedGameMetadata // ¡NUEVO!
import com.example.juego.data.SavedGameMetadataDao // ¡NUEVO!

// ¡MODIFICADO! Añadido soundManager al constructor
class ReflexViewModel(
    private val statsRepository: StatsRepository,
    private val gameSaveRepository: GameSaveRepository,
    private val soundManager: SoundManager,
    private val metadataDao: SavedGameMetadataDao // ¡NUEVO!
) : ViewModel() {

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
    private val timeAttackDuration = 60

    init {
        startGame(GameMode.CLASSIC)
    }

    // --- ¡NUEVO! Limpiar el SoundPool cuando el ViewModel se destruya ---
    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }
    // ------------------------------------------------------------------

    fun startGame(mode: GameMode) {
        gameJob?.cancel()
        timerJob?.cancel()

        _uiState.value = GameUiState(
            gameMode = mode,
            remainingTime = timeAttackDuration
        )

        startRound()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L) // Cada segundo

                val currentMode = _uiState.value.gameMode
                if (currentMode == GameMode.TIME_ATTACK) {
                    var newTime = 0
                    _uiState.update {
                        newTime = it.remainingTime - 1
                        it.copy(remainingTime = newTime)
                    }

                    if (newTime <= 0) {
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

                if (currentState.gameMode == GameMode.CONFUSION) {
                    do {
                        newTargetTextColor = GameColor.getRandom()
                    } while (newTargetTextColor == newTargetColor)
                }

                currentState.copy(
                    gameState = GamePhase.ESPERA,
                    targetColorName = newTargetColor.nombre,
                    roundColorName = GameColor.fromName("GRIS").nombre,
                    targetTextColorName = newTargetTextColor?.nombre
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
        if (_uiState.value.gameState == GamePhase.PROCESANDO || _uiState.value.gameState == GamePhase.GAME_OVER) {
            return
        }

        gameJob?.cancel()
        timerJob?.cancel()

        val moveLog = "P$player tocó en ${System.currentTimeMillis()}"
        var shouldStartNextRound = false
        var nextState: GameUiState? = null

        _uiState.update { currentState ->
            val newMoveHistory = currentState.moveHistory + moveLog

            when (currentState.gameState) {
                GamePhase.ESPERA -> {
                    // ¡SONIDO! Tocar antes de tiempo es un error
                    soundManager.play(SoundManager.SoundType.ERROR)
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

                    if (player == 0) { // Timeout
                        if (isCorrect) {
                            // ¡SONIDO! No presionar en el color correcto es un error
                            soundManager.play(SoundManager.SoundType.ERROR)
                            newScoreJ1 = (newScoreJ1 - 1).coerceAtLeast(0)
                            newScoreJ2 = (newScoreJ2 - 1).coerceAtLeast(0)
                        } else {
                            // No presionar en el color incorrecto está bien. Sin sonido.
                        }
                    } else if (isCorrect) { // Acierto
                        // ¡SONIDO! Acierto
                        soundManager.play(SoundManager.SoundType.ACIERTO)
                        if (player == 1) newScoreJ1++ else newScoreJ2++
                    } else { // Error
                        // ¡SONIDO! Error
                        soundManager.play(SoundManager.SoundType.ERROR)
                        if (player == 1) newScoreJ2++ else newScoreJ1++
                    }

                    if (currentState.gameMode != GameMode.TIME_ATTACK) {
                        if (newScoreJ1 >= metaPuntuacion || newScoreJ2 >= metaPuntuacion) {
                            val winner = if (newScoreJ1 > newScoreJ2) 1 else 2
                            nextState = endGameClassic(newScoreJ1, newScoreJ2, winner, newMoveHistory)
                            return@update nextState!!
                        }
                    }

                    shouldStartNextRound = true
                    currentState.copy(
                        scoreJ1 = newScoreJ1,
                        scoreJ2 = newScoreJ2,
                        gameState = GamePhase.PROCESANDO,
                        moveHistory = newMoveHistory
                    )
                }

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

    private fun endGameClassic(scoreJ1: Int, scoreJ2: Int, winner: Int, history: List<String>): GameUiState {
        timerJob?.cancel()
        viewModelScope.launch {
            statsRepository.recordGameWin(winner)
        }
        // ¡SONIDO! Fin de la partida
        soundManager.play(SoundManager.SoundType.GANAR)

        return _uiState.value.copy(
            scoreJ1 = scoreJ1,
            scoreJ2 = scoreJ2,
            gameState = GamePhase.GAME_OVER,
            winnerMessage = "¡GANA ${if (winner == 1) "Jugador 1" else "Jugador 2"}!",
            moveHistory = history
        )
    }

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
                // ¡SONIDO! Solo si hay ganador
                soundManager.play(SoundManager.SoundType.GANAR)
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
    // Esta función la llamaremos desde la UI cuando el usuario salga de la pantalla.
    fun pauseGame() {
        // Solo pausar si el juego está corriendo
        if (uiState.value.gameState != GamePhase.PAUSED && uiState.value.gameState != GamePhase.GAME_OVER) {
            gameJob?.cancel()
            timerJob?.cancel()
            _uiState.update { it.copy(gameState = GamePhase.PAUSED) }
            Log.d("ReflexViewModel", "Juego pausado automáticamente por salir de la pantalla")
        }
    }

    fun resetCurrentGame() {
        startGame(_uiState.value.gameMode)
    }

    fun saveCurrentGame(fileName: String, format: SaveFormat) {
        // Pausamos el juego antes de guardar
        gameJob?.cancel()
        timerJob?.cancel()
        _uiState.update { it.copy(gameState = GamePhase.PAUSED) }

        // Creamos una variable local para el estado actual
        val currentState = _uiState.value
        val fullFileName = "$fileName${format.extension}"

        viewModelScope.launch {
            // 1. Guardar el archivo (como antes)
            gameSaveRepository.saveGame(currentState, fileName, format)

            // 2. ¡NUEVO! Crear y guardar los metadatos en Room
            val metadata = SavedGameMetadata(
                fileName = fullFileName,
                scoreJ1 = currentState.scoreJ1,
                scoreJ2 = currentState.scoreJ2,
                gameMode = currentState.gameMode,
                timestamp = System.currentTimeMillis() // Fecha y hora actual
            )
            metadataDao.insert(metadata)

            Log.i("ReflexViewModel", "Partida guardada: $fullFileName y metadatos guardados.")
        }
    }

    fun loadGame(fileName: String) {
        viewModelScope.launch {
            val loadedState = gameSaveRepository.loadGame(fileName)
            if (loadedState != null) {
                gameJob?.cancel()
                timerJob?.cancel()
                _uiState.value = loadedState.copy(gameState = GamePhase.PAUSED)
            } else {
                Log.w("ReflexViewModel", "No se pudo cargar la partida: $fileName")
            }
        }
    }

    fun resumeGame() {
        if (_uiState.value.gameState == GamePhase.PAUSED) {
            startTimer()
            startRound()
        }
    }
}