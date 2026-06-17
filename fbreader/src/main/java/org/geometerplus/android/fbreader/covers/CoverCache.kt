/*
 * Copyright (C) 2010-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.android.fbreader.covers

import android.graphics.Bitmap
import org.geometerplus.fbreader.tree.FBTree
import java.util.Collections

internal class CoverCache {

    @Volatile
    var holdersCounter = 0

    private val myBitmaps = Collections.synchronizedMap(
        object : LinkedHashMap<FBTree.Key, Any>(10, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<FBTree.Key, Any>?): Boolean {
                return size > 3 * holdersCounter
            }
        }
    )

    @Throws(NullObjectException::class)
    fun getBitmap(key: FBTree.Key): Bitmap? {
        val bitmap = myBitmaps[key]
        if (bitmap === NULL_BITMAP) {
            throw NullObjectException()
        }
        return bitmap as Bitmap?
    }

    fun putBitmap(key: FBTree.Key, bitmap: Bitmap?) {
        myBitmaps[key] = bitmap ?: NULL_BITMAP
    }

    class NullObjectException : Exception()

    companion object {
        private val NULL_BITMAP = Any()
    }
}
