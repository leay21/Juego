package com.example.juego.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedGameMetadataDao {

    // Obtiene todas las partidas, ordenadas por la más reciente primero
    @Query("SELECT * FROM saved_game_metadata ORDER BY timestamp DESC")
    fun getAll(): Flow<List<SavedGameMetadata>>

    // Inserta o reemplaza una entrada de metadatos
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: SavedGameMetadata)

    // Borra una entrada de metadatos usando su nombre de archivo
    @Query("DELETE FROM saved_game_metadata WHERE fileName = :fileName")
    suspend fun deleteByFileName(fileName: String)
}