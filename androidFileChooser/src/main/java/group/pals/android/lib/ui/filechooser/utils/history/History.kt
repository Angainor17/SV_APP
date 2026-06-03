/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser.utils.history

import android.os.Parcelable

/**
 * Хранилище истории любого объекта.
 *
 * @param A любой тип
 * @author Hai Bison
 * @since v2.0 alpha
 */
interface History<A> : Parcelable {

    /**
     * Добавляет `newItem` в историю. Если верхний элемент такой же,
     * ничего не делает.
     *
     * @param newItem новый элемент.
     */
    fun push(newItem: A)

    /**
     * Находит `item` и, если он существует, удаляет все элементы после него.
     *
     * @param item [A]
     * @since v4.3 beta
     */
    fun truncateAfter(item: A)

    /**
     * Удаляет элемент.
     *
     * @param item [A]
     * @since v4.0 beta
     */
    fun remove(item: A)

    /**
     * Удаляет все элементы по фильтру.
     *
     * @param filter [HistoryFilter]
     * @since v4.0 beta
     */
    fun removeAll(filter: HistoryFilter<A>)

    /**
     * Получает размер истории.
     *
     * @return размер истории.
     */
    fun size(): Int

    /**
     * Получает индекс элемента `a`.
     *
     * @param a элемент.
     * @return индекс `a` или -1, если такого нет.
     */
    fun indexOf(a: A): Int

    /**
     * Получает предыдущий элемент `a`.
     *
     * @param a текущий элемент.
     * @return предыдущий элемент, может быть `null`.
     */
    fun prevOf(a: A): A?

    /**
     * Получает следующий элемент `a`.
     *
     * @param a текущий элемент.
     * @return следующий элемент, может быть `null`.
     */
    fun nextOf(a: A): A?

    /**
     * Получает все элементы этой истории в независимом списке.
     *
     * @return список [A].
     * @since v4.3 beta
     */
    fun items(): ArrayList<A>

    /**
     * Проверяет, пуста ли история.
     *
     * @return `true`, если история пуста, `false` в противном случае.
     * @since v4.3 beta
     */
    fun isEmpty(): Boolean

    /**
     * Очищает историю.
     *
     * @since v4.3 beta
     */
    fun clear()

    /**
     * Добавляет [HistoryListener].
     *
     * @param listener [HistoryListener]
     * @since v4.0 beta
     */
    fun addListener(listener: HistoryListener<A>)

    /**
     * Удаляет [HistoryListener].
     *
     * @param listener [HistoryListener]
     * @since v4.0 beta
     */
    fun removeListener(listener: HistoryListener<A>)

    /**
     * Уведомляет все [HistoryListener] о том, что история изменилась.
     */
    fun notifyHistoryChanged()
}
