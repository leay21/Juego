package com.example.juego.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// --- COLORES DEL JUEGO ---
val Rojo = Color(0xFFE53935)
val Azul = Color(0xFF1E88E5)
val Verde = Color(0xFF43A047)
val Amarillo = Color(0xFFFFEB3B)
val Naranja = Color(0xFFF4511E)
val GrisEspera = Color(0xFF9E9E9E)
val GrisFondo = Color(0xFF212121)

// --- TEMA POR DEFECTO (Material) ---
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val LightDefaultColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)
val DarkDefaultColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

// --- TEMA IPN (Guinda) ---
val IpnGuinda = Color(0xFF6A0035)
val IpnGuindaClaro = Color(0xFFA03F64)
val IpnDorado = Color(0xFFD4AF37)

val LightIpnColorScheme = lightColorScheme(
    primary = IpnGuinda,
    onPrimary = Color.White,
    secondary = IpnDorado,
    onSecondary = Color.Black,
    background = Color(0xFFFDF8F8),
    onBackground = Color.Black,
    surface = Color(0xFFFFFFFF),
    onSurface = Color.Black
)
val DarkIpnColorScheme = darkColorScheme(
    primary = IpnGuindaClaro,
    onPrimary = Color.White,
    secondary = IpnDorado,
    onSecondary = Color.Black,
    background = Color(0xFF1C1B1F),
    onBackground = Color.White,
    surface = Color(0xFF332D2F),
    onSurface = Color.White
)

// --- TEMA ESCOM (Azul) ---
val EscaAzul = Color(0xFF003366)
val EscaAzulClaro = Color(0xFF5A91C4)
val EscaGris = Color(0xFFB0B0B0)

val LightEscaColorScheme = lightColorScheme(
    primary = EscaAzul,
    onPrimary = Color.White,
    secondary = EscaGris,
    onSecondary = Color.Black,
    background = Color(0xFFF8F9FA),
    onBackground = Color.Black,
    surface = Color(0xFFFFFFFF),
    onSurface = Color.Black
)
val DarkEscaColorScheme = darkColorScheme(
    primary = EscaAzulClaro,
    onPrimary = Color.White,
    secondary = EscaGris,
    onSecondary = Color.Black,
    background = Color(0xFF1C1B1F),
    onBackground = Color.White,
    surface = Color(0xFF2E343A),
    onSurface = Color.White
)