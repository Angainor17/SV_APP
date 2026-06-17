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

package org.geometerplus.fbreader.bookmodel

import org.geometerplus.zlibrary.core.tree.ZLTree
import org.geometerplus.zlibrary.text.model.ZLTextModel

open class TOCTree : ZLTree<TOCTree> {
    private var text: String? = null
    private var myReference: Reference? = null

    val reference: Reference?
        get() = myReference

    internal constructor() : super()

    constructor(parent: TOCTree) : super(parent)

    companion object {
        @JvmStatic
        fun createRoot(): TOCTree = TOCTree()
    }

    // faster replacement for
    // return text.trim().replaceAll("[\t ]+", " ");
    private fun trim(text: String): String {
        val data = text.toCharArray()
        var count = 0
        var shift = 0
        var changed = false
        var space = ' '
        for (i in data.indices) {
            val ch = data[i]
            if (ch == ' ' || ch == '\t') {
                ++count
                space = ch
            } else {
                if (count > 0) {
                    if (count == i) {
                        shift += count
                        changed = true
                    } else {
                        shift += count - 1
                        if (shift > 0 || space == '\t') {
                            data[i - shift - 1] = ' '
                            changed = true
                        }
                    }
                    count = 0
                }
                if (shift > 0) {
                    data[i - shift] = data[i]
                }
            }
        }
        if (count > 0) {
            changed = true
            shift += count
        }
        return if (changed) String(data, 0, data.size - shift) else text
    }

    fun getText(): String? = text

    fun setText(text: String?) {
        this.text = text?.let { trim(it) }
    }

    fun setReference(model: ZLTextModel?, reference: Int) {
        if (model != null) {
            myReference = Reference(reference, model)
        }
    }

    class Reference(@JvmField val paragraphIndex: Int, @JvmField val model: ZLTextModel)
}
