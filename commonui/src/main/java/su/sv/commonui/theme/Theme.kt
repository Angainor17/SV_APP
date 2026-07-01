package su.sv.commonui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ============================================================
// COLOR SCHEMES
// ============================================================

/**
 * Тёмная цветовая схема
 */
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,

    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,

    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,

    background = DarkBackground,
    onBackground = DarkOnBackground,

    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceDim = DarkSurfaceDim,
    surfaceBright = DarkSurfaceBright,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,

    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,

    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,

    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    inversePrimary = DarkInversePrimary,
)

/**
 * Светлая цветовая схема
 */
private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,

    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,

    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,

    background = LightBackground,
    onBackground = LightOnBackground,

    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceDim = LightSurfaceDim,
    surfaceBright = LightSurfaceBright,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,

    outline = LightOutline,
    outlineVariant = LightOutlineVariant,

    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,

    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,
    inversePrimary = LightInversePrimary,
)

// ============================================================
// EXTENSION PROPERTIES
// ============================================================

/**
 * Цвет ссылок в зависимости от темы
 */
val ColorScheme.linkColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (this == DarkColorScheme) DarkLinkColor else LightLinkColor

/**
 * Цвет обводки карточки
 */
val ColorScheme.cardStroke: Color
    @Composable
    @ReadOnlyComposable
    get() = if (this == DarkColorScheme) CardStrokeDark else CardStrokeLight

/**
 * Функциональные цвета (одинаковы для обеих тем)
 */
val ColorScheme.success: Color
    @ReadOnlyComposable
    get() = FunctionalSuccess

val ColorScheme.warning: Color
    @ReadOnlyComposable
    get() = FunctionalWarning

val ColorScheme.info: Color
    @ReadOnlyComposable
    get() = FunctionalInfo

val ColorScheme.danger: Color
    @ReadOnlyComposable
    get() = FunctionalDanger

/**
 * Цвет избранного (красный для обеих тем)
 */
val ColorScheme.favorite: Color
    @ReadOnlyComposable
    get() = FavoriteColor

// ============================================================
// COMPOSITION LOCAL
// ============================================================

/**
 * CompositionLocal для хранения конфигурации темы
 */
val LocalThemeConfig = staticCompositionLocalOf { ThemeConfig.Default }

// ============================================================
// THEME
// ============================================================

/**
 * Тема приложения SV APP
 *
 * @param themeMode режим темы (LIGHT или DARK)
 * @param useDynamicColors использовать динамические цвета (Material You, Android 12+)
 * @param content контент приложения
 */
@Composable
fun SVAPPTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    useDynamicColors: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = themeMode.isDarkTheme()

    val context = LocalContext.current
    val view = LocalView.current

    // Выбор цветовой схемы
    val colorScheme = when {
        useDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Настройка статус-бара и навигационной панели
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    // Предоставление размеров и форм через CompositionLocal
    val appDimensions = AppDimensions.Default
    val appShapes = AppShapes.Default

    CompositionLocalProvider(
        LocalAppDimensions provides appDimensions,
        LocalAppShapes provides appShapes,
        LocalCustomTypography provides CustomTypography.Default,
        LocalThemeConfig provides ThemeConfig(themeMode, useDynamicColors)
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = appShapes.toMaterialShapes(),
            content = content
        )
    }
}

// ============================================================
// PREVIEW THEMES
// ============================================================

/**
 * Светлая тема для Preview
 */
@Composable
fun SVAPPThemeLightPreview(
    content: @Composable () -> Unit
) {
    SVAPPTheme(themeMode = ThemeMode.LIGHT, content = content)
}

/**
 * Тёмная тема для Preview
 */
@Composable
fun SVAPPThemeDarkPreview(
    content: @Composable () -> Unit
) {
    SVAPPTheme(themeMode = ThemeMode.DARK, content = content)
}
