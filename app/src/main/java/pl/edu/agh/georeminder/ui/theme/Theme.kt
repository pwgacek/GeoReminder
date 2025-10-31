package pl.edu.agh.georeminder.ui.theme

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
    primary = Blue80,
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF004A77),
    onPrimaryContainer = Color(0xFFD1E4FF),

    secondary = BlueGrey80,
    onSecondary = Color(0xFF2C3E45),
    secondaryContainer = Color(0xFF42555C),
    onSecondaryContainer = Color(0xFFD7E3EA),

    tertiary = Teal80,
    onTertiary = Color(0xFF003731),
    tertiaryContainer = Color(0xFF1F4E47),
    onTertiaryContainer = Color(0xFFB2DFDB),

    background = DarkBackground,
    onBackground = Color(0xFFE6E6E6),

    surface = DarkSurface,
    onSurface = Color(0xFFE6E6E6),
    surfaceVariant = Color(0xFF424242),
    onSurfaceVariant = Color(0xFFC2C2C2),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    outline = Color(0xFF8C8C8C),
    outlineVariant = Color(0xFF444444)
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D35),

    secondary = BlueGrey40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7E3EA),
    onSecondaryContainer = Color(0xFF0F1D23),

    tertiary = Teal40,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB2DFDB),
    onTertiaryContainer = Color(0xFF002019),

    background = LightBackground,
    onBackground = Color(0xFF1A1A1A),

    surface = LightSurface,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFF424242),

    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    outline = Color(0xFF737373),
    outlineVariant = Color(0xFFC2C2C2)
)

@Composable
fun GeoReminderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

//    = when {
//        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//            val context = LocalContext.current
//            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//        }
//
//        darkTheme -> DarkColorScheme
//        else -> LightColorScheme
//    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}