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

import org.geometerplus.zlibrary.core.filesystem.ZLResourceFile
import org.geometerplus.zlibrary.core.language.Language
import org.geometerplus.zlibrary.core.language.ZLLanguageUtil
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.TreeSet

internal class ZLTextTeXHyphenator : ZLTextHyphenator() {
    private val myPatternTable =
        HashMap<ZLTextTeXHyphenationPattern, ZLTextTeXHyphenationPattern>()
    private var myMaxPatternLength: Int = 0
    private var myLanguage: String? = null
    private var myLanguageCodes: List<String>? = null

    fun addPattern(pattern: ZLTextTeXHyphenationPattern) {
        myPatternTable[pattern] = pattern
        if (myMaxPatternLength < pattern.length()) {
            myMaxPatternLength = pattern.length()
        }
    }

    override fun languageCodes(): List<String> {
        var codes = myLanguageCodes
        if (codes == null) {
            val set = TreeSet<String>()
            val patternsFile = ZLResourceFile.createResourceFile("hyphenationPatterns")
            for (file in patternsFile.children()) {
                val name = file.getShortName()
                if (name.endsWith(".pattern")) {
                    set.add(name.substring(0, name.length - ".pattern".length))
                }
            }

            set.add("zh")
            codes = ArrayList(set)
            myLanguageCodes = codes
        }

        return Collections.unmodifiableList(codes)
    }

    override fun load(languageCode: String?) {
        var language = languageCode
        if (language == null || Language.OTHER_CODE == language) {
            language = ZLLanguageUtil.defaultLanguageCode()
        }
        if (language == null || language == myLanguage) {
            return
        }
        myLanguage = language
        unload()

        if (language != null) {
            ZLTextHyphenationReader(this).readQuietly(
                ZLResourceFile.createResourceFile("hyphenationPatterns/$language.pattern")
            )
        }
    }

    override fun unload() {
        myPatternTable.clear()
        myMaxPatternLength = 0
    }

    override fun hyphenate(stringToHyphenate: CharArray, mask: BooleanArray, length: Int) {
        if (myPatternTable.isEmpty()) {
            for (i in 0 until length - 1) {
                mask[i] = false
            }
            return
        }

        val values = ByteArray(length + 1)

        val pattern = ZLTextTeXHyphenationPattern(stringToHyphenate, 0, length, false)
        for (offset in 0 until length - 1) {
            var len = minOf(length - offset, myMaxPatternLength)
            pattern.update(stringToHyphenate, offset, len)
            while (len > 0) {
                pattern.reset(len)
                val toApply = myPatternTable[pattern]
                if (toApply != null) {
                    toApply.apply(values, offset)
                }
                --len
            }
        }

        for (i in 0 until length - 1) {
            mask[i] = values[i + 1] % 2 == 1
        }
    }
}
