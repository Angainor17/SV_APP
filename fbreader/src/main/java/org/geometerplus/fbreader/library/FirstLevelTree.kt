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

package org.geometerplus.fbreader.library

import org.fbreader.util.Pair
import org.geometerplus.zlibrary.core.resources.ZLResource

abstract class FirstLevelTree : LibraryTree {

    private val id: String
    private val resource: ZLResource

    internal constructor(root: RootTree, position: Int, id: String) : super(root, position) {
        this.id = id
        this.resource = resource().getResource(id)
    }

    internal constructor(root: RootTree, id: String) : super(root) {
        this.id = id
        this.resource = resource().getResource(id)
    }

    override val name: String get() = resource.value

    override val treeTitle: Pair<String, String?> get() = Pair(summary, null)

    override val summary: String get() = resource.getResource("summary").value

    override val stringId: String get() = id

    override fun isSelectable(): Boolean = false
}
