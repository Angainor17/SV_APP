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

import android.app.ListActivity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Window
import org.fbreader.util.Pair
import org.geometerplus.android.fbreader.util.AndroidImageSynchronizer
import org.geometerplus.android.util.OrientationUtil
import org.geometerplus.android.util.UIMessageUtil
import org.geometerplus.android.util.UIUtil
import org.geometerplus.fbreader.tree.FBTree
import org.geometerplus.zlibrary.ui.android.library.UncaughtExceptionHandler
import java.util.Collections

abstract class TreeActivity<T : FBTree> : ListActivity() {

    private val myHistory = Collections.synchronizedList(mutableListOf<FBTree.Key>())
    private var myCurrentTree: T? = null
    // we store the key separately because
    // it will be changed in case of myCurrentTree.removeSelf() call
    private var myCurrentKey: FBTree.Key? = null

    val imageSynchronizer = AndroidImageSynchronizer(this)

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        Thread.setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler(this))
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS)
    }

    override fun onStart() {
        super.onStart()
        OrientationUtil.setOrientation(this, intent)
    }

    override fun onDestroy() {
        imageSynchronizer.clear()
        super.onDestroy()
    }

    override fun getListAdapter(): TreeAdapter = super.getListAdapter() as TreeAdapter

    protected fun getCurrentTree(): T? = myCurrentTree

    override fun onNewIntent(intent: Intent) {
        OrientationUtil.setOrientation(this, intent)
        if (OPEN_TREE_ACTION == intent.action) {
            runOnUiThread {
                init(intent)
            }
        } else {
            super.onNewIntent(intent)
        }
    }

    protected abstract fun getTreeByKey(key: FBTree.Key): T?

    abstract fun isTreeSelected(tree: FBTree): Boolean

    protected open fun isTreeInvisible(tree: FBTree): Boolean = false

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            var parent: FBTree? = null
            synchronized(myHistory) {
                while (parent == null && myHistory.isNotEmpty()) {
                    parent = getTreeByKey(myHistory.removeAt(myHistory.size - 1))
                }
            }
            if (parent == null && myCurrentTree != null) {
                parent = myCurrentTree!!.parent
            }
            if (parent != null && !isTreeInvisible(parent)) {
                openTree(parent, myCurrentTree, false)
                return true
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    fun openTree(tree: FBTree) {
        openTree(tree, null, true)
    }

    fun clearHistory() {
        runOnUiThread {
            myHistory.clear()
        }
    }

    protected open fun onCurrentTreeChanged() {}

    private fun openTree(tree: FBTree, treeToSelect: FBTree?, storeInHistory: Boolean) {
        when (tree.openingStatus) {
            FBTree.Status.WAIT_FOR_OPEN,
            FBTree.Status.ALWAYS_RELOAD_BEFORE_OPENING -> {
                val messageKey = tree.openingStatusMessage
                if (messageKey != null) {
                    UIUtil.createExecutor(this, messageKey).execute(
                        { tree.waitForOpening() },
                        { openTreeInternal(tree, treeToSelect, storeInHistory) }
                    )
                } else {
                    tree.waitForOpening()
                    openTreeInternal(tree, treeToSelect, storeInHistory)
                }
            }
            else -> openTreeInternal(tree, treeToSelect, storeInHistory)
        }
    }

    private fun setTitleAndSubtitle(pair: Pair<String, String?>) {
        if (pair.Second != null) {
            title = pair.First + " - " + pair.Second
        } else {
            title = pair.First
        }
    }

    protected open fun init(intent: Intent) {
        val key = intent.getSerializableExtra(TREE_KEY_KEY) as FBTree.Key?
        val selectedKey = intent.getSerializableExtra(SELECTED_TREE_KEY_KEY) as FBTree.Key?
        myCurrentTree = if (key != null) getTreeByKey(key) else null
        // not myCurrentKey = key
        // because key might be null
        myCurrentKey = myCurrentTree?.getUniqueKey()
        val adapter = listAdapter
        adapter.replaceAll(myCurrentTree!!.subtrees(), false)
        setTitleAndSubtitle(myCurrentTree!!.treeTitle)
        val selectedTree = if (selectedKey != null) getTreeByKey(selectedKey) else adapter.firstSelectedItem
        val index = adapter.getIndex(selectedTree)
        if (index != -1) {
            setSelection(index)
            listView.post {
                setSelection(index)
            }
        }

        myHistory.clear()
        @Suppress("UNCHECKED_CAST")
        val history = intent.getSerializableExtra(HISTORY_KEY) as? ArrayList<FBTree.Key>
        if (history != null) {
            myHistory.addAll(history)
        }
        onCurrentTreeChanged()
    }

    private fun openTreeInternal(tree: FBTree, treeToSelect: FBTree?, storeInHistory: Boolean) {
        when (tree.openingStatus) {
            FBTree.Status.READY_TO_OPEN,
            FBTree.Status.ALWAYS_RELOAD_BEFORE_OPENING -> {
                if (storeInHistory && myCurrentKey != tree.getUniqueKey()) {
                    myHistory.add(myCurrentKey!!)
                }
                onNewIntent(Intent(this, javaClass)
                    .setAction(OPEN_TREE_ACTION)
                    .putExtra(TREE_KEY_KEY, tree.getUniqueKey())
                    .putExtra(
                        SELECTED_TREE_KEY_KEY,
                        if (treeToSelect != null) treeToSelect.getUniqueKey() else null
                    )
                    .putExtra(HISTORY_KEY, ArrayList(myHistory))
                )
            }
            FBTree.Status.CANNOT_OPEN -> {
                tree.openingStatusMessage?.let { UIMessageUtil.showErrorMessage(this, it) }
            }
            else -> {}
        }
    }

    companion object {
        const val TREE_KEY_KEY = "TreeKey"
        const val SELECTED_TREE_KEY_KEY = "SelectedTreeKey"
        const val HISTORY_KEY = "HistoryKey"
        private const val OPEN_TREE_ACTION = "android.fbreader.action.OPEN_TREE"
    }
}
