package com.example.juego.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class StatsRepository(private val statsDao: StatsDao) {

    // Exponemos un Flow que nunca es nulo
    val stats: Flow<GameStats> = statsDao.getStats().map { it ?: GameStats.default }

    // Función para actualizar las estadísticas
    suspend fun recordGameWin(winnerPlayer: Int) {
        withContext(Dispatchers.IO) {
            val currentStats = stats.first() // Toma el valor actual del Flow

            val newStats = currentStats.copy(
                gamesPlayed = currentStats.gamesPlayed + 1,
                player1Wins = if (winnerPlayer == 1) currentStats.player1Wins + 1 else currentStats.player1Wins,
                player2Wins = if (winnerPlayer == 2) currentStats.player2Wins + 1 else currentStats.player2Wins
            )

            statsDao.updateStats(newStats)
        }
    }
}