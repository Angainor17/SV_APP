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

import org.fbreader.util.NaturalOrderComparator
import java.text.Normalizer

abstract class TitledEntity<T : TitledEntity<T>>(title: String?) : Comparable<T> {

    private var myTitle: String? = title
    private var mySortKey: String? = null

    fun getTitle(): String = myTitle ?: ""

    open fun setTitle(title: String?) {
        myTitle = title
        mySortKey = null
    }

    fun isTitleEmpty(): Boolean = myTitle == null || "" == myTitle

    protected fun resetSortKey() {
        mySortKey = null
    }

    abstract fun getLanguage(): String

    fun getSortKey(): String {
        var sortKey = mySortKey
        if (sortKey == null) {
            try {
                sortKey = trim(myTitle, getLanguage())
            } catch (t: Throwable) {
                sortKey = myTitle
            }
            mySortKey = sortKey
        }
        return sortKey ?: ""
    }

    override fun compareTo(other: T): Int =
        ourComparator.compare(getSortKey(), other.getSortKey())

    fun firstTitleLetter(): String? {
        val str = getSortKey()
        if (str.isEmpty()) {
            return null
        }
        return Character.toUpperCase(str[0]).toString()
    }

    private companion object {
        private val ourComparator = NaturalOrderComparator()

        private val ARTICLES = HashMap<String, Array<String>>()

        private val EN_ARTICLES = arrayOf("the ", "a ", "an ")
        private val FR_ARTICLES = arrayOf(
            "un ", "une ", "le ", "la ", "les ", "du ", "de ",
            "des ", "de la", "l ", "de l "
        )
        private val GE_ARTICLES = arrayOf(
            "das ", "des ", "dem ", "die ", "der ", "den ",
            "ein ", "eine ", "einer ", "einem ", "einen ", "eines "
        )
        private val IT_ARTICLES = arrayOf(
            "il ", "lo ", "la ", "l ", "un ", "uno ", "una ",
            "i ", "gli ", "le "
        )
        private val SP_ARTICLES = arrayOf(
            "el ", "la ", "los ", "las ", "un ", "unos ", "una ", "unas "
        )

        init {
            ARTICLES["en"] = EN_ARTICLES
            ARTICLES["fr"] = FR_ARTICLES
            ARTICLES["de"] = GE_ARTICLES
            ARTICLES["it"] = IT_ARTICLES
            ARTICLES["es"] = SP_ARTICLES
        }

        private fun trim(s: String?, language: String?): String {
            if (s == null) {
                return ""
            }

            val normalized = Normalizer.normalize(s, Normalizer.Form.NFKD)
            val buffer = StringBuilder()
            var start = 0
            if (normalized.startsWith("M'") || normalized.startsWith("Mc")) {
                buffer.append("Mac")
                start = 2
            }

            var afterSpace = false
            for (i in start until normalized.length) {
                var ch = normalized[i]
                // In case it is d' or l', may be it is "I'm", but it's OK.
                if (ch == '\'' || Character.isWhitespace(ch)) {
                    ch = ' '
                }

                when (Character.getType(ch).toByte()) {
                    Character.UPPERCASE_LETTER,
                    Character.TITLECASE_LETTER,
                    Character.OTHER_LETTER,
                    Character.MODIFIER_LETTER,
                    Character.LOWERCASE_LETTER,
                    Character.DECIMAL_DIGIT_NUMBER,
                    Character.LETTER_NUMBER,
                    Character.OTHER_NUMBER -> {
                        buffer.append(Character.toLowerCase(ch))
                        afterSpace = false
                    }
                    Character.SPACE_SEPARATOR -> {
                        if (!afterSpace && buffer.length > 0) {
                            buffer.append(' ')
                        }
                        afterSpace = true
                    }
                    else -> {
                        // we do ignore all other symbols
                    }
                }
            }

            val result = buffer.toString()
            if (result.startsWith("a is")) {
                return result
            }

            val articles = ARTICLES[language]
            if (articles != null) {
                for (a in articles) {
                    if (result.startsWith(a)) {
                        return result.substring(a.length)
                    }
                }
            }
            return result
        }
    }
}
