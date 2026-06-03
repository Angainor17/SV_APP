/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser.utils.history

/**
 * Слушатель [History].
 *
 * @author Hai Bison
 * @since v4.0 beta
 */
interface HistoryListener<A> {

    /**
     * Будет вызван после изменения истории.
     *
     * @param history [History]
     */
    fun onChanged(history: History<A>)
}
