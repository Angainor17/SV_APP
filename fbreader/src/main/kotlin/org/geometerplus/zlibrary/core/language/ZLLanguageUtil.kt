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

package org.geometerplus.zlibrary.core.language

import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.filesystem.ZLResourceFile
import java.util.ArrayList
import java.util.Collections
import java.util.Locale
import java.util.TreeSet

object ZLLanguageUtil {

    private val ourLanguageCodes = ArrayList<String>()

    @JvmStatic
    fun defaultLanguageCode(): String = Locale.getDefault().getLanguage()

    @JvmStatic
    fun languageCodes(): List<String> {
        if (ourLanguageCodes.isEmpty()) {
            val codes = TreeSet<String>()
            for (file in patternsFile().children()) {
                val name = file.getShortName()
                val index = name.indexOf("_")
                if (index != -1) {
                    val str = name.substring(0, index)
                    if (!codes.contains(str)) {
                        codes.add(str)
                    }
                }
            }
            codes.add("id")
            codes.add("de-traditional")

            ourLanguageCodes.addAll(codes)
        }

        return Collections.unmodifiableList(ourLanguageCodes)
    }

    @JvmStatic
    fun patternsFile(): ZLFile = ZLResourceFile.createResourceFile("languagePatterns")
}
