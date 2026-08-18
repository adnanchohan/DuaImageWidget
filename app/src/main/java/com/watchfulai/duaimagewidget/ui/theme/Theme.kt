package com.watchfulai.duaimagewidget.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.watchfulai.duaimagewidget.data.AppTheme

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

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
