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

package org.geometerplus.fbreader.book

class Tag private constructor(val parent: Tag?, @JvmField val Name: String) {

    companion object {
        @JvmField
        val NULL = Tag(null, "")

        private val tagSet = mutableMapOf<Tag, Tag>()

        @JvmStatic
        fun getTag(parent: Tag?, name: String?): Tag {
            if (name == null) {
                return parent ?: NULL
            }
            val trimmedName = name.trim()
            if (trimmedName.isEmpty()) {
                return parent ?: NULL
            }
            val tag = Tag(parent, trimmedName)
            return tagSet.getOrPut(tag) { tag }
        }

        @JvmStatic
        fun getTag(names: Array<String>): Tag {
            var result: Tag? = null
            for (name in names) {
                result = getTag(result, name)
            }
            return result ?: NULL
        }
    }

    fun toString(delimiter: String): String = toStringBuilder(delimiter).toString()

    protected fun toStringBuilder(delimiter: String): StringBuilder {
        return if (parent == null) {
            StringBuilder(Name)
        } else {
            parent.toStringBuilder(delimiter).append(delimiter).append(Name)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Tag) return false
        return parent == other.parent && Name == other.Name
    }

    override fun hashCode(): Int = if (parent == null) Name.hashCode() else parent.hashCode() + Name.hashCode()
}
