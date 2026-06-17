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

package org.geometerplus.fbreader.book

import org.fbreader.util.ComparisonUtil
import org.geometerplus.fbreader.util.TextSnippet
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition
import org.geometerplus.zlibrary.text.view.ZLTextPosition

import java.util.UUID

class Bookmark : ZLTextFixedPosition {

    @JvmField
    val uid: String
    @JvmField
    val bookId: Long
    @JvmField
    val bookTitle: String
    @JvmField
    val creationTimestamp: Long
    @JvmField
    val modelId: String?
    @JvmField
    val isVisible: Boolean

    var id: Long = -1
        internal set
    private var versionUid: String? = null
    internal lateinit var text: String
    private var originalText: String? = null
    private var modificationTimestamp: Long? = null
    private var accessTimestamp: Long? = null
    internal var end: ZLTextFixedPosition? = null
    var length: Int = 0
        internal set
    internal var styleId: Int = 1

    // used for migration only
    private constructor(bookId: Long, original: Bookmark) : super(original) {
        this.id = -1
        this.uid = newUUID()
        this.bookId = bookId
        this.bookTitle = original.bookTitle
        this.text = original.text
        this.originalText = original.originalText
        this.creationTimestamp = original.creationTimestamp
        this.modificationTimestamp = original.modificationTimestamp
        this.accessTimestamp = original.accessTimestamp
        this.end = original.end
        this.length = original.length
        this.styleId = original.styleId
        this.modelId = original.modelId
        this.isVisible = original.isVisible
    }

    // create java object for existing bookmark
    // uid parameter can be null when comes from old format plugin!
    constructor(
        id: Long,
        uid: String?,
        versionUid: String?,
        bookId: Long,
        bookTitle: String?,
        text: String?,
        originalText: String?,
        creationTimestamp: Long,
        modificationTimestamp: Long?,
        accessTimestamp: Long?,
        modelId: String?,
        startParagraphIndex: Int,
        startElementIndex: Int,
        startCharIndex: Int,
        endParagraphIndex: Int,
        endElementIndex: Int,
        endCharIndex: Int,
        isVisible: Boolean,
        styleId: Int
    ) : super(startParagraphIndex, startElementIndex, startCharIndex) {
        this.id = id
        this.uid = verifiedUUID(uid) ?: ""
        this.versionUid = verifiedUUID(versionUid)
        this.bookId = bookId
        this.bookTitle = bookTitle ?: ""
        this.text = text ?: ""
        this.originalText = originalText
        this.creationTimestamp = creationTimestamp
        this.modificationTimestamp = modificationTimestamp
        this.accessTimestamp = accessTimestamp
        this.modelId = modelId
        this.isVisible = isVisible

        if (endCharIndex >= 0) {
            this.end = ZLTextFixedPosition(endParagraphIndex, endElementIndex, endCharIndex)
        } else {
            this.length = endParagraphIndex
            this.end = null
        }
        this.styleId = styleId
    }

    // creates new bookmark
    constructor(
        collection: IBookCollection<*>,
        book: Book,
        modelId: String?,
        snippet: TextSnippet,
        visible: Boolean
    ) : super(snippet.getStart()) {
        this.id = -1
        this.uid = newUUID()
        this.bookId = book.getId()
        this.bookTitle = book.getTitle()
        this.text = snippet.getText()
        this.originalText = null
        this.creationTimestamp = System.currentTimeMillis()
        this.modelId = modelId
        this.isVisible = visible
        this.end = ZLTextFixedPosition(snippet.getEnd())
        this.styleId = collection.getDefaultHighlightingStyleId()
        this.length = 0
    }

    fun getVersionUid(): String? = versionUid

    private fun onModification() {
        versionUid = newUUID()
        modificationTimestamp = System.currentTimeMillis()
    }

    fun getStyleId(): Int = styleId

    fun setStyleId(styleId: Int) {
        if (styleId != this.styleId) {
            this.styleId = styleId
            onModification()
        }
    }

    fun getText(): String = text

    fun setText(text: String) {
        if (text != this.text) {
            if (originalText == null) {
                originalText = this.text
            } else if (originalText == text) {
                originalText = null
            }
            this.text = text
            onModification()
        }
    }

    fun getOriginalText(): String? = originalText

    fun getTimestamp(type: DateType): Long? = when (type) {
        DateType.Creation -> creationTimestamp
        DateType.Modification -> modificationTimestamp
        DateType.Access -> accessTimestamp
        DateType.Latest -> {
            var latest = modificationTimestamp ?: creationTimestamp
            if (accessTimestamp != null && latest < accessTimestamp!!) {
                accessTimestamp
            } else {
                latest
            }
        }
    }

    fun getEnd(): ZLTextPosition? = end

    internal fun setEnd(paragraphsIndex: Int, elementIndex: Int, charIndex: Int) {
        end = ZLTextFixedPosition(paragraphsIndex, elementIndex, charIndex)
    }

    fun markAsAccessed() {
        versionUid = newUUID()
        accessTimestamp = System.currentTimeMillis()
    }

    fun update(other: Bookmark?) {
        // TODO: copy other fields (?)
        if (other != null) {
            id = other.id
        }
    }

    internal fun transferToBook(book: AbstractBook): Bookmark? {
        val bookId = book.getId()
        return if (bookId != -1L) Bookmark(bookId, this) else null
    }

    // not equals, we do not compare ids
    internal fun sameAs(other: Bookmark): Boolean =
        paragraphIndex == other.paragraphIndex &&
        elementIndex == other.elementIndex &&
        charIndex == other.charIndex &&
        ComparisonUtil.equal(text, other.text)

    fun compareToBookmark(other: Bookmark): Int {
        val byBook = bookId.compareTo(other.bookId)
        if (byBook != 0) return byBook
        return paragraphIndex.compareTo(other.paragraphIndex)
    }

    enum class DateType {
        Creation,
        Modification,
        Access,
        Latest
    }

    class ByTimeComparator : Comparator<Bookmark> {
        override fun compare(bm0: Bookmark, bm1: Bookmark): Int {
            val ts0 = bm0.getTimestamp(DateType.Latest)!!
            val ts1 = bm1.getTimestamp(DateType.Latest)!!
            // yes, reverse order; yes, latest ts is not null
            return ts1.compareTo(ts0)
        }
    }

    companion object {
        private fun newUUID(): String = UUID.randomUUID().toString()

        private fun verifiedUUID(uid: String?): String? {
            if (uid == null || uid.length == 36) {
                return uid
            }
            throw RuntimeException("INVALID UUID: $uid")
        }
    }
}
