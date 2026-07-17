package su.sv.commonui.theme

import androidx.compose.ui.graphics.Color

/**
 * Цветовая палитра SV APP
 * Основана на Material Design 3 с кастомизацией под бренд
 */

// ============================================================
// LIGHT THEME COLORS
// ============================================================

// Primary - основной цвет бренда (Indigo)
val LightPrimary = Color(0xFF3F51B5)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE8E8F0)
val LightOnPrimaryContainer = Color(0xFF1A1A2E)

// Secondary - акцентный цвет
val LightSecondary = Color(0xFF5C6BC0)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFE8EAF6)
val LightOnSecondaryContainer = Color(0xFF1A237E)

// Tertiary - используется для карточек книг
// ВАЖНО: tertiaryContainer - фон карточки, onTertiary - текст на карточке
// Согласуется с WarmGray палитрой: карточки темнее фона
val LightTertiary = Color(0xFF5C6BC0)
val LightOnTertiary = Color(0xFF1A1A2E)  // Тёмный текст на карточке
val LightTertiaryContainer = Color(0xFFE8E0D8)  // WarmGray 100 - тёмнее фона
val LightOnTertiaryContainer = Color(0xFF1A237E)

// ============================================================
// LIGHT THEME BACKGROUND COLORS
// Можно менять эти цвета в одном месте для экспериментов
// ============================================================

/**
 * Мягкий теплый серый фон для светлой темы.
 * WarmGray палитра (из background_colors_brown):
 * - 0xFFF5F0EB (WarmGray 50) — очень светлый теплый (текущий фон)
 * - 0xFFE8E0D8 (WarmGray 100) — светлый тёплый
 * - 0xFFDDD3CA (WarmGray 150) — средний тёплый
 */
val LightBackgroundBase = Color(0xFFF5F0EB)  // WarmGray 50 - очень светлый теплый фон

/**
 * Цвет карточек - темнее фона для контраста.
 * WarmGray палитра (из background_colors_brown):
 * - 0xFFF5F0EB (WarmGray 50) — фон приложения
 * - 0xFFE8E0D8 (WarmGray 100) — карточки (текущий)
 * - 0xFFDDD3CA (WarmGray 150) — промежуточные элементы
 */
private val LightCardColor = Color(0xFFE8E0D8)  // WarmGray 100 - карточки темнее фона

// Background & Surface - используют базовый цвет для согласованности
val LightBackground = LightBackgroundBase  // Фон экранов под списками, bottomNav, тулбары
val LightOnBackground = Color(0xFF1C1917)  // Основной текст - тёмный (WarmGray 900)
val LightSurface = LightCardColor  // WarmGray 100 для карточек в списках
val LightOnSurface = Color(0xFF1C1917)  // Текст на surface - тёмный
val LightSurfaceVariant = Color(0xFFE8E0D8)  // WarmGray 100
val LightOnSurfaceVariant = Color(0xFF57534E)  // Вторичный текст (WarmGray 600)
val LightSurfaceDim = Color(0xFFDDD3CA)  // WarmGray 150
val LightSurfaceBright = LightCardColor  // WarmGray 100

// SurfaceContainer - система уровней для Material Design 3
// Используются для разных уровней "возвышения" элементов
// Иерархия: фон (WarmGray 50) < карточки (WarmGray 100) < промежуточные (WarmGray 150)
val LightSurfaceContainerLowest = LightCardColor  // WarmGray 100 - для карточек
val LightSurfaceContainerLow = LightCardColor     // WarmGray 100 - для Card (Material3)
val LightSurfaceContainer = Color(0xFFDDD3CA)     // WarmGray 150 - промежуточный уровень
val LightSurfaceContainerHigh = Color(0xFFDDD3CA) // WarmGray 150
val LightSurfaceContainerHighest = Color(0xFFC9B8A8) // Тёмнее - самый тёмный

// Outline
val LightOutline = Color(0xFF79747E)
val LightOutlineVariant = Color(0xFFE0E0E0)

// Error
val LightError = Color(0xFFB3261E)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFF9DEDC)
val LightOnErrorContainer = Color(0xFF410E0B)

// Inverse
val LightInverseSurface = Color(0xFF313033)
val LightInverseOnSurface = Color(0xFFF4EFF4)
val LightInversePrimary = Color(0xFFD0BCFF)

// ============================================================
// DARK THEME COLORS
// ============================================================

// Primary
val DarkPrimary = Color(0xFFD0BCFF)
val DarkOnPrimary = Color(0xFF381E72)
val DarkPrimaryContainer = Color(0xFF4F378B)
val DarkOnPrimaryContainer = Color(0xFFEADDFF)

// Secondary
val DarkSecondary = Color(0xFFB2ACB2)
val DarkOnSecondary = Color(0xFF1E1E1E)
val DarkSecondaryContainer = Color(0xFF4A4458)
val DarkOnSecondaryContainer = Color(0xFFE8DEF8)

