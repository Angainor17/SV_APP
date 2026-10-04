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

package org.geometerplus.zlibrary.text.hyphenation

class ZLTextTeXHyphenationPattern(
    pattern: CharArray,
    offset: Int,
    length: Int,
    useValues: Boolean,
) {
    private val mySymbols: CharArray
    private val myValues: ByteArray?
    private var myLength: Int
    private var myHashCode: Int = 0

    init {
        if (useValues) {
            var patternLength = 0
            for (i in 0 until length) {
                val symbol = pattern[offset + i]
                if (symbol > '9' || symbol < '0') {
                    ++patternLength
                }
            }
            val symbols = CharArray(patternLength)
            val values = ByteArray(patternLength + 1)

            var k = 0
            for (i in 0 until length) {
                val sym = pattern[offset + i]
                if (sym <= '9' && sym >= '0') {
                    values[k] = (sym - '0').toByte()
                } else {
                    symbols[k] = sym
                    ++k
                }
            }

            myLength = patternLength
            mySymbols = symbols
            myValues = values
        } else {
            val symbols = CharArray(length)
            System.arraycopy(pattern, offset, symbols, 0, length)
            myLength = length
            mySymbols = symbols
            myValues = null
        }
    }

    internal fun update(pattern: CharArray, offset: Int, length: Int) {
        // We assert
        //      1. this pattern doesn't use values
        //      length <= original pattern length
        System.arraycopy(pattern, offset, mySymbols, 0, length)
        myLength = length
        myHashCode = 0
    }

    internal fun apply(mask: ByteArray, position: Int) {
        val patternLength = myLength
        val values = myValues!!
        var j = position
        for (i in 0..patternLength) {
            val value = values[i]
            if (mask[j] < value) {
                mask[j] = value
            }
            ++j
        }
    }

    internal fun reset(length: Int) {
        myLength = length
        myHashCode = 0
    }

    internal fun length(): Int = myLength

    override fun equals(other: Any?): Boolean {
        val pattern = other as ZLTextTeXHyphenationPattern
        val len = myLength
        if (len != pattern.myLength) {
            return false
        }
        val symbols0 = mySymbols
        val symbols1 = pattern.mySymbols
        var index = len
        while (index > 0) {
            index--
            if (symbols0[index] != symbols1[index]) {
                return false
            }
        }
        return true
    }

    override fun hashCode(): Int {
        var hash = myHashCode
        if (hash == 0) {
            val symbols = mySymbols
            var index = myLength
            while (index > 0) {
                index--
                hash *= 31
                hash += symbols[index].code
            }
            myHashCode = hash
        }
        return hash
    }

    override fun toString(): String {
        val buffer = StringBuilder()
        val values = myValues
        for (i in 0 until myLength) {
            if (values != null) {
                buffer.append(values[i].toInt())
            }
            buffer.append(mySymbols[i])
        }
        if (values != null) {
            buffer.append(values[myLength].toInt())
        }
        return buffer.toString()
    }
}
