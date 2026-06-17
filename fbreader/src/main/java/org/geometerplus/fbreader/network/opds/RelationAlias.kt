/*
 * Copyright (C) 2010-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.fbreader.network.opds

class RelationAlias(
    // `alias` and `type` parameters must be either null or interned String.
    private val alias: String?,
    private val type: String?
) : Comparable<RelationAlias> {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RelationAlias) return false
        return alias == other.alias && type == other.type
    }

    override fun hashCode(): Int = (alias?.hashCode() ?: 0) + (type?.hashCode() ?: 0)

    override fun compareTo(other: RelationAlias): Int {
        if (alias != other.alias) {
            if (alias == null) return -1
            if (other.alias == null) return 1
            return alias.compareTo(other.alias)
        }
        if (type != other.type) {
            if (type == null) return -1
            if (other.type == null) return 1
            return type.compareTo(other.type)
        }
        return 0
    }

    override fun toString(): String = "Alias($alias; $type)"
}
