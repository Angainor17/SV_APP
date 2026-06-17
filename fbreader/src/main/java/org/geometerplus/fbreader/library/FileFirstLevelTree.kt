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
import org.geometerplus.fbreader.Paths
import org.geometerplus.zlibrary.core.filesystem.ZLFile

class FileFirstLevelTree internal constructor(root: RootTree) : FirstLevelTree(root, ROOT_FILE) {

    override val treeTitle: Pair<String, String?> get() = Pair(name, null)

    override val openingStatus: Status get() = Status.ALWAYS_RELOAD_BEFORE_OPENING

    override fun waitForOpening() {
        clear()
        for (dir in Paths.bookPathOption.value) {
            addChild(dir, resource().getResource("fileTreeLibrary").value, dir)
        }
        addChild("/", "fileTreeRoot")
        val cards = Paths.allCardDirectories()
        if (cards.size == 1) {
            addChild(cards[0], "fileTreeCard")
        } else {
            val res = resource().getResource("fileTreeCard")
            val title = res.getResource("withIndex").value
            val summary = res.getResource("summary").value
            var index = 0
            for (dir in cards) {
                addChild(dir, title.replace("%s", (++index).toString()), summary)
            }
        }
    }

    private fun addChild(path: String, title: String, summary: String) {
        val file = ZLFile.createFileByPath(path)
        if (file != null) {
            FileTree(this, file, title, summary)
        }
    }

    private fun addChild(path: String, resourceKey: String) {
        val res = resource().getResource(resourceKey)
        addChild(path, res.value, res.getResource("summary").value)
    }
}