// Tertiary - для карточек книг
val DarkTertiary = Color(0xFFB2ACB2)
val DarkOnTertiary = Color(0xFFE6E1E5)  // Светлый текст на карточке
val DarkTertiaryContainer = Color(0xFF2D2D3A)  // Тёмный фон карточки
val DarkOnTertiaryContainer = Color(0xFFE6E1E5)

// Background & Surface
val DarkBackground = Color(0xFF1C1B1F)
val DarkOnBackground = Color(0xFFE6E1E5)  // Основной текст - светлый
val DarkSurface = Color(0xFF1C1B1F)
val DarkOnSurface = Color(0xFFE6E1E5)  // Текст на surface - светлый
val DarkSurfaceVariant = Color(0xFF2D2D3A)
val DarkOnSurfaceVariant = Color(0xFFCAC4D0)
val DarkSurfaceDim = Color(0xFF141218)
val DarkSurfaceBright = Color(0xFF3B383E)
val DarkSurfaceContainerLowest = Color(0xFF0F0D13)
val DarkSurfaceContainerLow = Color(0xFF1D1B20)
val DarkSurfaceContainer = Color(0xFF211F26)
val DarkSurfaceContainerHigh = Color(0xFF2B2930)
val DarkSurfaceContainerHighest = Color(0xFF36343B)

// Outline
val DarkOutline = Color(0xFF938F99)
val DarkOutlineVariant = Color(0xFF49454F)

// Error (одинаково для обеих тем - функциональный цвет)
val DarkError = Color(0xFFB3261E)
val DarkOnError = Color(0xFFFFFFFF)
val DarkErrorContainer = Color(0xFF8C1D18)
val DarkOnErrorContainer = Color(0xFFF9DEDC)

// Inverse
val DarkInverseSurface = Color(0xFFE6E1E5)
val DarkInverseOnSurface = Color(0xFF313033)
val DarkInversePrimary = Color(0xFF6750A4)

// ============================================================
// FUNCTIONAL COLORS (одинаково для обеих тем)
// ============================================================

/** Успешные действия */
val FunctionalSuccess = Color(0xFF2E7D32)

/** Предупреждения */
val FunctionalWarning = Color(0xFFF57C00)

/** Информация */
val FunctionalInfo = Color(0xFF1976D2)

/** Удаление/опасные действия */
val FunctionalDanger = Color(0xFFB3261E)

/** Цвет избранного (красный для обеих тем) */
val FavoriteColor = Color(0xFFE53935)

// ============================================================
// LINK COLORS
// ============================================================

/** Цвет ссылок в тексте - Light */
val LightLinkColor = Color(0xFF1976D2)

/** Цвет ссылок в тексте - Dark */
val DarkLinkColor = Color(0xFF64B5F6)

/** Цвет ссылок в тексте (для совместимости) */
val LinkInTextColor = Color(0xFF1976D2)

// ============================================================
// LEGACY COLORS (для совместимости)
// ============================================================

@Deprecated("Use LightPrimary instead", ReplaceWith("LightPrimary"))
val Purple80 = Color(0xFFD0BCFF)

@Deprecated("Use DarkTertiary instead", ReplaceWith("DarkTertiary"))
val Pink80 = Color(0xFFEFB8C8)

@Deprecated("Use DarkSecondary instead", ReplaceWith("DarkSecondary"))
val Grey80 = Color(0xFFB2ACB2)

@Deprecated("Use DarkPrimaryContainer instead", ReplaceWith("DarkPrimaryContainer"))
val PurpleGrey40 = Color(0x66448AFF)

@Deprecated("Use LightPrimary instead", ReplaceWith("LightPrimary"))
val DarkBlue40 = Color(0xFF3F51B5)

@Deprecated("Use LightTertiary instead", ReplaceWith("LightTertiary"))
val Pink40 = Color(0xFF7D5260)

@Deprecated("Use LightSecondary instead", ReplaceWith("LightSecondary"))
val Grey40 = Color(0xFFCAC7DC)

@Deprecated("Use DarkPrimaryContainer instead", ReplaceWith("DarkPrimaryContainer"))
val PurpleGrey80 = Color(0xFF285191)

val White = Color(0xFFFFFFFF)

// ============================================================
// CARD COLORS
// ============================================================

/** Обводка карточки - Light */
val CardStrokeLight = Color(0x40000000)

/** Обводка карточки - Dark */
val CardStrokeDark = Color(0x40FFFFFF)

/** Обводка карточки новостей (для совместимости) */
val NewItemBorderStroke = Color(0x40000000)

// ============================================================
// SWIPE ACTION COLORS
// ============================================================

/** Фон при свайпе удаления */
val SwipeDeleteBackground = Color(0xFFB3261E)

/** Фон при свайпе "в избранное" */
val SwipeFavoriteBackground = Color(0xFFFFC107)
