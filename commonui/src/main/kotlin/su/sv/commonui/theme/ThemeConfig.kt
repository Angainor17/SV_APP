package su.sv.commonui.theme

import kotlinx.coroutines.flow.Flow

/**
 * Режим темы приложения
 */
enum class ThemeMode {
    /** Светлая тема */
    LIGHT,

    /** Тёмная тема */
    DARK,

    /** Системная тема (следует за настройками устройства) */
    SYSTEM;

    /**
     * Проверяет, является ли тема тёмной
     * @param isSystemDark true если системная тема тёмная (из isSystemInDarkTheme())
     */
    fun isDarkTheme(isSystemDark: Boolean = false): Boolean = when (this) {
        DARK -> true
        LIGHT -> false
        SYSTEM -> isSystemDark
    }

    /**
     * Следующий режим темы при переключении
     * Переключает на противоположную отображаемую тему:
     * - Если текущая отображаемая тема тёмная -> LIGHT
     * - Если текущая отображаемая тема светлая -> DARK
     *
     * @param currentIsDark текущее отображаемое состояние (тёмная или светлая тема показывается)
     */
    fun next(currentIsDark: Boolean): ThemeMode = when {
        currentIsDark -> LIGHT  // Если показывается тёмная -> переключить на светлую
        else -> DARK            // Если показывается светлая -> переключить на тёмную
    }
}

/**
 * Конфигурация темы приложения
 */
data class ThemeConfig(
    /** Режим темы */
    val themeMode: ThemeMode = ThemeMode.SYSTEM,

    /** Использовать динамические цвета (Material You, Android 12+) */
    val useDynamicColors: Boolean = false,
) {
    companion object {
        /** Конфигурация по умолчанию */
        val Default = ThemeConfig()
    }
}

/**
 * Интерфейс для хранения настроек темы
 * Реализуется в модуле managers или commonui
 */
interface ThemeRepository {
    /**
     * Flow с текущим режимом темы
     */
    val themeMode: Flow<ThemeMode>

    /**
     * Flow с настройкой динамических цветов
     */
    val useDynamicColors: Flow<Boolean>

    /**
     * Flow с полной конфигурацией темы
     */
    val themeConfig: Flow<ThemeConfig>

    /**
     * Установить режим темы
     */
    suspend fun setThemeMode(mode: ThemeMode)

    /**
     * Установить использование динамических цветов
     */
    suspend fun setUseDynamicColors(use: Boolean)
}
