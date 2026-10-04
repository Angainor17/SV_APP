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
import java.util.Comparator
import java.util.UUID

class Bookmark(
    id: Long, uid: String?, versionUid: String?,
    bookId: Long, bookTitle: String?, text: String?, originalText: String?,
    creationTimestamp: Long, modificationTimestamp: Long?, accessTimestamp: Long?,
    modelId: String?,
    startParagraphIndex: Int, startElementIndex: Int, startCharIndex: Int,
    endParagraphIndex: Int, endElementIndex: Int, endCharIndex: Int,
    isVisible: Boolean,
    styleId: Int,
) : ZLTextFixedPosition(startParagraphIndex, startElementIndex, startCharIndex) {

    @JvmField val Uid: String? = verifiedUUID(uid)
    @JvmField val BookId: Long = bookId
    @JvmField val BookTitle: String? = bookTitle
    @JvmField val CreationTimestamp: Long = creationTimestamp
    @JvmField val ModelId: String? = modelId
    @JvmField val IsVisible: Boolean = isVisible

    private var myId: Long = id
    private var myVersionUid: String? = verifiedUUID(versionUid)
    private var myText: String? = text
    private var myOriginalText: String? = originalText
    private var myModificationTimestamp: Long? = modificationTimestamp
    private var myAccessTimestamp: Long? = accessTimestamp
    private var myEnd: ZLTextFixedPosition? =
        if (endCharIndex >= 0) ZLTextFixedPosition(endParagraphIndex, endElementIndex, endCharIndex) else null
    private var myLength: Int = if (endCharIndex >= 0) 0 else endParagraphIndex
    private var myStyleId: Int = styleId

    // used for migration only
    private constructor(bookId: Long, original: Bookmark) : this(
        -1L, newUUID(), null,
        bookId, original.BookTitle, original.myText, original.myOriginalText,
        original.CreationTimestamp, original.myModificationTimestamp, original.myAccessTimestamp,
        original.ModelId,
        original.ParagraphIndex, original.ElementIndex, original.CharIndex,
        original.myEnd?.ParagraphIndex ?: original.myLength,
        original.myEnd?.ElementIndex ?: 0,
        original.myEnd?.CharIndex ?: -1,
        original.IsVisible,
        original.myStyleId,
    )

    // creates new bookmark
    constructor(
        collection: IBookCollection<*>, book: Book, modelId: String?, snippet: TextSnippet, visible: Boolean,
    ) : this(
        -1L, newUUID(), null,
        book.getId(), book.getTitle(), snippet.getText(), null,
        System.currentTimeMillis(), null, null,
        modelId,
        snippet.getStart().paragraphIndex, snippet.getStart().elementIndex, snippet.getStart().charIndex,
        snippet.getEnd().paragraphIndex, snippet.getEnd().elementIndex, snippet.getEnd().charIndex,
        visible,
        collection.getDefaultHighlightingStyleId(),
    )

    fun getId(): Long = myId

    fun setId(id: Long) {
        myId = id
    }

    fun getVersionUid(): String? = myVersionUid

    private fun onModification() {
        myVersionUid = newUUID()
        myModificationTimestamp = System.currentTimeMillis()
    }

    fun getStyleId(): Int = myStyleId

    fun setStyleId(styleId: Int) {
        if (styleId != myStyleId) {
            myStyleId = styleId
            onModification()
        }
    }

    fun getText(): String? = myText

    fun setText(text: String) {
        if (text != myText) {
            if (myOriginalText == null) {
                myOriginalText = myText
            } else if (myOriginalText == text) {
                myOriginalText = null
            }
            myText = text
            onModification()
        }
    }

    fun getOriginalText(): String? = myOriginalText

    fun getTimestamp(type: DateType): Long? = when (type) {
        DateType.Creation -> CreationTimestamp
        DateType.Modification -> myModificationTimestamp
        DateType.Access -> myAccessTimestamp
        DateType.Latest -> {
            val latest: Long = myModificationTimestamp ?: CreationTimestamp
            val access = myAccessTimestamp
            if (access != null && latest < access) access else latest
        }
    }

    fun getEnd(): ZLTextPosition? = myEnd

    fun setEnd(paragraphsIndex: Int, elementIndex: Int, charIndex: Int) {
        myEnd = ZLTextFixedPosition(paragraphsIndex, elementIndex, charIndex)
    }

    fun getLength(): Int = myLength

    fun markAsAccessed() {
        myVersionUid = newUUID()
        myAccessTimestamp = System.currentTimeMillis()
    }

    fun update(other: Bookmark?) {
        if (other != null) {
            myId = other.myId
        }
    }

    fun transferToBook(book: AbstractBook): Bookmark? {
        val bookId = book.getId()
        return if (bookId != -1L) Bookmark(bookId, this) else null
    }

    // not equals, we do not compare ids
    fun sameAs(other: Bookmark): Boolean =
        ParagraphIndex == other.ParagraphIndex &&
            ElementIndex == other.ElementIndex &&
            CharIndex == other.CharIndex &&
            ComparisonUtil.equal(myText, other.myText)

    enum class DateType {
        Creation,
        Modification,
        Access,
        Latest,
    }

    class ByTimeComparator : Comparator<Bookmark> {
        override fun compare(bm0: Bookmark, bm1: Bookmark): Int {
            val ts0 = bm0.getTimestamp(DateType.Latest)
            val ts1 = bm1.getTimestamp(DateType.Latest)
            // yes, reverse order; yes, latest ts is not null
            return ts1!!.compareTo(ts0!!)
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
