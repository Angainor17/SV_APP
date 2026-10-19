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

package org.geometerplus.zlibrary.core.tree

import java.util.ArrayList
import java.util.Collections
import java.util.LinkedList
import kotlin.jvm.Volatile

@Suppress("UNCHECKED_CAST")
abstract class ZLTree<T : ZLTree<T>> : Iterable<T> {
    @JvmField
    val Parent: T?

    @JvmField
    val Level: Int

    private var mySize: Int = 1

    @Volatile
    private var mySubtrees: MutableList<T>? = null

    protected constructor() : this(null)

    protected constructor(parent: T?) : this(parent, -1)

    protected constructor(parent: T?, position: Int) {
        var pos = position
        if (pos == -1) {
            pos = if (parent == null) 0 else parent.subtrees().size
        }
        if (parent != null && (pos < 0 || pos > parent.subtrees().size)) {
            throw IndexOutOfBoundsException(
                "`position` value equals $pos but must be in range [0; ${parent.subtrees().size}]"
            )
        }
        Parent = parent
        if (parent != null) {
            Level = parent.Level + 1
            parent.addSubtree(this as T, pos)
        } else {
            Level = 0
        }
    }

    fun getSize(): Int = mySize

    fun hasChildren(): Boolean = mySubtrees != null && mySubtrees!!.isNotEmpty()

    fun subtrees(): List<T> {
        val subtrees = mySubtrees
        if (subtrees == null) {
            return Collections.emptyList()
        }
        synchronized(subtrees) {
            return ArrayList(subtrees)
        }
    }

    @Synchronized
    fun getTreeByParagraphNumber(index: Int): T? {
        if (index < 0 || index >= mySize) {
            // TODO: throw an exception?
            return null
        }
        if (index == 0) {
            return this as T
        }
        var i = index - 1
        val subtrees = mySubtrees
        if (subtrees != null) {
            synchronized(subtrees) {
                for (subtree in subtrees) {
                    if (subtree.mySize <= i) {
                        i -= subtree.mySize
                    } else {
                        return subtree.getTreeByParagraphNumber(i)
                    }
                }
            }
        }
        throw RuntimeException("That's impossible!!!")
    }

    @Synchronized
    private fun addSubtree(subtree: T, position: Int) {
        var sub = subtree
        if (mySubtrees == null) {
            mySubtrees = Collections.synchronizedList(ArrayList<T>())
        }
        val subtreeSize = sub.getSize()
        synchronized(mySubtrees!!) {
            val thisSubtreesSize = mySubtrees!!.size
            var pos = position
            while (pos < thisSubtreesSize) {
                sub = mySubtrees!!.set(pos++, sub)
            }
            mySubtrees!!.add(sub)
            var parent: ZLTree<*>? = this
            while (parent != null) {
                parent.mySize += subtreeSize
                parent = parent.Parent
            }
        }
    }

    @Synchronized
    fun moveSubtree(subtree: T, index: Int) {
        val subtrees = mySubtrees
        if (subtrees == null || !subtrees.contains(subtree)) {
            return
        }
        if (index < 0 || index >= subtrees.size) {
            return
        }
        subtrees.remove(subtree)
        subtrees.add(index, subtree)
    }

    fun removeSelf() {
        val subtreeSize = getSize()
        var parent: ZLTree<*>? = Parent
        if (parent != null) {
            (parent.mySubtrees!! as MutableList<Any?>).remove(this)
            while (parent != null) {
                parent.mySize -= subtreeSize
                parent = parent.Parent
            }
        }
    }

    fun clear() {
        val subtreesSize = mySize - 1
        mySubtrees?.clear()
        mySize = 1
        if (subtreesSize > 0) {
            var parent: ZLTree<*>? = Parent
            while (parent != null) {
                parent.mySize -= subtreesSize
                parent = parent.Parent
            }
        }
    }

    override fun iterator(): Iterator<T> = TreeIterator(Int.MAX_VALUE)

    fun allSubtrees(maxLevel: Int): Iterable<T> =
        object : Iterable<T> {
            override fun iterator(): Iterator<T> = TreeIterator(maxLevel)
        }

    private inner class TreeIterator : Iterator<T> {
        private val myIndexStack = LinkedList<Int>()
        private val myMaxLevel: Int
        private var myCurrentElement: T? = this@ZLTree as T

        constructor(maxLevel: Int) {
            myMaxLevel = maxLevel
        }

        override fun hasNext(): Boolean = myCurrentElement != null

        override fun next(): T {
            val element = myCurrentElement!!
            if (element.hasChildren() && element.Level < myMaxLevel) {
                myCurrentElement = element.mySubtrees!![0]
                myIndexStack.add(0)
            } else {
                var parent = element
                while (myIndexStack.isNotEmpty()) {
                    val index = myIndexStack.removeLast() + 1
                    parent = parent.Parent!!
                    synchronized(parent.mySubtrees!!) {
                        if (parent.mySubtrees!!.size > index) {
                            myCurrentElement = parent.mySubtrees!![index]
                            myIndexStack.add(index)
                            break
                        }
                    }
                }
                if (myIndexStack.isEmpty()) {
                    myCurrentElement = null
                }
            }
            return element
        }

    }
}
