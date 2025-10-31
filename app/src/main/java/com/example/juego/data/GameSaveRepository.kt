package com.example.juego.data

import android.content.Context
import android.util.Log
import android.util.Xml // ¡NUEVO! Para el Serializer
import com.example.juego.GamePhase
import com.example.juego.GameUiState
import com.google.gson.Gson
import org.xmlpull.v1.XmlPullParser // ¡NUEVO! Para el Parser
import org.xmlpull.v1.XmlSerializer // ¡NUEVO!
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.StringWriter

// Enum para los formatos de archivo
enum class SaveFormat(val extension: String) {
    JSON(".json"),
    XML(".xml"),
    TXT(".txt")
}

class GameSaveRepository(private val context: Context) {

    // Instancia de Gson para serialización JSON
    private val gson = Gson()

    // --- GUARDADO ---

    /**
     * Guarda el estado del juego en el formato especificado.
     */
    fun saveGame(state: GameUiState, fileName: String, format: SaveFormat) {
        // Asegura que el nombre de archivo no tenga la extensión,
        // ya que la añadiremos nosotros.
        val cleanFileName = fileName.removeSuffix(format.extension)

        when (format) {
            SaveFormat.JSON -> saveAsJson(state, cleanFileName)
            SaveFormat.XML -> saveAsXml(state, cleanFileName) // ¡LOGICA AÑADIDA!
            SaveFormat.TXT -> saveAsTxt(state, cleanFileName) // Lógica ya existente
        }
    }

    private fun saveAsJson(state: GameUiState, fileName: String) {
        try {
            // Convierte el objeto GameUiState a un String en formato JSON
            val jsonState = gson.toJson(state)

            // Escribe el string en un archivo en el almacenamiento interno
            val file = File(context.filesDir, "$fileName${SaveFormat.JSON.extension}")
            OutputStreamWriter(file.outputStream()).use {
                it.write(jsonState)
            }
            Log.i("GameSaveRepository", "Partida guardada en: ${file.absolutePath}")

        } catch (e: Exception) {
            Log.e("GameSaveRepository", "Error al guardar JSON", e)
        }
    }

    // --- ¡NUEVA LÓGICA DE GUARDADO XML! ---
    private fun saveAsXml(state: GameUiState, fileName: String) {
        val serializer: XmlSerializer = Xml.newSerializer()
        val stringWriter = StringWriter() // Escribimos a un String primero

        try {
            serializer.setOutput(stringWriter)
            serializer.startDocument("UTF-8", true)
            serializer.startTag(null, "GameUiState")

            // Escribir cada campo como una etiqueta
            fun writeTag(tag: String, text: String) {
                serializer.startTag(null, tag)
                serializer.text(text)
                serializer.endTag(null, tag)
            }

            writeTag("scoreJ1", state.scoreJ1.toString())
            writeTag("scoreJ2", state.scoreJ2.toString())
            writeTag("gameState", state.gameState.name)
            writeTag("targetColorName", state.targetColorName)
            writeTag("roundColorName", state.roundColorName)
            writeTag("winnerMessage", state.winnerMessage)
            writeTag("timeElapsed", state.timeElapsed.toString())

            // Escribir la lista de historial
            serializer.startTag(null, "moveHistory")
            state.moveHistory.forEach { move ->
                writeTag("move", move)
            }
            serializer.endTag(null, "moveHistory")

            serializer.endTag(null, "GameUiState")
            serializer.endDocument()

            // Escribir el String final al archivo
            val file = File(context.filesDir, "$fileName${SaveFormat.XML.extension}")
            OutputStreamWriter(file.outputStream()).use {
                it.write(stringWriter.toString())
            }
            Log.i("GameSaveRepository", "Partida guardada en: ${file.absolutePath}")

        } catch (e: Exception) {
            Log.e("GameSaveRepository", "Error al guardar XML", e)
        }
    }

    private fun saveAsTxt(state: GameUiState, fileName: String) {
        // Implementación de guardado en texto plano (clave=valor)
        try {
            val txtState = """
                scoreJ1=${state.scoreJ1}
                scoreJ2=${state.scoreJ2}
                gameState=${state.gameState}
                targetColorName=${state.targetColorName}
                roundColorName=${state.roundColorName}
                timeElapsed=${state.timeElapsed}
                winnerMessage=${state.winnerMessage}
                moveHistory=${state.moveHistory.joinToString(";;")}
            """.trimIndent() // Usamos ";;" como separador para el historial

            val file = File(context.filesDir, "$fileName${SaveFormat.TXT.extension}")
            OutputStreamWriter(file.outputStream()).use {
                it.write(txtState)
            }
            Log.i("GameSaveRepository", "Partida guardada en: ${file.absolutePath}")

        } catch (e: Exception) {
            Log.e("GameSaveRepository", "Error al guardar TXT", e)
        }
    }

    // --- CARGA ---

    /**
     * Carga el estado del juego desde un archivo.
     */
    fun loadGame(fileNameWithExtension: String): GameUiState? {
        val format = when {
            fileNameWithExtension.endsWith(SaveFormat.JSON.extension) -> SaveFormat.JSON
            fileNameWithExtension.endsWith(SaveFormat.XML.extension) -> SaveFormat.XML
            fileNameWithExtension.endsWith(SaveFormat.TXT.extension) -> SaveFormat.TXT
            else -> null
        }

        if (format == null) {
            Log.e("GameSaveRepository", "Formato de archivo desconocido: $fileNameWithExtension")
            return null
        }

        // ¡MODIFICADO! Llamamos a las nuevas funciones
        return when (format) {
            SaveFormat.JSON -> loadFromJson(fileNameWithExtension)
            SaveFormat.XML -> loadFromXml(fileNameWithExtension)
            SaveFormat.TXT -> loadFromTxt(fileNameWithExtension)
        }
    }

