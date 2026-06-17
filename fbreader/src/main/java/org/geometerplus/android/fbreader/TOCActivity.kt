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

import android.app.ListActivity
import android.content.Intent
import android.os.Bundle
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import org.geometerplus.R
import org.geometerplus.android.util.OrientationUtil
import org.geometerplus.android.util.ViewUtil
import org.geometerplus.fbreader.bookmodel.TOCTree
import org.geometerplus.fbreader.fbreader.FBReaderApp
import org.geometerplus.zlibrary.core.application.ZLApplication
import org.geometerplus.zlibrary.core.resources.ZLResource
import org.geometerplus.zlibrary.core.tree.ZLTree
import org.geometerplus.zlibrary.ui.android.library.UncaughtExceptionHandler

class TOCActivity : ListActivity() {
    private lateinit var myAdapter: TOCAdapter
    private var mySelectedItem: ZLTree<*>? = null

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)

        Thread.setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler(this))

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val fbreader = ZLApplication.Instance() as FBReaderApp
        val model = fbreader.model
        if (model == null) {
            finish()
            return
        }
        val root = model.tocTree
        myAdapter = TOCAdapter(root)
        val treeToSelect = fbreader.currentTOCElement
        myAdapter.selectItem(treeToSelect)
        mySelectedItem = treeToSelect
    }

    override fun onStart() {
        super.onStart()
        OrientationUtil.setOrientation(this, intent)
    }

    override fun onNewIntent(intent: Intent) {
        OrientationUtil.setOrientation(this, intent)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val position = (item.menuInfo as AdapterView.AdapterContextMenuInfo).position
        val tree = myAdapter.getItem(position) as TOCTree
        when (item.itemId) {
            PROCESS_TREE_ITEM_ID -> {
                myAdapter.executeTreeItem(tree)
                return true
            }
            READ_BOOK_ITEM_ID -> {
                myAdapter.openBookText(tree)
                return true
            }
        }
        return super.onContextItemSelected(item)
    }

    private inner class TOCAdapter(root: TOCTree) : ZLTreeAdapter(listView, root) {

        fun executeTreeItem(tree: ZLTree<*>) {
            runTreeItem(tree)
        }

        override fun onCreateContextMenu(menu: ContextMenu, view: View, menuInfo: ContextMenu.ContextMenuInfo) {
            val position = (menuInfo as AdapterView.AdapterContextMenuInfo).position
            val tree = getItem(position) as TOCTree
            if (tree.hasChildren()) {
                menu.setHeaderTitle(tree.getText())
                val resource = ZLResource.resource("tocView")
                menu.add(0, PROCESS_TREE_ITEM_ID, 0, resource.getResource(if (isOpen(tree)) "collapseTree" else "expandTree").value ?: "")
                menu.add(0, READ_BOOK_ITEM_ID, 0, resource.getResource("readText").value ?: "")
            }
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView
                ?: LayoutInflater.from(parent.context).inflate(R.layout.toc_tree_item, parent, false)
            val tree = getItem(position) as TOCTree
            view.setBackgroundColor(if (tree == mySelectedItem) -0x7f7f80 else 0)
            setIcon(ViewUtil.findImageView(view, R.id.toc_tree_item_icon), tree)
            ViewUtil.findTextView(view, R.id.toc_tree_item_text).text = tree.getText()
            return view
        }

        fun openBookText(tree: TOCTree) {
            val reference = tree.reference
            if (reference != null) {
                finish()
                val fbreader = ZLApplication.Instance() as FBReaderApp
                fbreader.addInvisibleBookmark()
                fbreader.bookTextView.gotoPosition(reference.paragraphIndex, 0, 0)
                fbreader.showBookTextView()
                fbreader.storePosition()
            }
        }

        override fun runTreeItem(tree: ZLTree<*>): Boolean {
            if (super.runTreeItem(tree)) {
                return true
            }
            openBookText(tree as TOCTree)
            return true
        }
    }

    companion object {
        private const val PROCESS_TREE_ITEM_ID = 0
        private const val READ_BOOK_ITEM_ID = 1
    }
}
