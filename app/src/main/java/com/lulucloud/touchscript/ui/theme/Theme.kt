package com.lulucloud.touchscript.ui.theme

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
    primary = Color(0xFFE2A16C),
    onPrimary = Color(0xFF3B1A07),
    primaryContainer = Color(0xFF5C3115),
    onPrimaryContainer = Color(0xFFFFD9BC),
    secondary = Color(0xFF8CC9C0),
    onSecondary = Color(0xFF0A3833),
    secondaryContainer = Color(0xFF174D46),
    onSecondaryContainer = Color(0xFFC3EEE7),
    tertiary = Color(0xFFD7C0A4),
    onTertiary = Color(0xFF3A2A17),
    background = Color(0xFF111A24),
    onBackground = Color(0xFFF2EBDD),
    surface = Color(0xFF182330),
    onSurface = Color(0xFFF2EBDD),
    surfaceVariant = Color(0xFF233243),
    onSurfaceVariant = Color(0xFFC1C9D2),
    outline = Color(0xFF5A6877),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = WorkshopCopper,
    onPrimary = Color(0xFFFFF8F3),
    primaryContainer = WorkshopCopperSoft,
    onPrimaryContainer = WorkshopCopperDeep,
    secondary = WorkshopTeal,
    onSecondary = Color(0xFFF6FFFC),
    secondaryContainer = WorkshopTealSoft,
    onSecondaryContainer = Color(0xFF123E39),
    tertiary = WorkshopInkSoft,
    onTertiary = Color.White,
    background = WorkshopPaper,
    onBackground = WorkshopText,
    surface = Color(0xFFFFFBF7),
    onSurface = WorkshopText,
    surfaceVariant = WorkshopPaperWarm,
    onSurfaceVariant = WorkshopTextMuted,
    outline = Color(0xFFC7B39C),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
    onError = Color.White,
    onErrorContainer = Color(0xFF410E0B)
)

@Composable
fun TouchScriptTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
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
