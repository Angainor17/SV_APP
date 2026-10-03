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

package org.geometerplus.zlibrary.core.options

class StringPair(group: String, name: String) {

    @JvmField
    val Group: String = group.intern()

    @JvmField
    val Name: String = name.intern()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is StringPair) {
            return false
        }
        // yes, I'm sure Group & Name are not nulls
        return Group == other.Group && Name == other.Name
    }

    override fun hashCode(): Int = Group.hashCode() + 37 * Name.hashCode()
}
