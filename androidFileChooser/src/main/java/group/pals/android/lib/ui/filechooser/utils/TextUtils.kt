/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser.utils

/**
 * Текстовые утилиты.
 *
 * @author Hai Bison
 * @since v4.3 beta
 */
object TextUtils {

    /**
     * Заключает текст в двойные кавычки.
     *
     * @param s текст, если `null`, будет использована пустая строка.
     * @return текст в кавычках.
     */
    fun quote(s: String?): String = "\"${s ?: ""}\""
}
