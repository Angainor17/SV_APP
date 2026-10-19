/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
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
import org.geometerplus.fbreader.sort.TitledEntity
import org.geometerplus.zlibrary.core.util.MiscUtil
import org.geometerplus.zlibrary.core.util.RationalNumber
import java.math.BigDecimal

abstract class AbstractBook internal constructor(
    id: Long,
    title: String?,
    encoding: String?,
    language: String?,
) : TitledEntity<AbstractBook>(title) {

    @Volatile
    @JvmField
    var HasBookmark: Boolean = false

    @Volatile
    protected var myId: Long = id

    @Volatile
    internal var myEncoding: String? = encoding

    @Volatile
    internal var myLanguage: String? = language

    @Volatile
    internal var myAuthors: MutableList<Author>? = null

    @Volatile
    internal var myTags: MutableList<Tag>? = null

    @Volatile
    protected var myLabels: MutableList<Label>? = null

    @Volatile
    internal var mySeriesInfo: SeriesInfo? = null

    @Volatile
    internal var myUids: MutableList<UID>? = null

    @Volatile
    protected var myProgress: RationalNumber? = null

    @Volatile
    internal var mySaveState: SaveState = SaveState.Saved

    abstract fun getPath(): String

    fun updateFrom(book: AbstractBook?) {
        if (book == null || myId != book.myId) {
            return
        }
        setTitle(book.getTitle())
        setEncoding(book.myEncoding)
        setLanguage(book.myLanguage)
        if (!ComparisonUtil.equal(myAuthors, book.myAuthors)) {
            myAuthors = book.myAuthors?.let { ArrayList(it) }
            mySaveState = SaveState.NotSaved
        }
        if (!ComparisonUtil.equal(myTags, book.myTags)) {
            myTags = book.myTags?.let { ArrayList(it) }
            mySaveState = SaveState.NotSaved
        }
        if (!MiscUtil.listsEquals(myLabels, book.myLabels)) {
            myLabels = book.myLabels?.let { ArrayList(it) }
            mySaveState = SaveState.NotSaved
        }
        if (!ComparisonUtil.equal(mySeriesInfo, book.mySeriesInfo)) {
            mySeriesInfo = book.mySeriesInfo
            mySaveState = SaveState.NotSaved
        }
        if (!MiscUtil.listsEquals(myUids, book.myUids)) {
            myUids = book.myUids?.let { ArrayList(it) }
            mySaveState = SaveState.NotSaved
        }
        setProgress(book.myProgress)
        if (HasBookmark != book.HasBookmark) {
            HasBookmark = book.HasBookmark
            mySaveState = SaveState.NotSaved
        }
    }

    fun authors(): List<Author> = myAuthors ?: emptyList()

    fun authorsString(separator: String): String? {
        val authors = myAuthors
        if (authors == null || authors.isEmpty()) {
            return null
        }

        val buffer = StringBuilder()
        var first = true
        for (a in authors) {
            if (!first) {
                buffer.append(separator)
            }
            buffer.append(a.DisplayName)
            first = false
        }
        return buffer.toString()
    }

    fun addAuthorWithNoCheck(author: Author) {
        val authors = myAuthors ?: ArrayList<Author>().also { myAuthors = it }
        authors.add(author)
    }

    fun removeAllAuthors() {
        if (myAuthors != null) {
            myAuthors = null
            mySaveState = SaveState.NotSaved
        }
    }

    fun addAuthor(author: Author?) {
        if (author == null) {
            return
        }
        val authors = myAuthors ?: ArrayList<Author>().also { myAuthors = it }
        if (!authors.contains(author)) {
            authors.add(author)
            mySaveState = SaveState.NotSaved
        }
    }

    fun addAuthor(name: String?) {
        addAuthor(name, null)
    }

    fun addAuthor(name: String?, sortKey: String?) {
        addAuthor(Author.create(name, sortKey))
    }

    fun getId(): Long = myId

    override fun setTitle(title: String?) {
        if (title == null) {
            return
        }
        val t = title.trim()
        if (t.isEmpty()) {
            return
        }
        if (getTitle() != t) {
            super.setTitle(t)
            mySaveState = SaveState.NotSaved
        }
    }

    fun getSeriesInfo(): SeriesInfo? = mySeriesInfo

    fun setSeriesInfoWithNoCheck(name: String?, index: String?) {
        mySeriesInfo = SeriesInfo.createSeriesInfo(name, index)
    }

    fun setSeriesInfo(name: String?, index: String?) {
        setSeriesInfo(name, SeriesInfo.createIndex(index))
    }

    fun setSeriesInfo(name: String?, index: BigDecimal?) {
        val seriesInfo = mySeriesInfo
        if (seriesInfo == null) {
            if (name != null) {
                mySeriesInfo = SeriesInfo(name, index)
                mySaveState = SaveState.NotSaved
            }
        } else if (name == null) {
            mySeriesInfo = null
            mySaveState = SaveState.NotSaved
        } else if (name != seriesInfo.Series.getTitle() || seriesInfo.Index !== index) {
            mySeriesInfo = SeriesInfo(name, index)
            mySaveState = SaveState.NotSaved
        }
    }

    override fun getLanguage(): String = myLanguage ?: ""

    fun setLanguage(language: String?) {
        if (!ComparisonUtil.equal(myLanguage, language)) {
            myLanguage = language
            resetSortKey()
            mySaveState = SaveState.NotSaved
        }
    }

    fun getEncodingNoDetection(): String? = myEncoding

    fun setEncoding(encoding: String?) {
        if (!ComparisonUtil.equal(myEncoding, encoding)) {
            myEncoding = encoding
            mySaveState = SaveState.NotSaved
        }
    }

    fun tags(): List<Tag> = myTags ?: emptyList()

    fun tagsString(separator: String): String? {
        val tags = myTags
        if (tags == null || tags.isEmpty()) {
            return null
        }

        val tagNames = HashSet<String>()
        val buffer = StringBuilder()
        var first = true
        for (t in tags) {
            if (!first) {
                buffer.append(separator)
            }
            if (!tagNames.contains(t.Name)) {
                tagNames.add(t.Name)
                buffer.append(t.Name)
                first = false
            }
        }
        return buffer.toString()
    }

    fun addTagWithNoCheck(tag: Tag) {
        val tags = myTags ?: ArrayList<Tag>().also { myTags = it }
        tags.add(tag)
    }

    fun removeAllTags() {
        if (myTags != null) {
            myTags = null
            mySaveState = SaveState.NotSaved
        }
    }

    fun addTag(tag: Tag?) {
        if (tag != null) {
            val tags = myTags ?: ArrayList<Tag>().also { myTags = it }
            if (!tags.contains(tag)) {
                tags.add(tag)
                mySaveState = SaveState.NotSaved
            }
        }
    }

    fun addTag(tagName: String?) {
        addTag(Tag.getTag(null, tagName))
    }

    fun hasLabel(name: String): Boolean {
        for (l in labels()) {
            if (name == l.Name) {
                return true
            }
        }
        return false
    }

    fun labels(): List<Label> = myLabels ?: emptyList()

    fun addLabelWithNoCheck(label: Label) {
        val labels = myLabels ?: ArrayList<Label>().also { myLabels = it }
        labels.add(label)
    }

    fun addNewLabel(label: String) {
        addLabel(Label(label))
    }

    fun addLabel(label: Label) {
        val labels = myLabels ?: ArrayList<Label>().also { myLabels = it }
        if (!labels.contains(label)) {
            labels.add(label)
            mySaveState = SaveState.NotSaved
        }
    }

    fun removeLabel(label: String) {
        val labels = myLabels
        if (labels != null && labels.remove(Label(label))) {
            mySaveState = SaveState.NotSaved
        }
    }

    fun uids(): List<UID> = myUids ?: emptyList()

    fun addUid(type: String, id: String) {
        addUid(UID(type, id))
    }

    fun addUidWithNoCheck(uid: UID?) {
        if (uid == null) {
            return
        }
        val uids = myUids ?: ArrayList<UID>().also { myUids = it }
        uids.add(uid)
    }

    fun addUid(uid: UID?) {
        if (uid == null) {
            return
        }
        val uids = myUids ?: ArrayList<UID>().also { myUids = it }
        if (!uids.contains(uid)) {
            uids.add(uid)
            mySaveState = SaveState.NotSaved
        }
    }

    fun matchesUid(uid: UID): Boolean = myUids?.contains(uid) ?: false

    fun getProgress(): RationalNumber? = myProgress

    fun setProgress(progress: RationalNumber?) {
        if (!ComparisonUtil.equal(myProgress, progress)) {
            myProgress = progress
            if (mySaveState == SaveState.Saved) {
                mySaveState = SaveState.ProgressNotSaved
            }
        }
    }

    fun setProgressWithNoCheck(progress: RationalNumber?) {
        myProgress = progress
    }

    fun matches(pattern: String): Boolean {
        if (MiscUtil.matchesIgnoreCase(getTitle(), pattern)) {
            return true
        }
        val seriesInfo = mySeriesInfo
        if (seriesInfo != null && MiscUtil.matchesIgnoreCase(seriesInfo.Series.getTitle(), pattern)) {
            return true
        }
        val authors = myAuthors
        if (authors != null) {
            for (author in authors) {
                if (MiscUtil.matchesIgnoreCase(author.DisplayName, pattern)) {
                    return true
                }
            }
        }
        val tags = myTags
        if (tags != null) {
            for (tag in tags) {
                if (MiscUtil.matchesIgnoreCase(tag.Name, pattern)) {
                    return true
                }
            }
        }

        var fileName = getPath()
        // first archive delimiter
        var index = fileName.indexOf(":")
        // last path delimiter before first archive delimiter
        if (index == -1) {
            index = fileName.lastIndexOf("/")
        } else {
            index = fileName.lastIndexOf("/", index)
        }
        fileName = fileName.substring(index + 1)
        return MiscUtil.matchesIgnoreCase(fileName, pattern)
    }

    override fun toString(): String =
        javaClass.name + "[" + getPath() + ", " + myId + ", " + getTitle() + "]"

    internal enum class SaveState {
        Saved,
        ProgressNotSaved,
        NotSaved,
    }

    companion object {
        const val FAVORITE_LABEL = "favorite"
        const val READ_LABEL = "read"
        const val SYNCHRONISED_LABEL = "sync-success"
        const val SYNC_FAILURE_LABEL = "sync-failure"
        const val SYNC_DELETED_LABEL = "sync-deleted"
        const val SYNC_TOSYNC_LABEL = "sync-tosync"
    }
}
