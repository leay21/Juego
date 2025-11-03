package com.example.juego.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {
    // Upsert = Inserta o Actualiza si ya existe
    @Upsert
    suspend fun updateStats(stats: GameStats)

    // Flow para que la UI se actualice sola
    @Query("SELECT * FROM game_stats WHERE id = 1")
    fun getStats(): Flow<GameStats?> // Nulo si la tabla está vacía
}