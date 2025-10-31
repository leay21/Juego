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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.juego.data.SaveFormat
import com.example.juego.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel,
    reflexViewModel: ReflexViewModel // ¡NUEVO! Para cargar partidas
) {
    // --- Obtener todos los estados ---
    val currentTheme by settingsViewModel.appTheme.collectAsState()
    val currentFormat by settingsViewModel.saveFormat.collectAsState()
    val savedGames by settingsViewModel.savedGames.collectAsState()

    // ¡NUEVO! Actualiza la lista de partidas cada vez que entras a esta pantalla
    LaunchedEffect(key1 = Unit) {
        settingsViewModel.refreshSavedGamesList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes y Partidas") }, // Título actualizado
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        // ¡MODIFICADO! Usamos LazyColumn para toda la pantalla
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
                // Genera un item por cada archivo de partida guardado
                items(savedGames) { fileName ->
                    SavedGameItem(
                        fileName = fileName,
                        onLoad = {
                            // 1. Llama al ReflexViewModel para cargar el estado
                            reflexViewModel.loadGame(fileName)

                            // 2. Navega de vuelta a la pantalla de juego
                            navController.navigate(Screen.Game.route) {
                                // Limpia el stack para que "Ajustes" no quede atrás
                                popUpTo(Screen.Home.route)
                            }
                        },
                        onDelete = {
                            // Llama al SettingsViewModel para borrar el archivo
                            settingsViewModel.deleteGame(fileName)
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
            onClick = null // El clic se maneja en el Row
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title)
    }
}

@Composable
private fun SavedGameItem(
    fileName: String,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
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
            // Nombre del archivo
            Text(
                text = fileName,
                modifier = Modifier.weight(1f), // Ocupa el espacio disponible
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            // Botones de acción
            Row {
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