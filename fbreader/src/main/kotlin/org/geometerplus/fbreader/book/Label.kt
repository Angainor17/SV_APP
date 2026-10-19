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

import java.util.UUID

class Label(@JvmField val Uid: String, @JvmField val Name: String) {

    constructor(name: String) : this(UUID.randomUUID().toString(), name)

    override fun toString(): String = "$Name[$Uid]"

    override fun equals(other: Any?): Boolean {
        if (other !is Label) {
            return false
        }
        return Name == other.Name
    }

    override fun hashCode(): Int = Name.hashCode()
}
