/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.fbreader.book

import java.util.Collections
import java.util.LinkedList

abstract class AbstractBookCollection<B : AbstractBook> : IBookCollection<B> {
    private val myListeners: MutableList<IBookCollection.Listener<B>> =
        Collections.synchronizedList(LinkedList())

    override fun addListener(listener: IBookCollection.Listener<B>) {
        if (!myListeners.contains(listener)) {
            myListeners.add(listener)
        }
    }

    override fun removeListener(listener: IBookCollection.Listener<B>) {
        myListeners.remove(listener)
    }

    protected fun hasListeners(): Boolean = !myListeners.isEmpty()

    protected fun fireBookEvent(event: BookEvent, book: B) {
        synchronized(myListeners) {
            for (listener in myListeners) {
                listener.onBookEvent(event, book)
            }
        }
    }

    protected fun fireBuildEvent(status: IBookCollection.Status) {
        synchronized(myListeners) {
            for (listener in myListeners) {
                listener.onBuildEvent(status)
            }
        }
    }

    override fun sameBook(b0: B?, b1: B?): Boolean {
        if (b0 === b1) {
            return true
        }
        if (b0 == null || b1 == null) {
            return false
        }
        if (b0.getPath() == b1.getPath()) {
            return true
        }
        val hash0 = getHash(b0, false)
        return hash0 != null && hash0 == getHash(b1, false)
    }
}
