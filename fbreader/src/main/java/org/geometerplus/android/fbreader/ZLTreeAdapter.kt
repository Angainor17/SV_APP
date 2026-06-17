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

package org.geometerplus.android.fbreader

import android.view.ContextMenu
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import org.geometerplus.R
import org.geometerplus.zlibrary.core.tree.ZLTree

abstract class ZLTreeAdapter(
    private val myListView: ListView,
    private val root: ZLTree<*>
) : BaseAdapter(), AdapterView.OnItemClickListener, View.OnCreateContextMenuListener {

    private val myOpenItems = HashSet<ZLTree<*>>()
    private val myItems = arrayOfNulls<ZLTree<*>>(root.size - 1)

    init {
        myOpenItems.add(root)

        myListView.adapter = this
        myListView.onItemClickListener = this
        myListView.setOnCreateContextMenuListener(this)
    }

    protected fun openTree(tree: ZLTree<*>?) {
        if (tree == null) {
            return
        }
        var current: ZLTree<*> = tree
        while (!myOpenItems.contains(current)) {
            myOpenItems.add(current)
            val parent = current.parent ?: break
            current = parent
        }
    }

    fun expandOrCollapseTree(tree: ZLTree<*>) {
        if (!tree.hasChildren()) {
            return
        }
        if (isOpen(tree)) {
            myOpenItems.remove(tree)
        } else {
            myOpenItems.add(tree)
        }
        notifyDataSetChanged()
    }

    fun isOpen(tree: ZLTree<*>): Boolean = myOpenItems.contains(tree)

    fun selectItem(tree: ZLTree<*>?) {
        if (tree == null) {
            return
        }
        tree.parent?.let { openTree(it) }
        var index = 0
        var current: ZLTree<*> = tree
        while (true) {
            val parent = current.parent
            if (parent == null) {
                break
            }
            for (sibling in parent.subtrees()) {
                if (sibling === current) {
                    break
                }
                index += getCount(sibling)
            }
            current = parent
            ++index
        }
        if (index > 0) {
            myListView.setSelection(index - 1)
        }
        myListView.invalidateViews()
    }

    private fun getCount(tree: ZLTree<*>): Int {
        var count = 1
        if (isOpen(tree)) {
            for (subtree in tree.subtrees()) {
                count += getCount(subtree)
            }
        }
        return count
    }

    override fun getCount(): Int = getCount(root) - 1

    private fun indexByPosition(position: Int, tree: ZLTree<*>): Int {
        if (position == 0) {
            return 0
        }
        var pos = position - 1
        var index = 1
        for (subtree in tree.subtrees()) {
            val count = getCount(subtree)
            if (count <= pos) {
                pos -= count
                index += subtree.size
            } else {
                return index + indexByPosition(pos, subtree)
            }
        }
        throw RuntimeException("That's impossible!!!")
    }

    override fun getItem(position: Int): ZLTree<*> {
        val index = indexByPosition(position + 1, root) - 1
        var item = myItems[index]
        if (item == null) {
            item = root.getTreeByParagraphNumber(index + 1)
            myItems[index] = item
        }
        return item!!
    }

    override fun areAllItemsEnabled(): Boolean = true

    override fun isEnabled(position: Int): Boolean = true

    override fun getItemId(position: Int): Long = indexByPosition(position + 1, root).toLong()

    protected open fun runTreeItem(tree: ZLTree<*>): Boolean {
        if (!tree.hasChildren()) {
            return false
        }
        expandOrCollapseTree(tree)
        return true
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        runTreeItem(getItem(position))
    }

    override fun onCreateContextMenu(menu: ContextMenu, view: View, menuInfo: ContextMenu.ContextMenuInfo) {
    }

    abstract override fun getView(position: Int, convertView: View?, parent: ViewGroup): View

    protected fun setIcon(imageView: ImageView, tree: ZLTree<*>) {
        if (tree.hasChildren()) {
            if (isOpen(tree)) {
                imageView.setImageResource(R.drawable.ic_list_group_open)
            } else {
                imageView.setImageResource(R.drawable.ic_list_group_closed)
            }
        } else {
            imageView.setImageResource(R.drawable.ic_list_group_empty)
        }
        imageView.setPadding(25 * (tree.level - 1), imageView.paddingTop, 0, imageView.paddingBottom)
    }
}
