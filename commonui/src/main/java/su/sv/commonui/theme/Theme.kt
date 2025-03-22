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
//
//    surface = PurpleGrey80,
//    onSurface = Color.White,

    tertiaryContainer = PurpleGrey40,
    onSurface = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = DarkBlue40,
    secondary = Grey40,
    tertiary = Pink40,

    tertiaryContainer = PurpleGrey80,
    onTertiary = White,



    /* Other default colors to override
    background = Color(0xFFFFFBFE),

    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
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
        content = content
    )
}