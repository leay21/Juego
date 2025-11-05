package com.example.juego.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// ¡MODIFICADO!
// 1. Añade SavedGameMetadata a la lista de 'entities'
// 2. Incrementa la 'version' de 1 a 2
@Database(entities = [GameStats::class, SavedGameMetadata::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun statsDao(): StatsDao
    abstract fun savedGameMetadataDao(): SavedGameMetadataDao // ¡NUEVO!

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "reflex_game_database"
                )
                    // Una migración destructiva es más fácil para desarrollo.
                    // Borra la BD si la versión cambia.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}