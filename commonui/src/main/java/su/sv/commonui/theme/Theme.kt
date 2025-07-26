package su.sv.commonui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = Grey80,
    tertiary = Pink80,

    // текст карточек - заголовок
    onPrimary = Color.LightGray,

    // синий текст для кнопки ещё
    surfaceBright = Color(0xFF6161FF),

    surfaceContainer = Color.DarkGray,
    onSurface = Color.White,

    tertiaryContainer = PurpleGrey40,
    onTertiary = White,
)

private val LightColorScheme = lightColorScheme(
    primary = DarkBlue40,
    secondary = Grey40,
    tertiary = Pink40,

    // текст карточек - заголовок
    onPrimary = Color.DarkGray,

    // синий текст для кнопки ещё
    surfaceBright = Color.Blue,

    surfaceContainer = Color.LightGray,

    tertiaryContainer = PurpleGrey80,
    onTertiary = White,
)

@Composable
fun SVAPPTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}