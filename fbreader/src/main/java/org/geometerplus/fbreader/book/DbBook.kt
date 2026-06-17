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
    @JvmField val file: ZLFile,
    title: String?,
    encoding: String?,
    language: String?
) : AbstractBook(id, title, encoding, language) {

    private var visitedHyperlinks: MutableSet<String>? = null

    init {
        requireNotNull(file) { "Creating book with no file" }
    }

    @Throws(BookReadingException::class)
    internal constructor(file: ZLFile, plugin: FormatPlugin) : this(-1, plugin.realBookFile(file), null, null, null) {
        BookUtil.readMetainfo(this, plugin)
        saveState = SaveState.NotSaved
    }

    override fun getPath(): String = file.path

    internal fun loadLists(database: BooksDatabase, pluginCollection: PluginCollection) {
        authors = database.listAuthors(id)
        tags = database.listTags(id)
        labels = database.listLabels(id)
        seriesInfo = database.getSeriesInfo(id)
        uids = database.listUids(id)
        progress = database.getProgress(id)
        hasBookmark = database.hasVisibleBookmark(id)
        saveState = SaveState.Saved
        if (uids == null || uids!!.isEmpty()) {
            try {
                BookUtil.getPlugin(pluginCollection, this).readUids(this)
                save(database, false)
            } catch (e: BookReadingException) {
            }
        }
    }

    internal fun save(database: BooksDatabase, force: Boolean): WhatIsSaved {
        if (force || id == -1L) {
            saveState = SaveState.NotSaved
        }

        return when (saveState) {
            SaveState.Saved -> WhatIsSaved.Nothing
            SaveState.ProgressNotSaved -> if (saveProgress(database)) WhatIsSaved.Progress else WhatIsSaved.Nothing
            SaveState.NotSaved -> if (saveFull(database)) WhatIsSaved.Everything else WhatIsSaved.Nothing
        }
    }

    private fun saveProgress(database: BooksDatabase): Boolean {
        var result = false
        database.executeAsTransaction {
            if (id != -1L && progress != null) {
                database.saveBookProgress(id, progress!!)
                result = true
            }
        }

        if (result) {
            saveState = SaveState.Saved
        }
        return result
    }

    private fun saveFull(database: BooksDatabase): Boolean {
        var result = true
        database.executeAsTransaction {
            if (id >= 0) {
                val fileInfos = FileInfoSet(database, file)
                database.updateBookInfo(id, fileInfos.getId(file), encoding, language, getTitle())
            } else {
                id = database.insertBookInfo(file, encoding, language, getTitle())
                if (id == -1L) {
                    result = false
                    return@executeAsTransaction
                }
                visitedHyperlinks?.let { links ->
                    for (linkId in links) {
                        database.addVisitedHyperlink(id, linkId)
                    }
                }
                database.addBookHistoryEvent(id, BooksDatabase.HistoryEvent.Added)
            }

            var index = 0L
            database.deleteAllBookAuthors(id)
            for (author in authors()) {
                database.saveBookAuthorInfo(id, index++, author)
            }
            database.deleteAllBookTags(id)
            for (tag in tags()) {
                database.saveBookTagInfo(id, tag)
            }
            val labelsInDb = database.listLabels(id) ?: emptyList()
            for (label in labelsInDb) {
                if (labels == null || !labels!!.any { it.name == label.name }) {
                    database.removeLabel(id, label)
                }
            }
            labels?.let { labelList ->
                for (label in labelList) {
                    database.addLabel(id, label)
                }
            }
            database.saveBookSeriesInfo(id, seriesInfo)
            database.deleteAllBookUids(id)
            for (uid in uids()) {
                database.saveBookUid(id, uid)
            }
            progress?.let {
                database.saveBookProgress(id, it)
            }
        }

        if (result) {
            saveState = SaveState.Saved
        }
        return result
    }

    private fun initHyperlinkSet(database: BooksDatabase) {
        if (visitedHyperlinks == null) {
            visitedHyperlinks = TreeSet()
            if (id != -1L) {
                visitedHyperlinks!!.addAll(database.loadVisitedHyperlinks(id))
            }
        }
    }

    internal fun isHyperlinkVisited(database: BooksDatabase, linkId: String): Boolean {
        initHyperlinkSet(database)
        return visitedHyperlinks!!.contains(linkId)
    }

    internal fun markHyperlinkAsVisited(database: BooksDatabase, linkId: String) {
        initHyperlinkSet(database)
        if (!visitedHyperlinks!!.contains(linkId)) {
            visitedHyperlinks!!.add(linkId)
            if (id != -1L) {
                database.addVisitedHyperlink(id, linkId)
            }
        }
    }

    internal fun hasSameMetainfoAs(other: DbBook): Boolean =
        ComparisonUtil.equal(getTitle(), other.getTitle()) &&
        ComparisonUtil.equal(encoding, other.encoding) &&
        ComparisonUtil.equal(language, other.language) &&
        ComparisonUtil.equal(authors, other.authors) &&
        MiscUtil.listsEquals(tags, other.tags) &&
        ComparisonUtil.equal(seriesInfo, other.seriesInfo) &&
        ComparisonUtil.equal(uids, other.uids)

    internal fun merge(other: DbBook, base: DbBook) {
        if (!ComparisonUtil.equal(getTitle(), other.getTitle()) &&
            ComparisonUtil.equal(getTitle(), base.getTitle())) {
            setTitle(other.getTitle())
        }
        if (!ComparisonUtil.equal(encoding, other.encoding) &&
            ComparisonUtil.equal(encoding, base.encoding)) {
            setEncoding(other.encoding)
        }
        if (!ComparisonUtil.equal(language, other.language) &&
            ComparisonUtil.equal(language, base.language)) {
            setLanguage(other.language)
        }
        if (!MiscUtil.listsEquals(tags, other.tags) &&
            MiscUtil.listsEquals(tags, base.tags)) {
            tags = other.tags?.let { ArrayList(it) }
            saveState = SaveState.NotSaved
        }
        if (!ComparisonUtil.equal(seriesInfo, other.seriesInfo) &&
            ComparisonUtil.equal(seriesInfo, base.seriesInfo)) {
            seriesInfo = other.seriesInfo
            saveState = SaveState.NotSaved
        }
        if (!MiscUtil.listsEquals(uids, other.uids) &&
            MiscUtil.listsEquals(uids, base.uids)) {
            uids = other.uids?.let { ArrayList(it) }
            saveState = SaveState.NotSaved
        }
    }

    override fun hashCode(): Int = file.shortName.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DbBook) return false

        val obook = other
        val ofile = obook.file
        if (file == ofile) return true
        if (file.shortName != ofile.shortName) return false
        if (uids == null || obook.uids == null) return false

        for (uid in obook.uids!!) {
            if (uids!!.contains(uid)) return true
        }
        return false
    }

    internal enum class WhatIsSaved {
        Nothing,
        Progress,
        Everything
    }
}
