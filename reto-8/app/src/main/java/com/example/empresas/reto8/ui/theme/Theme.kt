package com.example.empresas.reto8.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Paleta propia (azul petróleo + ámbar) para Android < 12, donde no hay color dinámico
private val LightColors = lightColorScheme(
    primary = Color(0xFF00677F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7EAFF),
    onPrimaryContainer = Color(0xFF001F28),
    secondary = Color(0xFF4C626A),
    secondaryContainer = Color(0xFFCFE6F0),
    tertiary = Color(0xFF8B5000),
    tertiaryContainer = Color(0xFFFFDCBE),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5DD5FC),
    onPrimary = Color(0xFF003543),
    primaryContainer = Color(0xFF004E60),
    onPrimaryContainer = Color(0xFFB7EAFF),
    secondary = Color(0xFFB3CAD4),
    secondaryContainer = Color(0xFF344A52),
    tertiary = Color(0xFFFFB870),
    tertiaryContainer = Color(0xFF6A3C00),
)

@Composable
fun Reto8Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
