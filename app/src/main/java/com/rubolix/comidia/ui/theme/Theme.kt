package com.rubolix.comidia.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = Secondary,
    onSecondary = OnSecondary,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = Color(0xFFF5EDE5),
    onSurfaceVariant = WarmGray
)

private val DarkColorScheme = darkColorScheme(
    primary = SaffronLight,
    onPrimary = DeepBrown,
    secondary = OliveGreen,
    onSecondary = Color.White,
    tertiary = Terracotta,
    onTertiary = Color.White,
    background = Color(0xFF1A1410),
    onBackground = CreamWhite,
    surface = Color(0xFF2D2520),
    onSurface = CreamWhite
)

@Composable
fun ComiDiaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
