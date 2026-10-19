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

package org.geometerplus.zlibrary.core.fonts

import org.fbreader.util.ComparisonUtil
import java.util.HashMap

class FontEntry private constructor(
    @JvmField val Family: String,
    private val myFileInfos: Array<FileInfo?>?
) {

    constructor(
        family: String,
        normal: FileInfo?,
        bold: FileInfo?,
        italic: FileInfo?,
        boldItalic: FileInfo?
    ) : this(family, arrayOf(normal, bold, italic, boldItalic))

    private constructor(family: String) : this(family, null)

    fun isSystem(): Boolean = myFileInfos == null

    fun fileInfo(bold: Boolean, italic: Boolean): FileInfo? =
        myFileInfos?.get((if (bold) 1 else 0) + (if (italic) 2 else 0))

    override fun toString(): String {
        val builder = StringBuilder("FontEntry[")
        builder.append(Family)
        if (myFileInfos != null) {
            for (info in myFileInfos) {
                builder.append(';').append(info?.Path ?: "null")
            }
        }
        return builder.append(']').toString()
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other !is FontEntry) {
            return false
        }
        if (!ComparisonUtil.equal(Family, other.Family)) {
            return false
        }
        if (myFileInfos == null) {
            return other.myFileInfos == null
        }
        if (other.myFileInfos == null) {
            return false
        }
        for (i in myFileInfos.indices) {
            if (!ComparisonUtil.equal(myFileInfos[i], other.myFileInfos[i])) {
                return false
            }
        }
        return true
    }

    override fun hashCode(): Int = ComparisonUtil.hashCode(Family)

    companion object {
        private val ourSystemEntries = HashMap<String, FontEntry>()

        @JvmStatic
        fun systemEntry(family: String): FontEntry = synchronized(ourSystemEntries) {
            val existing = ourSystemEntries[family]
            if (existing != null) {
                existing
            } else {
                val entry = FontEntry(family)
                ourSystemEntries[family] = entry
                entry
            }
        }
    }
}
