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

package org.geometerplus.android.fbreader.tree

import android.widget.BaseAdapter
import org.geometerplus.fbreader.tree.FBTree
import java.util.Collections

abstract class TreeAdapter(protected val activity: TreeActivity<*>) : BaseAdapter() {

    private val myItems = Collections.synchronizedList(mutableListOf<FBTree>())

    init {
        activity.listAdapter = this
    }

    fun remove(item: FBTree) {
        activity.runOnUiThread {
            myItems.remove(item)
            notifyDataSetChanged()
        }
    }

    fun add(item: FBTree) {
        activity.runOnUiThread {
            myItems.add(item)
            notifyDataSetChanged()
        }
    }

    fun add(index: Int, item: FBTree) {
        activity.runOnUiThread {
            myItems.add(index, item)
            notifyDataSetChanged()
        }
    }

    fun replaceAll(items: Collection<FBTree>, invalidateViews: Boolean) {
        activity.runOnUiThread {
            synchronized(myItems) {
                myItems.clear()
                myItems.addAll(items)
            }
            notifyDataSetChanged()
            if (invalidateViews) {
                activity.listView.invalidateViews()
            }
        }
    }

    override fun getCount(): Int = myItems.size

    override fun getItem(position: Int): FBTree = myItems[position]

    override fun getItemId(position: Int): Long = position.toLong()

    fun getIndex(item: FBTree?): Int = myItems.indexOf(item)

    val firstSelectedItem: FBTree?
        get() {
            synchronized(myItems) {
                for (t in myItems) {
                    if (activity.isTreeSelected(t)) {
                        return t
                    }
                }
            }
            return null
        }
}
