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
import org.geometerplus.fbreader.book.AbstractBook
import org.geometerplus.fbreader.book.Filter

class SyncTree internal constructor(root: RootTree) : FirstLevelTree(root, ROOT_SYNC) {

    private val labels = listOf(
        AbstractBook.SYNCHRONISED_LABEL,
        AbstractBook.SYNC_FAILURE_LABEL,
        AbstractBook.SYNC_DELETED_LABEL
    )

    override val treeTitle: Pair<String, String?> get() = Pair(name, null)

    override val openingStatus: Status get() = Status.ALWAYS_RELOAD_BEFORE_OPENING

    override fun waitForOpening() {
        clear()

        val baseResource = resource().getResource(ROOT_SYNC)
        var others: Filter = Filter.HasPhysicalFile()

        for (label in labels) {
            val filter = Filter.ByLabel(label)
            if (collection.hasBooks(filter)) {
                SyncLabelTree(this, label, filter, baseResource.getResource(label))
            }
            others = Filter.And(others, Filter.Not(filter))
        }
        if (collection.hasBooks(others)) {
            SyncLabelTree(
                this,
                AbstractBook.SYNC_TOSYNC_LABEL,
                others,
                baseResource.getResource(AbstractBook.SYNC_TOSYNC_LABEL)
            )
        }
    }
}
