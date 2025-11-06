package com.example.juego.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// ¡MODIFICADO!
// Incrementa la 'version' de 2 a 3
@Database(entities = [GameStats::class, SavedGameMetadata::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun statsDao(): StatsDao
    abstract fun savedGameMetadataDao(): SavedGameMetadataDao

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
                    // fallbackToDestructiveMigration borrará la BD
                    // y la recreará con el nuevo campo.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}