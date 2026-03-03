package dev.taxmachine.gymapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import dev.taxmachine.gymapp.db.CustomThemeColorsEntity
import dev.taxmachine.gymapp.db.GymDatabase

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun GymAppTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val darkTheme = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    // Optimization: Remember the database and the flow to avoid re-querying on every recomposition
    val db = remember { GymDatabase.getDatabase(context) }
    val customColorsFlow = remember(db, darkTheme) { db.gymDao().getCustomThemeColors(darkTheme) }
    val customColors by customColorsFlow.collectAsState(initial = null)

    val colorScheme = when {
        customColors != null -> customColors!!.toColorScheme()
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
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

fun CustomThemeColorsEntity.toColorScheme(): ColorScheme {
    return if (isDark) {
        darkColorScheme(
            primary = Color(primary),
            onPrimary = Color(onPrimary),
            secondary = Color(secondary),
            onSecondary = Color(onSecondary),
            tertiary = Color(tertiary),
            onTertiary = Color(onTertiary),
            background = Color(background),
            onBackground = Color(onBackground),
            surface = Color(surface),
            onSurface = Color(onSurface),
            error = Color(error),
            onError = Color(onError)
        )
    } else {
        lightColorScheme(
            primary = Color(primary),
            onPrimary = Color(onPrimary),
            secondary = Color(secondary),
            onSecondary = Color(onSecondary),
            tertiary = Color(tertiary),
            onTertiary = Color(onTertiary),
            background = Color(background),
            onBackground = Color(onBackground),
            surface = Color(surface),
            onSurface = Color(onSurface),
            error = Color(error),
            onError = Color(onError)
        )
    }
}

fun ColorScheme.toEntity(isDark: Boolean): CustomThemeColorsEntity {
    return CustomThemeColorsEntity(
        isDark = isDark,
        primary = primary.toArgb().toLong(),
        onPrimary = onPrimary.toArgb().toLong(),
        secondary = secondary.toArgb().toLong(),
        onSecondary = onSecondary.toArgb().toLong(),
        tertiary = tertiary.toArgb().toLong(),
        onTertiary = onTertiary.toArgb().toLong(),
        background = background.toArgb().toLong(),
        onBackground = onBackground.toArgb().toLong(),
        surface = surface.toArgb().toLong(),
        onSurface = onSurface.toArgb().toLong(),
        error = error.toArgb().toLong(),
        onError = onError.toArgb().toLong()
    )
}
