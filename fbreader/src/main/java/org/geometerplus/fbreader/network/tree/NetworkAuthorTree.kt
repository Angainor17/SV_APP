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

package org.geometerplus.fbreader.network.tree

import org.geometerplus.fbreader.network.NetworkBookItem
import org.geometerplus.fbreader.network.NetworkTree
import java.util.LinkedList

class NetworkAuthorTree internal constructor(
    parent: NetworkTree,
    val author: NetworkBookItem.AuthorData
) : NetworkTree(parent) {

    private var myBooksNumber = 0
    private var mySeriesMap: HashMap<String, Int>? = null

    override val name: String
        get() = author.displayName

    override val sortKey: String
        get() = author.sortKey

    private fun getSeriesIndex(seriesName: String): Int {
        val map = mySeriesMap ?: return -1
        val value = map[seriesName] ?: return -1
        return value.toInt()
    }

    private fun setSeriesIndex(seriesName: String, index: Int) {
        if (mySeriesMap == null) {
            mySeriesMap = HashMap()
        }
        mySeriesMap!![seriesName] = Integer.valueOf(index)
    }

    fun updateSubtrees(books: LinkedList<NetworkBookItem>) {
        if (myBooksNumber >= books.size) {
            return
        }

        val booksIterator = books.listIterator(myBooksNumber)
        while (booksIterator.hasNext()) {
            val book = booksIterator.next()

            if (book.seriesTitle != null) {
                val seriesPosition = getSeriesIndex(book.seriesTitle!!)
                if (seriesPosition == -1) {
                    val insertAt = subtrees().size
                    setSeriesIndex(book.seriesTitle!!, insertAt)
                    NetworkBookTree(this, book, false)
                } else {
                    var treeAtSeriesPosition = subtrees()[seriesPosition]
                    if (treeAtSeriesPosition is NetworkBookTree) {
                        val bookTree = treeAtSeriesPosition
                        bookTree.removeSelf()
                        val seriesTree = NetworkSeriesTree(this, book.seriesTitle!!, seriesPosition, false)
                        NetworkBookTree(seriesTree, bookTree.book, false)
                        treeAtSeriesPosition = seriesTree
                    }

                    if (treeAtSeriesPosition !is NetworkSeriesTree) {
                        throw RuntimeException("That's impossible!!!")
                    }
                    val seriesTree = treeAtSeriesPosition
                    val nodesIterator = seriesTree.subtrees().listIterator()
                    var insertAt = 0
                    while (nodesIterator.hasNext()) {
                        val tree = nodesIterator.next()
                        if (tree !is NetworkBookTree) {
                            throw RuntimeException("That's impossible!!!")
                        }
                        val bookTree = tree
                        if (bookTree.book.indexInSeries > book.indexInSeries) {
                            break
                        }
                        ++insertAt
                    }
                    NetworkBookTree(seriesTree, book, insertAt, false)
                }
            } else {
                NetworkBookTree(this, book, false)
            }
        }

        myBooksNumber = books.size
    }

    override val stringId: String
        get() = "@Author:" + author.displayName + ":" + author.sortKey
}
