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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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
 * Цвет Bottom Navigation Bar
 */
val ColorScheme.navigationBarColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (this == DarkColorScheme) DarkNavigationBarColor else LightNavigationBarColor

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
 * CompositionLocal для хранения кастомных цветов темы
 */
val LocalCustomColors = staticCompositionLocalOf<CustomThemeColors?> { null }

/**
 * Тема приложения SV APP
 *
 * @param themeMode режим темы (LIGHT, DARK или SYSTEM)
 * @param useDynamicColors использовать динамические цвета (Material You, Android 12+)
 * @param customColors кастомные цвета темы (null для использования стандартных)
 * @param content контент приложения
 */
@Composable
fun SVAPPTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    useDynamicColors: Boolean = false,
    customColors: CustomThemeColors? = null,
    content: @Composable () -> Unit,
) {
    // Для Xiaomi/MIUI: отслеживаем uiMode напрямую, а не через remember(configuration)
    // Configuration объект может быть тем же, а uiMode меняться
    val configuration = LocalConfiguration.current
    val isSystemDark = remember(configuration.uiMode) {
        val uiMode = configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    // Явно определяем тему: если режим не SYSTEM, используем выбранный режим
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemDark
    }

    val context = LocalContext.current
    val view = LocalView.current

    // Выбор цветовой схемы
    val colorScheme = remember(darkTheme, useDynamicColors, customColors) {
        val baseColorScheme = when {
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

        // Применение кастомных цветов если есть
        if (customColors != null) {
            applyCustomColors(baseColorScheme, customColors)
        } else {
            baseColorScheme
        }
    }

    // Настройка статус-бара и навигационной панели
    // Xiaomi/MIUI fix: DisposableEffect с key() вместо SideEffect
    if (!view.isInEditMode) {
        DisposableEffect(darkTheme) {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
            onDispose { }
        }
    }

    // Предоставление размеров и форм через CompositionLocal
    val appDimensions = AppDimensions.Default
    val appShapes = AppShapes.Default

    CompositionLocalProvider(
        LocalAppDimensions provides appDimensions,
        LocalAppShapes provides appShapes,
        LocalCustomTypography provides CustomTypography.Default,
        LocalThemeConfig provides ThemeConfig(themeMode, useDynamicColors),
        LocalCustomColors provides customColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = appShapes.toMaterialShapes(),
            content = content
        )
    }
}

/**
 * Применить кастомные цвета к базовой цветовой схеме.
 */
private fun applyCustomColors(
    baseScheme: ColorScheme,
    customColors: CustomThemeColors
): ColorScheme {
    return baseScheme.copy(
        primary = customColors.getColor("primary") ?: baseScheme.primary,
        onPrimary = customColors.getColor("onPrimary") ?: baseScheme.onPrimary,
        primaryContainer = customColors.getColor("primaryContainer") ?: baseScheme.primaryContainer,
        onPrimaryContainer = customColors.getColor("onPrimaryContainer") ?: baseScheme.onPrimaryContainer,

        secondary = customColors.getColor("secondary") ?: baseScheme.secondary,
        onSecondary = customColors.getColor("onSecondary") ?: baseScheme.onSecondary,
        secondaryContainer = customColors.getColor("secondaryContainer") ?: baseScheme.secondaryContainer,
        onSecondaryContainer = customColors.getColor("onSecondaryContainer") ?: baseScheme.onSecondaryContainer,

        tertiary = customColors.getColor("tertiary") ?: baseScheme.tertiary,
        onTertiary = customColors.getColor("onTertiary") ?: baseScheme.onTertiary,
        tertiaryContainer = customColors.getColor("tertiaryContainer") ?: baseScheme.tertiaryContainer,
        onTertiaryContainer = customColors.getColor("onTertiaryContainer") ?: baseScheme.onTertiaryContainer,

        background = customColors.getColor("background") ?: baseScheme.background,
        onBackground = customColors.getColor("onBackground") ?: baseScheme.onBackground,

        surface = customColors.getColor("surface") ?: baseScheme.surface,
        onSurface = customColors.getColor("onSurface") ?: baseScheme.onSurface,
        surfaceVariant = customColors.getColor("surfaceVariant") ?: baseScheme.surfaceVariant,
        onSurfaceVariant = customColors.getColor("onSurfaceVariant") ?: baseScheme.onSurfaceVariant,
        surfaceDim = customColors.getColor("surfaceDim") ?: baseScheme.surfaceDim,
        surfaceBright = customColors.getColor("surfaceBright") ?: baseScheme.surfaceBright,
        surfaceContainerLowest = customColors.getColor("surfaceContainerLowest") ?: baseScheme.surfaceContainerLowest,
        surfaceContainerLow = customColors.getColor("surfaceContainerLow") ?: baseScheme.surfaceContainerLow,
        surfaceContainer = customColors.getColor("surfaceContainer") ?: baseScheme.surfaceContainer,
        surfaceContainerHigh = customColors.getColor("surfaceContainerHigh") ?: baseScheme.surfaceContainerHigh,
        surfaceContainerHighest = customColors.getColor("surfaceContainerHighest") ?: baseScheme.surfaceContainerHighest,

        outline = customColors.getColor("outline") ?: baseScheme.outline,
        outlineVariant = customColors.getColor("outlineVariant") ?: baseScheme.outlineVariant,

        error = customColors.getColor("error") ?: baseScheme.error,
        onError = customColors.getColor("onError") ?: baseScheme.onError,
        errorContainer = customColors.getColor("errorContainer") ?: baseScheme.errorContainer,
        onErrorContainer = customColors.getColor("onErrorContainer") ?: baseScheme.onErrorContainer,

        inverseSurface = customColors.getColor("inverseSurface") ?: baseScheme.inverseSurface,
        inverseOnSurface = customColors.getColor("inverseOnSurface") ?: baseScheme.inverseOnSurface,
        inversePrimary = customColors.getColor("inversePrimary") ?: baseScheme.inversePrimary,
    )
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