    private fun loadFromJson(fileName: String): GameUiState? {
        try {
            val file = File(context.filesDir, fileName)
            if (!file.exists() || !file.canRead()) {
                Log.w("GameSaveRepository", "El archivo $fileName no existe o no se puede leer")
                return null
            }

            // Lee el archivo y lo convierte de JSON a objeto GameUiState
            InputStreamReader(file.inputStream()).use {
                return gson.fromJson(it, GameUiState::class.java)
            }
        } catch (e: Exception) {
            Log.e("GameSaveRepository", "Error al cargar JSON", e)
            return null
        }
    }

    // --- ¡NUEVA LÓGICA DE CARGA XML! ---
    private fun loadFromXml(fileName: String): GameUiState? {
        val file = File(context.filesDir, fileName)
        if (!file.exists() || !file.canRead()) {
            Log.w("GameSaveRepository", "El archivo $fileName no existe o no se puede leer")
            return null
        }

        val parser: XmlPullParser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)

        // Variables temporales para construir el estado
        var scoreJ1 = 0
        var scoreJ2 = 0
        var gameState: GamePhase = GamePhase.ESPERA
        var targetColorName = ""
        var roundColorName = ""
        var winnerMessage = ""
        var timeElapsed = 0L
        val moveHistory = mutableListOf<String>()

        try {
            InputStreamReader(file.inputStream()).use { reader ->
                parser.setInput(reader)

                var eventType = parser.eventType
                var currentTag: String? = null

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> currentTag = parser.name
                        XmlPullParser.TEXT -> {
                            currentTag?.let {
                                when (it) {
                                    "scoreJ1" -> scoreJ1 = parser.text.toIntOrNull() ?: 0
                                    "scoreJ2" -> scoreJ2 = parser.text.toIntOrNull() ?: 0
                                    "gameState" -> gameState = try { GamePhase.valueOf(parser.text) } catch (e: Exception) { GamePhase.ESPERA }
                                    "targetColorName" -> targetColorName = parser.text
                                    "roundColorName" -> roundColorName = parser.text
                                    "winnerMessage" -> winnerMessage = parser.text
                                    "timeElapsed" -> timeElapsed = parser.text.toLongOrNull() ?: 0L
                                    "move" -> moveHistory.add(parser.text)
                                    else -> {} // Ignorar otras etiquetas
                                }
                                currentTag = null // Reseteamos la etiqueta actual
                            }
                        }
                    }
                    eventType = parser.next()
                }
            }

            // Construir el objeto GameUiState final
            return GameUiState(
                scoreJ1 = scoreJ1,
                scoreJ2 = scoreJ2,
                gameState = gameState,
                targetColorName = targetColorName,
                roundColorName = roundColorName,
                winnerMessage = winnerMessage,
                timeElapsed = timeElapsed,
                moveHistory = moveHistory
            )

        } catch (e: Exception) {
            Log.e("GameSaveRepository", "Error al cargar XML", e)
            return null
        }
    }

    // --- ¡NUEVA LÓGICA DE CARGA TXT! ---
    private fun loadFromTxt(fileName: String): GameUiState? {
        val file = File(context.filesDir, fileName)
        if (!file.exists() || !file.canRead()) {
            Log.w("GameSaveRepository", "El archivo $fileName no existe o no se puede leer")
            return null
        }

        try {
            // Usamos un mapa para almacenar los valores leídos
            val values = mutableMapOf<String, String>()

            file.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val parts = line.split("=", limit = 2)
                    if (parts.size == 2) {
                        values[parts[0]] = parts[1]
                    }
                }
            }

            // Construir el objeto GameUiState desde el mapa
            return GameUiState(
                scoreJ1 = values["scoreJ1"]?.toIntOrNull() ?: 0,
                scoreJ2 = values["scoreJ2"]?.toIntOrNull() ?: 0,
                gameState = try { GamePhase.valueOf(values["gameState"] ?: "ESPERA") } catch (e: Exception) { GamePhase.ESPERA },
                targetColorName = values["targetColorName"] ?: "",
                roundColorName = values["roundColorName"] ?: "",
                winnerMessage = values["winnerMessage"] ?: "",
                timeElapsed = values["timeElapsed"]?.toLongOrNull() ?: 0L,
                moveHistory = values["moveHistory"]?.split(";;") ?: emptyList()
            )

        } catch (e: Exception) {
            Log.e("GameSaveRepository", "Error al cargar TXT", e)
            return null
        }
    }

    // --- LISTAR PARTIDAS ---

    /**
     * Devuelve una lista de los nombres de todos los archivos de guardado.
     */
    fun listSavedGames(): List<String> {
        return context.filesDir.listFiles { file ->
            // Filtra solo los archivos que terminen con nuestras extensiones
            file.isFile && (
                    file.name.endsWith(SaveFormat.JSON.extension) ||
                            file.name.endsWith(SaveFormat.XML.extension) ||
                            file.name.endsWith(SaveFormat.TXT.extension)
                    )
        }?.map { it.name } ?: emptyList() // Devuelve la lista de nombres o una lista vacía
    }

    /**
     * Elimina un archivo de guardado por su nombre.
     */
    fun deleteGame(fileNameWithExtension: String): Boolean {
        return try {
            val file = File(context.filesDir, fileNameWithExtension)
            if (file.exists()) {
                file.delete()
            } else {
                Log.w("GameSaveRepository", "El archivo $fileNameWithExtension no se encontró para eliminar")
                false
            }
        } catch (e: Exception) {
            Log.e("GameSaveRepository", "Error al eliminar $fileNameWithExtension", e)
            false
        }
    }
}