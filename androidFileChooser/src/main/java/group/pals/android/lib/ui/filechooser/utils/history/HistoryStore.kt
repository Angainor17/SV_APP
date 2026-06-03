/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    См. файл LICENSE в корневой директории этого проекта для
 *    получения разрешения на копирование.
 */

package group.pals.android.lib.ui.filechooser.utils.history

import android.os.Parcel
import android.os.Parcelable

/**
 * Хранилище истории любого объекта, расширяющего [Parcelable].
 *
 * **Примечание:** Этот класс не поддерживает сохранение своих [HistoryListener]
 * в [Parcelable]. Вы должны заново создать все слушатели после получения
 * [HistoryStore] из [android.os.Bundle], например.
 *
 * @param A тип объекта, должен реализовывать [Parcelable]
 * @author Hai Bison
 * @since v2.0 alpha
 */
open class HistoryStore<A : Parcelable> : History<A>, Parcelable {

    companion object CREATOR : Parcelable.Creator<HistoryStore<Parcelable>> {
        override fun createFromParcel(parcel: Parcel): HistoryStore<Parcelable> {
            return HistoryStore(parcel)
        }

        override fun newArray(size: Int): Array<HistoryStore<Parcelable>?> {
            return arrayOfNulls(size)
        }
    }

    private val historyList = ArrayList<A>()
    private val maxSize: Int
    private val listeners = ArrayList<HistoryListener<A>>()

    /**
     * Создаёт новый [HistoryStore].
     *
     * @param maxSize максимальный размер, если <= 0, будет использовано 100.
     */
    constructor(maxSize: Int) {
        this.maxSize = if (maxSize > 0) maxSize else 100
    }

    @Suppress("UNCHECKED_CAST")
    private constructor(parcel: Parcel) {
        maxSize = parcel.readInt()
        val count = parcel.readInt()
        for (i in 0 until count) {
            val item = parcel.readParcelable<Parcelable>(null) as? A
            item?.let { historyList.add(it) }
        }
    }

    override fun push(newItem: A) {
        if (newItem == null) return

        if (historyList.isNotEmpty() && historyList.indexOf(newItem) == historyList.size - 1) return

        historyList.add(newItem)
        notifyHistoryChanged()
    }

    override fun truncateAfter(item: A) {
        if (item == null) return

        val idx = historyList.indexOf(item)
        if (idx >= 0 && idx < historyList.size - 1) {
            historyList.subList(idx + 1, historyList.size).clear()
            notifyHistoryChanged()
        }
    }

    override fun remove(item: A) {
        if (historyList.remove(item)) {
            notifyHistoryChanged()
        }
    }

    override fun removeAll(filter: HistoryFilter<A>) {
        var changed = false
        for (i in historyList.size - 1 downTo 0) {
            if (filter.accept(historyList[i])) {
                historyList.removeAt(i)
                if (!changed) changed = true
            }
        }

        if (changed) notifyHistoryChanged()
    }

    override fun notifyHistoryChanged() {
        for (listener in listeners) {
            listener.onChanged(this)
        }
    }

    override fun size(): Int = historyList.size

    override fun indexOf(a: A): Int = historyList.indexOf(a)

    override fun prevOf(a: A): A? {
        val idx = historyList.indexOf(a)
        return if (idx > 0) historyList[idx - 1] else null
    }

    override fun nextOf(a: A): A? {
        val idx = historyList.indexOf(a)
        return if (idx >= 0 && idx < historyList.size - 1) historyList[idx + 1] else null
    }

    override fun items(): ArrayList<A> = historyList.clone() as ArrayList<A>

    override fun isEmpty(): Boolean = historyList.isEmpty()

    override fun clear() {
        historyList.clear()
        notifyHistoryChanged()
    }

    override fun addListener(listener: HistoryListener<A>) {
        listeners.add(listener)
    }

    override fun removeListener(listener: HistoryListener<A>) {
        listeners.remove(listener)
    }

    // Parcelable

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(maxSize)
        dest.writeInt(size())
        for (i in 0 until size()) {
            dest.writeParcelable(historyList[i], flags)
        }
    }
}
