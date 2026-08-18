package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = Cyan500,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Cyan400,
    secondary = Emerald500,
    onSecondary = Color(0xFF003919),
    secondaryContainer = Color(0xFF005327),
    onSecondaryContainer = Emerald400,
    tertiary = Violet400,
    onTertiary = Color(0xFF27005D),
    tertiaryContainer = Color(0xFF431F86),
    onTertiaryContainer = Color(0xFFEADBFF),
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = DarkSurfaceContainer,
    error = Red500,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA6EEFF),
    onPrimaryContainer = Color(0xFF001F25),
    secondary = Color(0xFF006D3A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF95F8B2),
    onSecondaryContainer = Color(0xFF00210D),
    tertiary = TertiaryLight,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCE1FF),
    onTertiaryContainer = Color(0xFF111837),
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainer = LightSurfaceContainer,
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

@Composable
fun NROnlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep distinctive 5G cyber styling
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
