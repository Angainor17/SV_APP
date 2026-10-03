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

class Tag private constructor(@JvmField val Parent: Tag?, @JvmField val Name: String) {

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is Tag) {
            return false
        }
        return Parent === other.Parent && Name == other.Name
    }

    override fun hashCode(): Int =
        if (Parent == null) Name.hashCode() else Parent.hashCode() + Name.hashCode()

    fun toString(delimiter: String): String = toStringBuilder(delimiter).toString()

    protected fun toStringBuilder(delimiter: String): StringBuilder {
        val parent = Parent
        return if (parent == null) {
            StringBuilder(Name)
        } else {
            parent.toStringBuilder(delimiter).append(delimiter).append(Name)
        }
    }

    companion object {
        @JvmField
        val NULL: Tag = Tag(null, "")

        private val ourTagSet = HashMap<Tag, Tag>()

        @JvmStatic
        fun getTag(parent: Tag?, name: String?): Tag? {
            if (name == null) {
                return parent
            }
            val n = name.trim()
            if (n.isEmpty()) {
                return parent ?: NULL
            }
            val tag = Tag(parent, n)
            val stored = ourTagSet[tag]
            if (stored != null) {
                return stored
            }
            ourTagSet[tag] = tag
            return tag
        }

        @JvmStatic
        fun getTag(names: Array<String>): Tag? = getTag(names, names.size)

        private fun getTag(names: Array<String>, count: Int): Tag? =
            if (count == 0) null else getTag(getTag(names, count - 1), names[count - 1])
    }
}
