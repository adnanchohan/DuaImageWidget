package com.watchfultech.mywidgets.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.watchfultech.mywidgets.data.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = Emerald300,
    onPrimary = Emerald950,
    primaryContainer = Emerald800,
    onPrimaryContainer = Emerald100,
    secondary = Gold300,
    onSecondary = Ink950,
    secondaryContainer = Emerald900,
    onSecondaryContainer = Emerald100,
    tertiary = Emerald200,
    onTertiary = Emerald950,
    background = DarkBackground,
    onBackground = Ivory50,
    surface = DarkSurface,
    onSurface = Ivory50,
    surfaceVariant = DarkSurfaceRaised,
    onSurfaceVariant = Emerald200,
    outline = Emerald600,
    outlineVariant = Emerald900,
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
)

private val LightColorScheme = lightColorScheme(
    primary = Emerald700,
    onPrimary = Color.White,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald950,
    secondary = Gold500,
    onSecondary = Ink950,
    secondaryContainer = Color(0xFFF5E8C6),
    onSecondaryContainer = Ink950,
    tertiary = Emerald500,
    onTertiary = Color.White,
    background = Ivory100,
    onBackground = Ink950,
    surface = Ivory50,
    onSurface = Ink950,
    surfaceVariant = Emerald100,
    onSurfaceVariant = Ink700,
    outline = Emerald300,
    outlineVariant = Sand200,
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
)

data class WidgetColors(
    val backgroundStart: Color,
    val backgroundEnd: Color,
    val heroBg: Color,
    val slot: Color,
    val accent: Color,
    val gold: Color,
    val accentText: Color,
    val text: Color,
    val secondaryText: Color,
    val liveText: Color,
    val isDark: Boolean,
)

val LightWidgetColors = WidgetColors(
    backgroundStart = Color(0xFF1C6B57),
    backgroundEnd = Color(0xFF103D32),
    heroBg = Color(0x26FFFFFF),
    slot = Color(0x1FFFFFFF),
    accent = Color(0xFFE6C985),
    gold = Color(0xFFE6C985),
    accentText = Color(0xFF3E2723),
    text = Color(0xFFFFFFFF),
    secondaryText = Color(0xCCFFFFFF),
    liveText = Color(0xFF3E2723),
    isDark = false,
)

val DarkWidgetColors = WidgetColors(
    backgroundStart = Color(0xFF0B352E),
    backgroundEnd = Color(0xFF06211C),
    heroBg = Color(0x26FFFFFF),
    slot = Color(0xFF1B4A41),
    accent = Color(0xFFF1C75B),
    gold = Color(0xFFF1C75B),
    accentText = Color(0xFF2B2209),
    text = Color(0xFFFFFDF7),
    secondaryText = Color(0xFFBED3CC),
    liveText = Color(0xFF2B2209),
    isDark = true,
)

val LocalWidgetColors = androidx.compose.runtime.staticCompositionLocalOf { LightWidgetColors }

@Composable
fun DuaImageWidgetTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
    val view = LocalView.current

    if (!view.isInEditMode) {
        (view.context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val widgetColors = if (darkTheme) DarkWidgetColors else LightWidgetColors
    androidx.compose.runtime.CompositionLocalProvider(
        LocalWidgetColors provides widgetColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content,
        )
    }
}
