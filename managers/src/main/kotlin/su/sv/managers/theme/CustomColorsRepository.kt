package su.sv.managers.theme

import kotlinx.coroutines.flow.Flow
import su.sv.commonui.theme.CustomThemeColors

/**
 * Репозиторий для хранения кастомных цветов темы.
 */
interface CustomColorsRepository {

    /**
     * Получить кастомные цвета для указанного режима темы.
     *
     * @param themeMode режим темы ("LIGHT" или "DARK")
     * @return Flow с кастомными цветами или null если нет кастомных цветов
     */
    fun getCustomColors(themeMode: String): Flow<CustomThemeColors?>

    /**
     * Сохранить кастомные цвета.
     *
     * @param colors кастомные цвета
     */
    suspend fun saveCustomColors(colors: CustomThemeColors)

    /**
     * Очистить кастомные цвета для указанного режима.
     *
     * @param themeMode режим темы ("LIGHT" или "DARK")
     */
    suspend fun clearCustomColors(themeMode: String)

    /**
     * Очистить все кастомные цвета.
     */
    suspend fun clearAll()
}