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

class ZLMutableString(
    @JvmField var myData: CharArray,
    @JvmField var myLength: Int,
) {
    constructor() : this(CharArray(20), 0)

    constructor(len: Int) : this(CharArray(len), 0)

    constructor(container: ZLMutableString) : this(
        ZLArrayUtils.createCopy(container.myData, container.myLength, container.myLength),
        container.myLength,
    )

    fun append(buffer: CharArray, offset: Int, count: Int) {
        val len = myLength
        var data = myData
        val newLength = len + count
        if (data.size < newLength) {
            data = ZLArrayUtils.createCopy(data, len, newLength)
            myData = data
        }
        System.arraycopy(buffer, offset, data, len, count)
        myLength = newLength
    }

    fun clear() {
        myLength = 0
    }

    override fun equals(other: Any?): Boolean {
        val container = other as ZLMutableString
        val len = myLength
        if (len != container.myLength) {
            return false
        }
        val data0 = myData
        val data1 = container.myData
        for (i in len - 1 downTo 0) {
            if (data0[i] != data1[i]) {
                return false
            }
        }
        return true
    }

    override fun hashCode(): Int {
        val len = myLength
        val data = myData
        var code = len * 31
        if (len > 1) {
            code += data[0].code
            code *= 31
            code += data[1].code
            if (len > 2) {
                code *= 31
                code += data[2].code
            }
        } else if (len > 0) {
            code += data[0].code
        }
        return code
    }

    override fun toString(): String =
        String(myData, 0, myLength).intern()
}
