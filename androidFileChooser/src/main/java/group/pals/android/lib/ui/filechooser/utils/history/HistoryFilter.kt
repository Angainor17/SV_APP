/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser.utils.history

/**
 * Фильтр [History].
 *
 * @author Hai Bison
 * @since v4.0 beta
 */
interface HistoryFilter<A> {

    /**
     * Фильтрует элемент.
     *
     * @param item [A]
     * @return `true`, если элемент принят.
     */
    fun accept(item: A): Boolean
}
