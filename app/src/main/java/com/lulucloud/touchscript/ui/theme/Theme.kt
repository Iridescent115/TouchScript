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
    primary = WorkshopOrange,
    secondary = WorkshopMint,
    tertiary = WorkshopCream,
    background = WorkshopInk,
    surface = Color(0xFF1D2A39),
    onPrimary = Color(0xFF1C120A),
    onSecondary = WorkshopInk,
    onBackground = Color(0xFFF5F0E7),
    onSurface = Color(0xFFF5F0E7)
)

private val LightColorScheme = lightColorScheme(
    primary = WorkshopOrange,
    secondary = WorkshopMint,
    tertiary = WorkshopOrangeDeep,
    background = WorkshopPaper,
    surface = WorkshopCream,
    onPrimary = Color.White,
    onSecondary = WorkshopInk,
    onTertiary = Color.White,
    onBackground = WorkshopText,
    onSurface = WorkshopText
)

@Composable
fun TouchScriptTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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
