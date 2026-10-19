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

package org.geometerplus.fbreader.book

import java.util.Locale

class Author(@JvmField val DisplayName: String, sortKey: String) : Comparable<Author> {

    @JvmField
    val SortKey: String = sortKey.lowercase(Locale.ROOT)

    override fun compareTo(other: Author): Int {
        val byKeys = SortKey.compareTo(other.SortKey)
        return if (byKeys != 0) byKeys else DisplayName.compareTo(other.DisplayName)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is Author) {
            return false
        }
        return SortKey == other.SortKey && DisplayName == other.DisplayName
    }

    override fun hashCode(): Int = SortKey.hashCode() + DisplayName.hashCode()

    override fun toString(): String = "$DisplayName ($SortKey)"

    companion object {
        @JvmField
        val NULL: Author = Author("", "")

        @JvmStatic
        fun create(name: String?, sortKey: String?): Author? {
            if (name == null) {
                return null
            }
            var strippedName = name.trim()
            if (strippedName.isEmpty()) {
                return null
            }

            var strippedKey = sortKey?.trim() ?: ""
            if (strippedKey.isEmpty()) {
                var index = strippedName.lastIndexOf(' ')
                if (index == -1) {
                    strippedKey = strippedName
                } else {
                    strippedKey = strippedName.substring(index + 1)
                    while (index >= 0 && strippedName[index] == ' ') {
                        --index
                    }
                    strippedName = strippedName.substring(0, index + 1) + ' ' + strippedKey
                }
            }

            return Author(strippedName, strippedKey)
        }

        @JvmStatic
        fun hashCode(author: Author?): Int = author?.hashCode() ?: 0
    }
}
