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
import org.geometerplus.fbreader.formats.BookReadingException
import org.geometerplus.fbreader.formats.FormatPlugin
import org.geometerplus.fbreader.formats.PluginCollection
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.util.MiscUtil
import java.util.TreeSet

class DbBook(
    id: Long,
    file: ZLFile?,
    title: String?,
    encoding: String?,
    language: String?,
) : AbstractBook(id, title, encoding, language) {

    @JvmField
    val File: ZLFile = file ?: throw IllegalArgumentException("Creating book with no file")

    private var myVisitedHyperlinks: MutableSet<String>? = null

    constructor(file: ZLFile, plugin: FormatPlugin) :
        this(-1L, plugin.realBookFile(file), null, null, null) {
        BookUtil.readMetainfo(this, plugin)
        mySaveState = SaveState.NotSaved
    }

    override fun getPath(): String = File.getPath()

    fun loadLists(database: BooksDatabase, pluginCollection: PluginCollection) {
        myAuthors = database.listAuthors(myId)
        myTags = database.listTags(myId)
        myLabels = database.listLabels(myId)
        mySeriesInfo = database.getSeriesInfo(myId)
        myUids = database.listUids(myId)
        myProgress = database.getProgress(myId)
        HasBookmark = database.hasVisibleBookmark(myId)
        mySaveState = SaveState.Saved
        val uids = myUids
        if (uids == null || uids.isEmpty()) {
            try {
                BookUtil.getPlugin(pluginCollection, this).readUids(this)
                save(database, false)
            } catch (e: BookReadingException) {
            }
        }
    }

    fun save(database: BooksDatabase, force: Boolean): WhatIsSaved {
        if (force || myId == -1L) {
            mySaveState = SaveState.NotSaved
        }
        return when (mySaveState) {
            SaveState.Saved -> WhatIsSaved.Nothing
            SaveState.ProgressNotSaved ->
                if (saveProgress(database)) WhatIsSaved.Progress else WhatIsSaved.Nothing
            else -> if (saveFull(database)) WhatIsSaved.Everything else WhatIsSaved.Nothing
        }
    }

    private fun saveProgress(database: BooksDatabase): Boolean {
        var result = false
        database.executeAsTransaction {
            val progress = myProgress
            if (myId != -1L && progress != null) {
                database.saveBookProgress(myId, progress)
                result = true
            }
        }
        if (result) {
            mySaveState = SaveState.Saved
            return true
        }
        return false
    }

    private fun saveFull(database: BooksDatabase): Boolean {
        var result = true
        database.executeAsTransaction {
            if (myId >= 0) {
                val fileInfos = FileInfoSet(database, File)
                database.updateBookInfo(myId, fileInfos.getId(File), myEncoding, myLanguage, getTitle())
            } else {
                myId = database.insertBookInfo(File, myEncoding, myLanguage, getTitle())
                if (myId == -1L) {
                    result = false
                    return@executeAsTransaction
                }
                val links = myVisitedHyperlinks
                if (links != null) {
                    for (linkId in links) {
                        database.addVisitedHyperlink(myId, linkId)
                    }
                }
                database.addBookHistoryEvent(myId, BooksDatabase.HistoryEvent.Added)
            }

            var index = 0L
            database.deleteAllBookAuthors(myId)
            for (author in authors()) {
                database.saveBookAuthorInfo(myId, index, author)
                index++
            }
            database.deleteAllBookTags(myId)
            for (tag in tags()) {
                database.saveBookTagInfo(myId, tag)
            }
            val labelsInDb = database.listLabels(myId)
            val labels = myLabels
            for (label in labelsInDb) {
                if (labels == null || !labels.contains(label)) {
                    database.removeLabel(myId, label)
                }
            }
            if (labels != null) {
                for (label in labels) {
                    database.addLabel(myId, label)
                }
            }
            database.saveBookSeriesInfo(myId, mySeriesInfo)
            database.deleteAllBookUids(myId)
            for (uid in uids()) {
                database.saveBookUid(myId, uid)
            }
            val progress = myProgress
            if (progress != null) {
                database.saveBookProgress(myId, progress)
            }
        }
        if (result) {
            mySaveState = SaveState.Saved
            return true
        }
        return false
    }

    private fun initHyperlinkSet(database: BooksDatabase) {
        if (myVisitedHyperlinks == null) {
            val links = TreeSet<String>()
            myVisitedHyperlinks = links
            if (myId != -1L) {
                links.addAll(database.loadVisitedHyperlinks(myId))
            }
        }
    }

    fun isHyperlinkVisited(database: BooksDatabase, linkId: String): Boolean {
        initHyperlinkSet(database)
        return myVisitedHyperlinks!!.contains(linkId)
    }

    fun markHyperlinkAsVisited(database: BooksDatabase, linkId: String) {
        initHyperlinkSet(database)
        val links = myVisitedHyperlinks!!
        if (!links.contains(linkId)) {
            links.add(linkId)
            if (myId != -1L) {
                database.addVisitedHyperlink(myId, linkId)
            }
        }
    }

    fun hasSameMetainfoAs(other: DbBook): Boolean =
        ComparisonUtil.equal(getTitle(), other.getTitle()) &&
            ComparisonUtil.equal(myEncoding, other.myEncoding) &&
            ComparisonUtil.equal(myLanguage, other.myLanguage) &&
            ComparisonUtil.equal(myAuthors, other.myAuthors) &&
            MiscUtil.listsEquals(myTags, other.myTags) &&
            ComparisonUtil.equal(mySeriesInfo, other.mySeriesInfo) &&
            ComparisonUtil.equal(myUids, other.myUids)

    fun merge(other: DbBook, base: DbBook) {
        if (!ComparisonUtil.equal(getTitle(), other.getTitle()) &&
            ComparisonUtil.equal(getTitle(), base.getTitle())
        ) {
            setTitle(other.getTitle())
        }
        if (!ComparisonUtil.equal(myEncoding, other.myEncoding) &&
            ComparisonUtil.equal(myEncoding, base.myEncoding)
        ) {
            setEncoding(other.myEncoding)
        }
        if (!ComparisonUtil.equal(myLanguage, other.myLanguage) &&
            ComparisonUtil.equal(myLanguage, base.myLanguage)
        ) {
            setLanguage(other.myLanguage)
        }
        if (!MiscUtil.listsEquals(myTags, other.myTags) &&
            MiscUtil.listsEquals(myTags, base.myTags)
        ) {
            myTags = other.myTags?.let { ArrayList(it) }
            mySaveState = SaveState.NotSaved
        }
        if (!ComparisonUtil.equal(mySeriesInfo, other.mySeriesInfo) &&
            ComparisonUtil.equal(mySeriesInfo, base.mySeriesInfo)
        ) {
            mySeriesInfo = other.mySeriesInfo
            mySaveState = SaveState.NotSaved
        }
        if (!MiscUtil.listsEquals(myUids, other.myUids) &&
            MiscUtil.listsEquals(myUids, base.myUids)
        ) {
            myUids = other.myUids?.let { ArrayList(it) }
            mySaveState = SaveState.NotSaved
        }
    }

    override fun hashCode(): Int = File.getShortName().hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is DbBook) {
            return false
        }
        val ofile = other.File
        if (File == ofile) {
            return true
        }
        if (File.getShortName() != ofile.getShortName()) {
            return false
        }
        val uids = myUids
        val otherUids = other.myUids
        if (uids == null || otherUids == null) {
            return false
        }
        for (uid in otherUids) {
            if (uids.contains(uid)) {
                return true
            }
        }
        return false
    }

    enum class WhatIsSaved {
        Nothing,
        Progress,
        Everything,
    }
}
