package org.geometerplus.zlibrary.core.util

import java.util.Arrays
import java.util.LinkedList
import java.util.regex.Pattern

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
    fun <KeyT, ValueT> mapsEquals(map1: Map<KeyT, ValueT>?, map2: Map<KeyT, ValueT>?): Boolean {
        if (map1 == null) {
            return map2 == null || map2.isEmpty()
        }
        if (map2 == null) {
            return map1.isEmpty()
        }
        return map1 == map2
    }

    @JvmStatic
    fun matchesIgnoreCase(text: String, lowerCasePattern: String): Boolean {
        return text.length >= lowerCasePattern.length &&
                text.lowercase().indexOf(lowerCasePattern) >= 0
    }

    @JvmStatic
    fun join(list: List<String>?, delimiter: String): String {
        if (list == null || list.isEmpty()) {
            return ""
        }
        val builder = StringBuilder()
        var first = true
        for (s in list) {
            if (first) {
                first = false
            } else {
                builder.append(delimiter)
            }
            builder.append(s)
        }
        return builder.toString()
    }

    @JvmStatic
    fun split(str: String?, delimiter: String): List<String> {
        if (str == null || "" == str) {
            return emptyList()
        }
        return Arrays.asList(*str.split(delimiter.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
    }

    // splits str on any space symbols, keeps quoted substrings
    @JvmStatic
    fun smartSplit(str: String): List<String> {
        val tokens = LinkedList<String>()
        val m = Pattern.compile("([^\"\\s:;]+|\".+?\")").matcher(str)
        while (m.find()) {
            tokens.add(m.group(1).replace("\"", ""))
        }
        return tokens
    }
}
