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

import org.geometerplus.zlibrary.text.view.ZLTextWord

abstract class ZLTextHyphenator protected constructor() {

    abstract fun languageCodes(): List<String>

    abstract fun load(languageCode: String?)

    abstract fun unload()

    fun getInfo(word: ZLTextWord): ZLTextHyphenationInfo {
        val len = word.Length
        val isLetter = BooleanArray(len)
        val pattern = CharArray(len + 2)
        val data = word.Data
        pattern[0] = ' '
        var j = word.Offset
        for (i in 0 until len) {
            val character = data[j]
            if (character == '\'' || character == '^' || character.isLetter()) {
                isLetter[i] = true
                pattern[i + 1] = character.lowercaseChar()
            } else {
                pattern[i + 1] = ' '
            }
            ++j
        }
        pattern[len + 1] = ' '

        val info = ZLTextHyphenationInfo(len + 2)
        val mask = info.Mask
        hyphenate(pattern, mask, len + 2)
        j = word.Offset - 1
        for (i in 0..len) {
            if (i < 2 || i > len - 2) {
                mask[i] = false
            } else {
                when (data[j]) {
                    0xAD.toChar() -> mask[i] = true // soft hyphen
                    '-' -> mask[i] = i >= 3 &&
                            isLetter[i - 3] &&
                            isLetter[i - 2] &&
                            isLetter[i] &&
                            isLetter[i + 1]
                    else -> mask[i] = mask[i] &&
                            isLetter[i - 2] &&
                            isLetter[i - 1] &&
                            isLetter[i] &&
                            isLetter[i + 1]
                }
            }
            ++j
        }

        return info
    }

    protected abstract fun hyphenate(stringToHyphenate: CharArray, mask: BooleanArray, length: Int)

    companion object {
        private var ourInstance: ZLTextHyphenator? = null

        @JvmStatic
        fun Instance(): ZLTextHyphenator {
            if (ourInstance == null) {
                ourInstance = ZLTextTeXHyphenator()
            }
            return ourInstance!!
        }

        @JvmStatic
        fun deleteInstance() {
            if (ourInstance != null) {
                ourInstance!!.unload()
                ourInstance = null
            }
        }
    }
}
