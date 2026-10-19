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

package org.geometerplus.zlibrary.core.xml

import org.geometerplus.zlibrary.core.util.ZLArrayUtils

// optimized partially implemented map String -> String
// key must be interned
// there is no remove() in this implementation
// put with the same key does not remove old entry

class ZLStringMap {
    private var myKeys: Array<String?> = arrayOfNulls(8)
    private var myValues: Array<String?> = arrayOfNulls(8)
    private var mySize: Int = 0

    fun put(key: String, value: String) {
        val size = mySize++
        var keys = myKeys
        if (keys.size == size) {
            keys = ZLArrayUtils.createCopy(keys, size, size shl 1)
            myKeys = keys
            myValues = ZLArrayUtils.createCopy(myValues, size, size shl 1)
        }
        keys[size] = key
        myValues[size] = value
    }

    /*
     * Parameter `key` must be an interned string.
     */
    fun getValue(key: String): String? {
        var index = mySize
        if (index > 0) {
            val keys = myKeys
            while (--index >= 0) {
                if (keys[index] === key) {
                    return myValues[index]
                }
            }
        }
        return null
    }

    fun getSize(): Int = mySize

    fun getKey(index: Int): String? = myKeys[index]

    fun getValue(index: Int): String? = myValues[index]

    fun clear() {
        mySize = 0
    }
}
