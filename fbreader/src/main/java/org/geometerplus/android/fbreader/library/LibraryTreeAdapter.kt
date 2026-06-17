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

package org.geometerplus.android.fbreader.library

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.geometerplus.R
import org.geometerplus.android.fbreader.covers.CoverManager
import org.geometerplus.android.fbreader.tree.TreeAdapter
import org.geometerplus.android.util.ViewUtil
import org.geometerplus.fbreader.book.AbstractBook
import org.geometerplus.fbreader.library.AuthorListTree
import org.geometerplus.fbreader.library.AuthorTree
import org.geometerplus.fbreader.library.ExternalViewTree
import org.geometerplus.fbreader.library.FavoritesTree
import org.geometerplus.fbreader.library.FileFirstLevelTree
import org.geometerplus.fbreader.library.FileTree
import org.geometerplus.fbreader.library.LibraryTree
import org.geometerplus.fbreader.library.RecentBooksTree
import org.geometerplus.fbreader.library.SearchResultsTree
import org.geometerplus.fbreader.library.SyncTree
import org.geometerplus.fbreader.library.TagListTree
import org.geometerplus.fbreader.library.TagTree
import org.geometerplus.fbreader.library.TitleListTree

internal class LibraryTreeAdapter(activity: LibraryActivity) : TreeAdapter(activity) {
    private var myCoverManager: CoverManager? = null

    private fun createView(convertView: View?, parent: ViewGroup, tree: LibraryTree): View {
        val view = convertView
            ?: LayoutInflater.from(parent.context).inflate(R.layout.library_tree_item, parent, false)

        val unread = tree.getBook() != null && !tree.getBook()!!.hasLabel(AbstractBook.READ_LABEL)

        val nameView = ViewUtil.findTextView(view, R.id.library_tree_item_name)
        nameView.text = if (unread) {
            Html.fromHtml("<b>${tree.name}")
        } else {
            tree.name
        }

        val summaryView = ViewUtil.findTextView(view, R.id.library_tree_item_childrenlist)
        summaryView.text = if (unread) {
            Html.fromHtml("<b>${tree.summary}")
        } else {
            tree.summary
        }

        return view
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val tree = getItem(position) as LibraryTree
        val view = createView(convertView, parent, tree)
        if (activity.isTreeSelected(tree)) {
            view.setBackgroundColor(0xff555555.toInt())
        } else {
            view.setBackgroundColor(0)
        }

        if (myCoverManager == null) {
            view.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            val coverHeight = view.measuredHeight
            val treeActivity = activity
            myCoverManager = CoverManager(treeActivity, treeActivity.imageSynchronizer, coverHeight * 15 / 32, coverHeight)
            view.requestLayout()
        }

        val coverView = ViewUtil.findImageView(view, R.id.library_tree_item_icon)
        if (!myCoverManager!!.trySetCoverImage(coverView, tree)) {
            coverView.setImageResource(getCoverResourceId(tree))
        }

        return view
    }

    private fun getCoverResourceId(tree: LibraryTree): Int {
        if (tree.getBook() != null) {
            return R.drawable.ic_list_library_book
        }
        return when (tree) {
            is ExternalViewTree -> R.drawable.plugin_bookshelf
            is FavoritesTree -> R.drawable.ic_list_library_favorites
            is RecentBooksTree, is SyncTree -> R.drawable.ic_list_library_recent
            is AuthorListTree -> R.drawable.ic_list_library_authors
            is TitleListTree -> R.drawable.ic_list_library_books
            is TagListTree -> R.drawable.ic_list_library_tags
            is FileFirstLevelTree -> R.drawable.ic_list_library_folder
            is SearchResultsTree -> R.drawable.ic_list_library_search
            is FileTree -> {
                val file = tree.file
                when {
                    file.isArchive -> R.drawable.ic_list_library_zip
                    file.isDirectory && file.isReadable -> R.drawable.ic_list_library_folder
                    else -> R.drawable.ic_list_library_permission_denied
                }
            }
            is AuthorTree -> R.drawable.ic_list_library_author
            is TagTree -> R.drawable.ic_list_library_tag
            else -> R.drawable.ic_list_library_books
        }
    }
}
