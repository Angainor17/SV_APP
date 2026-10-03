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

package org.geometerplus.zlibrary.core.util

object ZLSearchUtil {

    @JvmStatic
    fun find(text: CharArray, offset: Int, length: Int, pattern: ZLSearchPattern): Result? =
        find(text, offset, length, pattern, 0)

    @JvmStatic
    fun find(
        text: CharArray,
        offset: Int,
        length: Int,
        pattern: ZLSearchPattern,
        pos: Int
    ): Result? {
        var pos = pos
        if (pos < 0) {
            pos = 0
        }
        val lower = pattern.lowerCasePattern
        val patternLength = lower.size
        val end = offset + length
        val lastStart = end - patternLength
        if (pattern.ignoreCase) {
            val upper = pattern.upperCasePattern!!
            val firstCharLower = lower[0]
            val firstCharUpper = upper[0]
            var i = offset + pos
            while (i <= lastStart) {
                val current = text[i]
                if (current == firstCharLower || current == firstCharUpper) {
                    var j = 1
                    var k = i + 1
                    while (j < patternLength) {
                        val symbol = text[k]
                        if (symbol == '​') {
                            if (patternLength - j > end - k) {
                                break
                            } else {
                                ++k
                                continue
                            }
                        }
                        if (lower[j] != symbol && upper[j] != symbol) {
                            break
                        }
                        ++j
                        ++k
                    }
                    if (j == patternLength) {
                        return Result(i - offset, k - i)
                    }
                }
                ++i
            }
        } else {
            val firstChar = lower[0]
            var i = offset + pos
            while (i <= lastStart) {
                if (text[i] == firstChar) {
                    var j = 1
                    var k = i + 1
                    while (j < patternLength) {
                        val symbol = text[k]
                        if (symbol == '​') {
                            if (patternLength - j > end - k) {
                                break
                            } else {
                                ++k
                                continue
                            }
                        }
                        if (lower[j] != text[k]) {
                            break
                        }
                        ++j
                        ++k
                    }
                    if (j >= patternLength) {
                        return Result(i - offset, k - i)
                    }
                }
                ++i
            }
        }
        return null
    }

    class Result(@JvmField val Start: Int, @JvmField val Length: Int)
}
