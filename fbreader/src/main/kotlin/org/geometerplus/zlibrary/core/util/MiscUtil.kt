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

import java.util.LinkedList
import java.util.Locale

object MiscUtil {
    @JvmStatic
    fun isEmptyString(s: String?): Boolean = s == null || "" == s

    @JvmStatic
    fun <T> listsEquals(list1: List<T>?, list2: List<T>?): Boolean {
        if (list1 == null) {
            return list2 == null || list2.isEmpty()
        }
        if (list2 == null) {
            return list1.isEmpty()
        }
        if (list1.size != list2.size) {
            return false
        }
        return list1.containsAll(list2)
    }

    @JvmStatic
    fun <K, V> mapsEquals(map1: Map<K, V>?, map2: Map<K, V>?): Boolean {
        if (map1 == null) {
            return map2 == null || map2.isEmpty()
        }
        if (map2 == null) {
            return map1.isEmpty()
        }
        return map1 == map2
    }

    @JvmStatic
    fun matchesIgnoreCase(text: String, lowerCasePattern: String): Boolean =
        text.length >= lowerCasePattern.length &&
            text.lowercase(Locale.ROOT).indexOf(lowerCasePattern) >= 0

    @JvmStatic
    fun join(list: List<String>?, delimiter: String): String {
        if (list == null || list.isEmpty()) {
            return ""
        }
        return list.joinToString(delimiter)
    }

    @JvmStatic
    fun split(str: String?, delimiter: String): List<String> {
        if (str == null || "" == str) {
            return emptyList()
        }
        return str.split(delimiter).dropLastWhile { it.isEmpty() }
    }

    // splits str on any space symbols, keeps quoted substrings
    @JvmStatic
    fun smartSplit(str: String): List<String> {
        val tokens = LinkedList<String>()
        val regex = Regex("""([^"\s:;]+|".+?")""")
        for (m in regex.findAll(str)) {
            tokens.add(m.groupValues[1].replace("\"", ""))
        }
        return tokens
    }
}
