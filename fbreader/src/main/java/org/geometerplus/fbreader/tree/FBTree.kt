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

package org.geometerplus.fbreader.tree

import org.fbreader.util.ComparisonUtil
import org.fbreader.util.Pair
import org.geometerplus.zlibrary.core.image.ZLImage
import org.geometerplus.zlibrary.core.tree.ZLTree
import java.io.Serializable

abstract class FBTree : ZLTree<FBTree>, Comparable<FBTree> {
    private var myCover: ZLImage? = null
    private var coverRequested = false
    private var key: Key? = null

    protected constructor() : super()

    protected constructor(parent: FBTree) : super(parent)

    protected constructor(parent: FBTree, position: Int) : super(parent, position)

    companion object {
        private fun compareStringsIgnoreCase(s0: String, s1: String): Int {
            val len = minOf(s0.length, s1.length)
            for (i in 0 until len) {
                var c0 = s0[i]
                var c1 = s1[i]
                if (c0 == c1) continue
                c0 = c0.lowercaseChar()
                c1 = c1.lowercaseChar()
                if (c0 == c1) continue
                return c0.code - c1.code
            }
            return when {
                s0.length > len -> 1
                s1.length > len -> -1
                else -> 0
            }
        }
    }

    fun getUniqueKey(): Key {
        if (key == null) {
            key = Key(if (parent != null) parent.getUniqueKey() else null, stringId)
        }
        return key!!
    }

    /**
     * Returns id used as a part of unique key above. This string must be not null
     * and be different for all children of same tree
     */
    protected abstract val stringId: String

    fun getSubtree(id: String): FBTree? {
        for (tree in subtrees()) {
            if (id == tree.stringId) {
                return tree
            }
        }
        return null
    }

    fun indexOf(tree: FBTree): Int = subtrees().indexOf(tree)

    abstract val name: String?

    open val treeTitle: Pair<String, String?>
        get() = Pair(name ?: "", null)

    protected open val sortKey: String?
        get() {
            val sortKey = name
            if (sortKey == null ||
                sortKey.length <= 1 ||
                sortKey[0].isLetterOrDigit()
            ) {
                return sortKey
            }

            for (i in 1 until sortKey.length) {
                if (sortKey[i].isLetterOrDigit()) {
                    return sortKey.substring(i)
                }
            }
            return sortKey
        }

    override fun compareTo(tree: FBTree): Int {
        val key0 = sortKey
        val key1 = tree.sortKey
        if (key0 == null) {
            return if (key1 == null) 0 else -1
        }
        if (key1 == null) {
            return 1
        }
        val diff = compareStringsIgnoreCase(key0, key1)
        return if (diff != 0) diff else name!!.compareTo(tree.name!!)
    }

    abstract val summary: String?

    protected open fun createCover(): ZLImage? = null

    protected open fun canUseParentCover(): Boolean = true

    fun getCover(): ZLImage? {
        if (!coverRequested) {
            myCover = createCover()
            if (myCover == null && parent != null && canUseParentCover()) {
                myCover = parent.getCover()
            }
            coverRequested = true
        }
        return myCover
    }

    open val openingStatus: Status
        get() = Status.READY_TO_OPEN

    open val openingStatusMessage: String?
        get() = null

    open fun waitForOpening() {}

    enum class Status {
        READY_TO_OPEN,
        WAIT_FOR_OPEN,
        ALWAYS_RELOAD_BEFORE_OPENING,
        CANNOT_OPEN
    }

    class Key(val parent: Key?, val id: String) : Serializable {
        companion object {
            private const val serialVersionUID: Long = -6500763093522202052L
        }

        init {
            require(id.isNotEmpty()) { "FBTree.Key string id must be non-null" }
        }

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is Key) return false
            return id == other.id && ComparisonUtil.equal(parent, other.parent)
        }

        override fun hashCode(): Int = id.hashCode()

        override fun toString(): String = if (parent == null) id else "${parent} :: $id"
    }
}
