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

package org.geometerplus.android.fbreader.api

import java.io.Serializable

abstract class MenuNode(val code: String) : Cloneable, Serializable {

    var optionalTitle: String? = null

    public abstract override fun clone(): MenuNode

    class Item(code: String, val iconId: Int?) : MenuNode(code) {
        constructor(code: String) : this(code, null)

        override fun clone(): Item = Item(code, iconId)

        companion object {
            private const val serialVersionUID: Long = 43L
        }
    }

    class Submenu(code: String) : MenuNode(code) {
        val children: ArrayList<MenuNode> = ArrayList()

        override fun clone(): Submenu {
            val copy = Submenu(code)
            for (node in children) {
                copy.children.add(node.clone())
            }
            return copy
        }

        companion object {
            private const val serialVersionUID: Long = 44L
        }
    }

    companion object {
        private const val serialVersionUID: Long = 42L
    }
}
