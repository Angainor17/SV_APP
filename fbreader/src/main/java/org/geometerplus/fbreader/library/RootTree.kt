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

import org.geometerplus.fbreader.book.IBookCollection
import org.geometerplus.fbreader.fbreader.options.SyncOptions
import org.geometerplus.fbreader.formats.PluginCollection

class RootTree(
    collection: IBookCollection<org.geometerplus.fbreader.book.Book>,
    pluginCollection: PluginCollection
) : LibraryTree(collection, pluginCollection) {

    init {
        FavoritesTree(this)
        RecentBooksTree(this)
        AuthorListTree(this)
        TitleListTree(this)
        SeriesListTree(this)
        TagListTree(this)
        if (SyncOptions().enabled.value) {
            SyncTree(this)
        }
        FileFirstLevelTree(this)
    }

    fun getLibraryTree(key: Key?): LibraryTree? {
        if (key == null) return null
        if (key.parent == null) {
            return if (key.id == getUniqueKey().id) this else null
        }
        val parentTree = getLibraryTree(key.parent) ?: return null
        return parentTree.getSubtree(key.id) as? LibraryTree
    }

    fun getSearchResultsTree(): SearchResultsTree? =
        getSubtree(ROOT_FOUND) as? SearchResultsTree

    fun createSearchResultsTree(pattern: String): SearchResultsTree {
        val children = subtrees()
        val position = if (children.isEmpty()) {
            0
        } else {
            if (children[0] is ExternalViewTree) 1 else 0
        }
        return SearchResultsTree(this, ROOT_FOUND, pattern, position)
    }

    override val name: String get() = resource().value

    override val summary: String get() = resource().value

    override val stringId: String get() = "@FBReaderLibraryRoot"
}
