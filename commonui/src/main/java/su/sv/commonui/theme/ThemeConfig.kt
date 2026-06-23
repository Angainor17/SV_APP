package su.sv.commonui.theme

import kotlinx.coroutines.flow.Flow

/**
 * Режим темы приложения
 */
enum class ThemeMode {
    /** Светлая тема */
    LIGHT,

    /** Тёмная тема */
    DARK;

    /**
     * Проверяет, является ли тема тёмной
     */
    fun isDarkTheme(): Boolean = this == DARK

    /**
     * Следующий режим темы при переключении
     */
    fun next(): ThemeMode {
        return when (this) {
            LIGHT -> DARK
            DARK -> LIGHT
        }
    }
}

/**
 * Конфигурация темы приложения
 */
data class ThemeConfig(
    /** Режим темы */
    val themeMode: ThemeMode = ThemeMode.LIGHT,

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
