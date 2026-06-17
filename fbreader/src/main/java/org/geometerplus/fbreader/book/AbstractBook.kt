package org.geometerplus.fbreader.book

import org.fbreader.util.ComparisonUtil
import org.geometerplus.fbreader.sort.TitledEntity
import org.geometerplus.zlibrary.core.util.MiscUtil
import org.geometerplus.zlibrary.core.util.RationalNumber

import java.math.BigDecimal

abstract class AbstractBook(
    id: Long,
    title: String?,
    encoding: String?,
    language: String?
) : TitledEntity<AbstractBook>(title) {

    companion object {
        @JvmField
        val FAVORITE_LABEL = "favorite"
        @JvmField
        val READ_LABEL = "read"
        @JvmField
        val SYNCHRONISED_LABEL = "sync-success"
        @JvmField
        val SYNC_FAILURE_LABEL = "sync-failure"
        @JvmField
        val SYNC_DELETED_LABEL = "sync-deleted"
        @JvmField
        val SYNC_TOSYNC_LABEL = "sync-tosync"
    }

    @JvmField
    var hasBookmark: Boolean = false

    @Volatile
    internal open var id: Long = id
    @Volatile
    internal open var encoding: String? = encoding
    @Volatile
    private var _language: String? = language
    @Volatile
    internal open var authors: MutableList<Author>? = null
    @Volatile
    internal open var tags: MutableList<Tag>? = null
    @Volatile
    protected var labels: MutableList<Label>? = null
    @Volatile
    internal open var seriesInfo: SeriesInfo? = null
    @Volatile
    internal open var uids: MutableList<UID>? = null
    @Volatile
    internal open var progress: RationalNumber? = null
    @Volatile
    internal open var saveState: SaveState = SaveState.Saved

    init {
        this.saveState = SaveState.Saved
    }

    override val language: String?
        get() = _language

    abstract fun getPath(): String

    fun updateFrom(book: AbstractBook?) {
        if (book == null || id != book.id) {
            return
        }
        setTitle(book.getTitle())
        setEncoding(book.encoding)
        setLanguage(book.language)
        if (!ComparisonUtil.equal(authors, book.authors)) {
            authors = book.authors?.let { ArrayList(it) }
            saveState = SaveState.NotSaved
        }
        if (!ComparisonUtil.equal(tags, book.tags)) {
            tags = book.tags?.let { ArrayList(it) }
            saveState = SaveState.NotSaved
        }
        if (!MiscUtil.listsEquals(labels, book.labels)) {
            labels = book.labels?.let { ArrayList(it) }
            saveState = SaveState.NotSaved
        }
        if (!ComparisonUtil.equal(seriesInfo, book.seriesInfo)) {
            seriesInfo = book.seriesInfo
            saveState = SaveState.NotSaved
        }
        if (!MiscUtil.listsEquals(uids, book.uids)) {
            uids = book.uids?.let { ArrayList(it) }
            saveState = SaveState.NotSaved
        }
        setProgress(book.progress)
        if (hasBookmark != book.hasBookmark) {
            hasBookmark = book.hasBookmark
            saveState = SaveState.NotSaved
        }
    }

    fun authors(): List<Author> = authors?.let { it.toList() } ?: emptyList()

    fun authorsString(separator: String): String? {
        val authorList = authors ?: return null
        if (authorList.isEmpty()) return null

        val buffer = StringBuilder()
        var first = true
        for (a in authorList) {
            if (!first) {
                buffer.append(separator)
            }
            buffer.append(a.displayName)
            first = false
        }
        return buffer.toString()
    }

    internal fun addAuthorWithNoCheck(author: Author) {
        if (authors == null) {
            authors = ArrayList()
        }
        authors!!.add(author)
    }

    fun removeAllAuthors() {
        if (authors != null) {
            authors = null
            saveState = SaveState.NotSaved
        }
    }

    fun addAuthor(author: Author?) {
        if (author == null) return
        if (authors == null) {
            authors = ArrayList()
            authors!!.add(author)
            saveState = SaveState.NotSaved
        } else if (!authors!!.contains(author)) {
            authors!!.add(author)
            saveState = SaveState.NotSaved
        }
    }

    fun addAuthor(name: String) {
        addAuthor(name, null)
    }

    fun addAuthor(name: String, sortKey: String?) {
        addAuthor(Author.create(name, sortKey))
    }

    fun getId(): Long = id

    fun setTitleInternal(title: String?) {
        var newTitle = title ?: return
        newTitle = newTitle.trim()
        if (newTitle.isEmpty()) return
        if (getTitle() != newTitle) {
            setTitle(newTitle)
            saveState = SaveState.NotSaved
        }
    }

    fun getSeriesInfo(): SeriesInfo? = seriesInfo

    internal fun setSeriesInfoWithNoCheck(name: String?, index: String?) {
        seriesInfo = SeriesInfo.createSeriesInfo(name, index)
    }

    fun setSeriesInfo(name: String?, index: String?) {
        setSeriesInfo(name, SeriesInfo.createIndex(index))
    }

    fun setSeriesInfo(name: String?, index: BigDecimal?) {
        if (seriesInfo == null) {
            if (name != null) {
                seriesInfo = SeriesInfo(Series(name), index)
                saveState = SaveState.NotSaved
            }
        } else if (name == null) {
            seriesInfo = null
            saveState = SaveState.NotSaved
        } else if (name != seriesInfo!!.series.title || seriesInfo!!.index != index) {
            seriesInfo = SeriesInfo(Series(name), index)
            saveState = SaveState.NotSaved
        }
    }

    fun setLanguage(language: String?) {
        if (!ComparisonUtil.equal(_language, language)) {
            _language = language
            resetSortKey()
            saveState = SaveState.NotSaved
        }
    }

    fun getEncodingNoDetection(): String? = encoding

    fun setEncoding(encoding: String?) {
        if (!ComparisonUtil.equal(this.encoding, encoding)) {
            this.encoding = encoding
            saveState = SaveState.NotSaved
        }
    }

    fun tags(): List<Tag> = tags?.let { it.toList() } ?: emptyList()

    fun tagsString(separator: String): String? {
        val tagList = tags ?: return null
        if (tagList.isEmpty()) return null

        val tagNames = HashSet<String>()
        val buffer = StringBuilder()
        var first = true
        for (t in tagList) {
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

    internal fun addTagWithNoCheck(tag: Tag) {
        if (tags == null) {
            tags = ArrayList()
        }
        tags!!.add(tag)
    }

    fun removeAllTags() {
        if (tags != null) {
            tags = null
            saveState = SaveState.NotSaved
        }
    }

    fun addTag(tag: Tag?) {
        if (tag != null) {
            if (tags == null) {
                tags = ArrayList()
            }
            if (!tags!!.contains(tag)) {
                tags!!.add(tag)
                saveState = SaveState.NotSaved
            }
        }
    }

    fun addTag(tagName: String) {
        addTag(Tag.getTag(null, tagName))
    }

    fun hasLabel(name: String): Boolean {
        for (l in labels()) {
            if (name == l.name) {
                return true
            }
        }
        return false
    }

    fun labels(): List<Label> = labels?.let { it.toList() } ?: emptyList()

    internal fun addLabelWithNoCheck(label: Label) {
        if (labels == null) {
            labels = ArrayList()
        }
        labels!!.add(label)
    }

    fun addNewLabel(label: String) {
        addLabel(Label(label))
    }

    fun addLabel(label: Label) {
        if (labels == null) {
            labels = ArrayList()
        }
        if (!labels!!.contains(label)) {
            labels!!.add(label)
            saveState = SaveState.NotSaved
        }
    }

    fun removeLabel(label: String) {
        if (labels != null && labels!!.remove(Label(label))) {
            saveState = SaveState.NotSaved
        }
    }

    fun uids(): List<UID> = uids?.let { it.toList() } ?: emptyList()

    fun addUid(type: String, id: String) {
        addUid(UID(type, id))
    }

    internal fun addUidWithNoCheck(uid: UID?) {
        if (uid == null) return
        if (uids == null) {
            uids = ArrayList()
        }
        uids!!.add(uid)
    }

    fun addUid(uid: UID?) {
        if (uid == null) return
        if (uids == null) {
            uids = ArrayList()
        }
        if (!uids!!.contains(uid)) {
            uids!!.add(uid)
            saveState = SaveState.NotSaved
        }
    }

    fun matchesUid(uid: UID): Boolean = uids?.contains(uid) ?: false

    fun getProgress(): RationalNumber? = progress

    fun setProgress(progress: RationalNumber?) {
        if (!ComparisonUtil.equal(this.progress, progress)) {
            this.progress = progress
            if (saveState == SaveState.Saved) {
                saveState = SaveState.ProgressNotSaved
            }
        }
    }

    fun setProgressWithNoCheck(progress: RationalNumber?) {
        this.progress = progress
    }

    fun matches(pattern: String): Boolean {
        if (MiscUtil.matchesIgnoreCase(getTitle(), pattern)) {
            return true
        }
        if (seriesInfo != null && MiscUtil.matchesIgnoreCase(seriesInfo!!.series.getTitle(), pattern)) {
            return true
        }
        authors?.let { authorList ->
            for (author in authorList) {
                if (MiscUtil.matchesIgnoreCase(author.displayName, pattern)) {
                    return true
                }
            }
        }
        tags?.let { tagList ->
            for (tag in tagList) {
                if (MiscUtil.matchesIgnoreCase(tag.Name, pattern)) {
                    return true
                }
            }
        }

        var fileName = getPath()
        // first archive delimiter
        var index = fileName.indexOf(":")
        // last path delimiter before first archive delimiter
        index = if (index == -1) {
            fileName.lastIndexOf("/")
        } else {
            fileName.lastIndexOf("/", index)
        }
        fileName = fileName.substring(index + 1)
        return MiscUtil.matchesIgnoreCase(fileName, pattern)
    }

    override fun toString(): String =
        "${javaClass.name}[${getPath()}, $id, ${getTitle()}]"

    public enum class SaveState {
        Saved,
        ProgressNotSaved,
        NotSaved
    }
}
