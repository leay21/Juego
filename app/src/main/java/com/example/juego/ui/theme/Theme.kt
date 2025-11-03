package com.example.juego.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Enum para definir los temas disponibles
enum class AppTheme {
    SYSTEM,
    IPN,
    ESCOM
}

@Composable
fun JuegoTheme(
    appTheme: AppTheme = AppTheme.SYSTEM, // El tema actual
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current

    val colorScheme = when (appTheme) {
        AppTheme.IPN -> if (darkTheme) DarkIpnColorScheme else LightIpnColorScheme
        AppTheme.ESCOM -> if (darkTheme) DarkEscaColorScheme else LightEscaColorScheme
        AppTheme.SYSTEM -> when {
            dynamicColor && darkTheme -> dynamicDarkColorScheme(context)
            dynamicColor && !darkTheme -> dynamicLightColorScheme(context)
            darkTheme -> DarkDefaultColorScheme
            else -> LightDefaultColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}