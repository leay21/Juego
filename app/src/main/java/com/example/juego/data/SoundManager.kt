package com.example.juego.data

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.example.juego.R

class SoundManager(private val context: Context) {

    // Enum para identificar los sonidos
    enum class SoundType {
        ACIERTO,
        ERROR,
        GANAR
    }

    private val soundPool: SoundPool
    private var soundIds = mutableMapOf<SoundType, Int>()

    init {
        // Configurar atributos de audio para el juego
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // Inicializar SoundPool
        soundPool = SoundPool.Builder()
            .setMaxStreams(3) // Máximo 3 sonidos a la vez
            .setAudioAttributes(audioAttributes)
            .build()

        // Cargar los sonidos (esto puede fallar si los archivos no existen)
        try {
            soundIds[SoundType.ACIERTO] = soundPool.load(context, R.raw.acierto, 1)
            soundIds[SoundType.ERROR] = soundPool.load(context, R.raw.error, 1)
            soundIds[SoundType.GANAR] = soundPool.load(context, R.raw.ganar, 1)
        } catch (e: Exception) {
            Log.e("SoundManager", "Error al cargar sonidos. ¿Existen los archivos en res/raw?", e)
        }
    }

    /**
     * Reproduce un sonido basado en su tipo.
     */
    fun play(sound: SoundType) {
        val soundId = soundIds[sound]
        if (soundId != null) {
            soundPool.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f)
        } else {
            Log.w("SoundManager", "No se pudo reproducir el sonido $sound: ID no encontrado")
        }
    }

    /**
     * Libera los recursos del SoundPool.
     */
    fun release() {
        soundPool.release()
    }
}