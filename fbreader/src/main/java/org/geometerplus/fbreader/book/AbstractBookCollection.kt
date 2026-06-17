package org.geometerplus.fbreader.book

import java.util.Collections
import java.util.LinkedList

abstract class AbstractBookCollection<B : AbstractBook> : IBookCollection<B> {

    private val listeners: MutableList<IBookCollection.Listener<B>> =
        Collections.synchronizedList(LinkedList())

    override fun addListener(listener: IBookCollection.Listener<B>) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    override fun removeListener(listener: IBookCollection.Listener<B>) {
        listeners.remove(listener)
    }

    protected fun hasListeners(): Boolean = listeners.isNotEmpty()

    protected fun fireBookEvent(event: BookEvent, book: B?) {
        synchronized(listeners) {
            for (l in listeners) {
                l.onBookEvent(event, book)
            }
        }
    }

    protected fun fireBuildEvent(status: IBookCollection.Status) {
        synchronized(listeners) {
            for (l in listeners) {
                l.onBuildEvent(status)
            }
        }
    }

    override fun sameBook(book0: B, book1: B): Boolean {
        if (book0 === book1) return true

        if (book0.getPath() == book1.getPath()) return true

        val hash0 = getHash(book0, false)
        return hash0 != null && hash0 == getHash(book1, false)
    }
}
