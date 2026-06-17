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

package org.geometerplus.fbreader.library

import org.fbreader.util.NaturalOrderComparator
import org.fbreader.util.Pair
import org.geometerplus.fbreader.book.Book
import org.geometerplus.fbreader.book.CoverUtil
import org.geometerplus.fbreader.tree.FBTree
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.image.ZLImage

import java.util.TreeSet

class FileTree : LibraryTree {

    companion object {
        private val naturalOrderComparator = NaturalOrderComparator()
        private val NULL_BOOK = Any()

        private val fileComparator: Comparator<ZLFile> = Comparator { file0, file1 ->
            val isDir = file0.isDirectory
            if (isDir != file1.isDirectory) {
                return@Comparator if (isDir) -1 else 1
            }
            naturalOrderComparator.compare(file0.shortName, file1.shortName)
        }
    }

    val file: ZLFile
    private val _name: String?
    private val _summary: String?
    private val isSelectableFlag: Boolean
    private var book: Any? = null

    internal constructor(parent: LibraryTree, file: ZLFile, name: String?, summary: String?) : super(parent) {
        this.file = file
        this._name = name
        this._summary = summary
        this.isSelectableFlag = false
    }

    constructor(parent: FileTree, file: ZLFile) : super(parent) {
        this.file = file
        this._name = null
        this._summary = null
        this.isSelectableFlag = true
    }

    override val name: String get() = _name ?: file.shortName

    override val treeTitle: Pair<String, String?> get() = Pair(file.path, null)

    override val stringId: String get() = file.shortName

    override val summary: String? get() {
        if (_summary != null) return _summary

        val book = getBook()
        if (book != null) return book.getTitle()

        return null
    }

    override fun isSelectable(): Boolean = isSelectableFlag

    override fun createCover(): ZLImage? = CoverUtil.getCover(getBook(), pluginCollection)

    override fun getBook(): Book? {
        if (book == null) {
            book = collection.getBookByFile(file.path) ?: NULL_BOOK
        }
        return book as? Book
    }

    override fun containsBook(book: Book): Boolean {
        if (file.isDirectory) {
            var prefix = file.path
            if (!prefix.endsWith("/")) {
                prefix += "/"
            }
            return book.getPath().startsWith(prefix)
        } else if (file.isArchive) {
            return book.getPath().startsWith(file.path + ":")
        } else {
            return book == getBook()
        }
    }

    override val openingStatus: Status get() {
        return if (!file.isReadable) Status.CANNOT_OPEN else Status.ALWAYS_RELOAD_BEFORE_OPENING
    }

    override val openingStatusMessage: String? get() =
        if (openingStatus == Status.CANNOT_OPEN) "permissionDenied" else null

    override fun waitForOpening() {
        if (getBook() != null) return

        val set = TreeSet(fileComparator)
        for (fileItem in file.children()) {
            if (fileItem.isDirectory || fileItem.isArchive ||
                collection.getBookByFile(fileItem.path) != null) {
                set.add(fileItem)
            }
        }
        clear()
        for (fileItem in set) {
            FileTree(this, fileItem)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is FileTree) return true
        return file == other.file
    }

    override fun compareTo(tree: FBTree): Int =
        fileComparator.compare(file, (tree as FileTree).file)
}
