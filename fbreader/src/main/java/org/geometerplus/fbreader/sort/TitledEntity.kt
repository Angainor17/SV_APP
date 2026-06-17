/*
 * Copyright (C) 2009-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.fbreader.sort

import android.annotation.TargetApi
import android.os.Build
import org.fbreader.util.NaturalOrderComparator
import java.text.Normalizer

abstract class TitledEntity<T : TitledEntity<T>>(internal open var title: String?) : Comparable<T> {

    private var sortKey: String? = null

    companion object {
        private val comparator = NaturalOrderComparator()
        private val articles: Map<String, Array<String>> = mapOf(
            "en" to arrayOf("the ", "a ", "an "),
            "fr" to arrayOf("un ", "une ", "le ", "la ", "les ", "du ", "de ", "des ", "de la", "l ", "de l "),
            "de" to arrayOf("das ", "des ", "dem ", "die ", "der ", "den ", "ein ", "eine ", "einer ", "einem ", "einen ", "eines "),
            "it" to arrayOf("il ", "lo ", "la ", "l ", "un ", "uno ", "una ", "i ", "gli ", "le "),
            "es" to arrayOf("el ", "la ", "los ", "las ", "un ", "unos ", "una ", "unas ")
        )

        private fun trim(s: String?, language: String): String {
            if (s == null) {
                return ""
            }

            var str = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                normalize(s)
            } else {
                s
            }

            val buffer = StringBuilder()
            var start = 0
            if (str.startsWith("M'") || str.startsWith("Mc")) {
                buffer.append("Mac")
                start = 2
            }

            var afterSpace = false
            for (i in start until str.length) {
                var ch = str[i]
                // In case it is d' or l', may be it is "I'm", but it's OK.
                if (ch == '\'' || Character.isWhitespace(ch)) {
                    ch = ' '
                }

                when (Character.getType(ch).toInt()) {
                    Character.UPPERCASE_LETTER.toInt(),
                    Character.TITLECASE_LETTER.toInt(),
                    Character.OTHER_LETTER.toInt(),
                    Character.MODIFIER_LETTER.toInt(),
                    Character.LOWERCASE_LETTER.toInt(),
                    Character.DECIMAL_DIGIT_NUMBER.toInt(),
                    Character.LETTER_NUMBER.toInt(),
                    Character.OTHER_NUMBER.toInt() -> {
                        buffer.append(Character.toLowerCase(ch))
                        afterSpace = false
                    }
                    Character.SPACE_SEPARATOR.toInt() -> {
                        if (!afterSpace && buffer.isNotEmpty()) {
                            buffer.append(' ')
                        }
                        afterSpace = true
                    }
                    // we do ignore all other symbols
                }
            }

            val result = buffer.toString()
            if (result.startsWith("a is")) {
                return result
            }

            articles[language]?.let { articleList ->
                for (a in articleList) {
                    if (result.startsWith(a)) {
                        return result.substring(a.length)
                    }
                }
            }
            return result
        }

        @TargetApi(Build.VERSION_CODES.GINGERBREAD)
        private fun normalize(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFKD)
    }

    fun getTitle(): String = title ?: ""

    fun setTitle(title: String?) {
        this.title = title
        sortKey = null
    }

    val isTitleEmpty: Boolean
        get() = title == null || title!!.isEmpty()

    protected fun resetSortKey() {
        sortKey = null
    }

    abstract val language: String?

    fun getSortKey(): String {
        if (sortKey == null) {
            sortKey = try {
                trim(title, language ?: "")
            } catch (t: Throwable) {
                title ?: ""
            }
        }
        return sortKey!!
    }

    override fun compareTo(other: T): Int = comparator.compare(getSortKey(), other.getSortKey())

    fun firstTitleLetter(): String? {
        val str = getSortKey()
        if (str.isEmpty()) {
            return null
        }
        return str[0].uppercaseChar().toString()
    }
}
