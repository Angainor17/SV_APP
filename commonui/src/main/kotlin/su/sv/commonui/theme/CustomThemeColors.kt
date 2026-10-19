package su.sv.commonui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Кастомные цвета темы для отладочного редактора.
 *
 * @param themeMode режим темы ("LIGHT" или "DARK")
 * @param colors карта цветов: attributeName -> ARGB значение (Int as Long)
 */
data class CustomThemeColors(
    val themeMode: String,  // "LIGHT" или "DARK"
    val colors: Map<String, Long> = emptyMap(),  // attributeName -> ARGB (Int as Long)
) {
    /**
     * Получить цвет по имени атрибута
     */
    fun getColor(attributeName: String): Color? {
        return colors[attributeName]?.let { Color(it.toInt()) }
    }

    /**
     * Установить цвет для атрибута
     */
    fun setColor(attributeName: String, color: Color): CustomThemeColors {
        // Конвертируем Color в ARGB Int, потом в Long для хранения
        val argb = color.toArgb().toLong()
        return copy(
            colors = colors + (attributeName to argb)
        )
    }

    /**
     * Удалить цвет для атрибута (вернуть к исходному)
     */
    fun removeColor(attributeName: String): CustomThemeColors {
        return copy(
            colors = colors - attributeName
        )
    }

    /**
     * Проверить, есть ли кастомный цвет для атрибута
     */
    fun hasCustomColor(attributeName: String): Boolean {
        return colors.containsKey(attributeName)
    }

    /**
     * Очистить все кастомные цвета
     */
    fun clearAll(): CustomThemeColors {
        return copy(colors = emptyMap())
    }

    companion object {
        /**
         * Пустой набор цветов для светлой темы
         */
        fun emptyLight() = CustomThemeColors(themeMode = "LIGHT")

        /**
         * Пустой набор цветов для тёмной темы
         */
        fun emptyDark() = CustomThemeColors(themeMode = "DARK")
    }
}

/**
 * Расширение для конвертации Color в Long для хранения
 */
fun Color.toArgbLong(): Long = toArgb().toLong()

/**
 * Расширение для конвертации Long в Color
 */
fun Long.toColor(): Color = Color(this.toInt())