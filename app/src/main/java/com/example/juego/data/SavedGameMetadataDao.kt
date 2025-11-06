package com.example.juego.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update // ¡NUEVO IMPORT!
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedGameMetadataDao {

    // ¡MODIFICADO!
    // Ordena por favoritas (DESC) y luego por fecha (DESC)
    @Query("SELECT * FROM saved_game_metadata ORDER BY isFavorite DESC, timestamp DESC")
    fun getAll(): Flow<List<SavedGameMetadata>>

    // Inserta o reemplaza una entrada de metadatos
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: SavedGameMetadata)

    // ¡NUEVO! Función para actualizar una entrada (para el 'toggle')
    @Update
    suspend fun update(metadata: SavedGameMetadata)

    // Borra una entrada de metadatos usando su nombre de archivo
    @Query("DELETE FROM saved_game_metadata WHERE fileName = :fileName")
    suspend fun deleteByFileName(fileName: String)
}