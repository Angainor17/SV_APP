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
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

private const val IGNORABLE_WHITESPACE = 0
private const val LANGLE = 1
private const val TAG_PREFIX = 2
private const val COMMENT = 3
private const val END_OF_COMMENT1 = 4
private const val END_OF_COMMENT2 = 5
private const val ENTITY = 6
private const val WAIT_NAME = 7
private const val NAME = 8
private const val WAIT_VALUE = 9
private const val VALUE = 10
private const val WAIT_END_OF_ENTITY = 11

internal class ZLDTDParser {
    @Throws(IOException::class)
    fun doIt(stream: InputStream, entityMap: HashMap<String, CharArray>) {
        val streamReader = InputStreamReader(stream, Charsets.US_ASCII)

        var buffer = CharArray(8192)
        var startPosition = 0
        var name = ""
        var value = ""

        var state = IGNORABLE_WHITESPACE
        errorLabel@ while (true) {
            val count = streamReader.read(buffer)
            if (count <= 0) {
                streamReader.close()
                return
            }
            if (count < buffer.size) {
                buffer = ZLArrayUtils.createCopy(buffer, count, count)
            }
            try {
                var i = -1
                stateLoop@ while (true) {
                    when (state) {
                        IGNORABLE_WHITESPACE ->
                            while (true) {
                                when (buffer[++i]) {
                                    '<' -> {
                                        state = LANGLE
                                        continue@stateLoop
                                    }
                                }
                            }
                        LANGLE ->
                            when (buffer[++i]) {
                                '!' -> {
                                    state = TAG_PREFIX
                                    continue@stateLoop
                                }
                                else -> break@errorLabel
                            }
                        TAG_PREFIX ->
                            when (buffer[++i]) {
                                'E' -> {
                                    state = ENTITY
                                    continue@stateLoop
                                }
                                '-' -> {
                                    state = COMMENT
                                    continue@stateLoop
                                }
                                else -> break@errorLabel
                            }
                        ENTITY ->
                            while (true) {
                                when (buffer[++i]) {
                                    ' ', '\b', '\t', '\n', '\u000B', '\u000C', '\r' -> {
                                        state = WAIT_NAME
                                        continue@stateLoop
                                    }
                                }
                            }
                        WAIT_NAME ->
                            while (true) {
                                when (buffer[++i]) {
                                    ' ', '\b', '\t', '\n', '\u000B', '\u000C', '\r' -> {}
                                    else -> {
                                        state = NAME
                                        startPosition = i
                                        continue@stateLoop
                                    }
                                }
                            }
                        NAME ->
                            while (true) {
                                when (buffer[++i]) {
                                    ' ', '\b', '\t', '\n', '\u000B', '\u000C', '\r' -> {
                                        state = WAIT_VALUE
                                        name += String(buffer, startPosition, i - startPosition)
                                        continue@stateLoop
                                    }
                                }
                            }
                        WAIT_VALUE ->
                            while (true) {
                                when (buffer[++i]) {
                                    ' ', '\b', '\t', '\n', '\u000B', '\u000C', '\r' -> {}
                                    else -> {
                                        state = VALUE
                                        startPosition = i
                                        continue@stateLoop
                                    }
                                }
                            }
                        VALUE ->
                            while (true) {
                                when (buffer[++i]) {
                                    '>' -> {
                                        state = IGNORABLE_WHITESPACE
                                        value += String(buffer, startPosition, i - startPosition)
                                        val len = value.length
                                        if ((len > 2) &&
                                            (value[0] == '"') &&
                                            (value[len - 1] == '"')
                                        ) {
                                            value = value.substring(1, len - 1)
                                            if (value.startsWith("&#") && value.endsWith(";")) {
                                                try {
                                                    var number = 0
                                                    if (value[2] == 'x') {
                                                        number = value.substring(3, len - 3).toInt(16)
                                                    } else {
                                                        for (j in 2 until len - 3) {
                                                            number *= 10
                                                            number += value[j].code - 48
                                                        }
                                                    }
                                                    entityMap[name] = charArrayOf(number.toChar())
                                                } catch (e: NumberFormatException) {
                                                }
                                            } else {
                                                val aValue = CharArray(len - 2)
                                                value.toCharArray(aValue, 0, 0, len - 2)
                                                entityMap[name] = aValue
                                            }
                                        }
                                        name = ""
                                        value = ""
                                        continue@stateLoop
                                    }
                                    ' ', '\b', '\t', '\n', '\u000B', '\u000C', '\r' -> {
                                        state = WAIT_END_OF_ENTITY
                                        value += String(buffer, startPosition, i - startPosition)
                                        name = ""
                                        value = ""
                                        continue@stateLoop
                                    }
                                }
                            }
                        WAIT_END_OF_ENTITY ->
                            while (true) {
                                when (buffer[++i]) {
                                    '>' -> {
                                        state = IGNORABLE_WHITESPACE
                                        continue@stateLoop
                                    }
                                }
                            }
                        COMMENT ->
                            while (true) {
                                when (buffer[++i]) {
                                    '-' -> {
                                        state = END_OF_COMMENT1
                                        continue@stateLoop
                                    }
                                }
                            }
                        END_OF_COMMENT1 ->
                            when (buffer[++i]) {
                                '-' -> {
                                    state = END_OF_COMMENT2
                                    continue@stateLoop
                                }
                                else -> {
                                    state = COMMENT
                                    continue@stateLoop
                                }
                            }
                        END_OF_COMMENT2 ->
                            when (buffer[++i]) {
                                '>' -> {
                                    state = IGNORABLE_WHITESPACE
                                    continue@stateLoop
                                }
                                else -> {
                                    state = COMMENT
                                    continue@stateLoop
                                }
                            }
                    }
                }
            } catch (e: ArrayIndexOutOfBoundsException) {
                when (state) {
                    NAME -> {
                        name = String(buffer, startPosition, count - startPosition)
                        startPosition = 0
                    }
                    VALUE -> {
                        value = String(buffer, startPosition, count - startPosition)
                        startPosition = 0
                    }
                }
            }
        }
        streamReader.close()
    }
}
