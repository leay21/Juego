package com.example.juego

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember // ¡IMPORT AÑADIDO!
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight // ¡IMPORT AÑADIDO!
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.juego.data.SaveFormat
import com.example.juego.ui.theme.AppTheme
import com.example.juego.data.SavedGameMetadata
import com.example.juego.GameMode // ¡IMPORT AÑADIDO!
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel,
    reflexViewModel: ReflexViewModel
) {
    // --- Obtener todos los estados ---
    val currentTheme by settingsViewModel.appTheme.collectAsState()
    val currentFormat by settingsViewModel.saveFormat.collectAsState()
    val savedGames by settingsViewModel.savedGames.collectAsState()

    // ¡CORREGIDO! 'LaunchedEffect' para refrescar la lista no es necesario
    // porque estamos usando un Flow de Room, que se actualiza solo.

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes y Partidas") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            // --- SECCIÓN 1: SELECCIONAR TEMA ---
            item {
                SectionTitle("Seleccionar Tema")
            }
            items(AppTheme.entries.toTypedArray()) { theme ->
                SelectableRow(
                    title = theme.name.lowercase().replaceFirstChar { it.uppercase() },
                    selected = (theme == currentTheme),
                    onClick = { settingsViewModel.setAppTheme(theme) }
                )
            }

            // --- SECCIÓN 2: FORMATO DE GUARDADO ---
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionTitle("Formato de Guardado Preferido")
            }
            items(SaveFormat.entries.toTypedArray()) { format ->
                SelectableRow(
                    title = format.name,
                    selected = (format == currentFormat),
                    onClick = { settingsViewModel.setSaveFormat(format) }
                )
            }

            // --- SECCIÓN 3: PARTIDAS GUARDADAS ---
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionTitle("Partidas Guardadas")
            }
            if (savedGames.isEmpty()) {
                item {
                    Text(
                        "No hay partidas guardadas.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(savedGames, key = { it.fileName }) { metadata ->
                    SavedGameItem(
                        metadata = metadata,
                        onLoad = {
                            reflexViewModel.loadGame(metadata.fileName)
                            navController.navigate(Screen.Game.route) {
                                popUpTo(Screen.Home.route)
                            }
                        },
                        onDelete = {
                            settingsViewModel.deleteGame(metadata)
                        }
                    )
                }
            }
        }
    }
}

// --- COMPOSABLES REUTILIZABLES ---

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SelectableRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title)
    }
}

@Composable
private fun SavedGameItem(
    metadata: SavedGameMetadata,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    // Función para formatear el timestamp
    val formattedDate = remember(metadata.timestamp) {
        val sdf = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault())
        sdf.format(Date(metadata.timestamp))
    }

    // Función para formatear el título del modo
    val formattedMode = remember(metadata.gameMode) {
        when(metadata.gameMode) {
            GameMode.CLASSIC -> "Clásico"
            GameMode.TIME_ATTACK -> "Contrarreloj"
            GameMode.CONFUSION -> "Confusión"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // --- Columna de Metadatos ---
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$formattedMode (${metadata.scoreJ1} - ${metadata.scoreJ2})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Botones de acción
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onLoad) {
                    Text("Cargar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Borrar")
                }
            }
        }
    }
}