package org.geometerplus.fbreader.book

import android.util.Xml
import org.geometerplus.zlibrary.core.constants.XMLNamespaces
import org.geometerplus.zlibrary.core.util.RationalNumber
import org.geometerplus.zlibrary.core.util.ZLColor
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import java.text.DateFormat
import java.util.Date
import java.util.Locale

internal class XMLSerializer : AbstractSerializer() {

    companion object {
        private val dateFormatter: DateFormat =
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.FULL, Locale.ENGLISH)

        private fun timestampByDate(date: Long?): String? = date?.toString()

        private fun dateByTimestamp(str: String?): Date? {
            return try {
                str?.let { Date(it.toLong()) }
            } catch (e: Exception) {
                throw SAXException("XML parsing error", e)
            }
        }

        private fun formatDate(timestamp: Long?): String? =
            timestamp?.let { dateFormatter.format(Date(it)) }

        private fun parseDate(str: String?): Long? {
            return try {
                str?.let { dateFormatter.parse(it).time }
            } catch (e: Exception) {
                throw SAXException("XML parsing error", e)
            }
        }

        private fun parseDateSafe(str: String?): Long? {
            return try {
                str?.let { dateFormatter.parse(it).time }
            } catch (e: Exception) {
                null
            }
        }

        private fun parseInt(str: String?): Int {
            return try {
                str!!.toInt()
            } catch (e: Exception) {
                throw SAXException("XML parsing error", e)
            }
        }

        private fun parseIntSafe(str: String?, defaultValue: Int): Int {
            return try {
                str?.toInt() ?: defaultValue
            } catch (e: Exception) {
                defaultValue
            }
        }

        private fun parseLong(str: String?): Long {
            return try {
                str!!.toLong()
            } catch (e: Exception) {
                throw SAXException("XML parsing error", e)
            }
        }

        private fun parseLongSafe(str: String?, defaultValue: Long): Long {
            return try {
                str?.toLong() ?: defaultValue
            } catch (e: Exception) {
                defaultValue
            }
        }

        private fun parseLongObjectSafe(str: String?): Long? {
            return try {
                str?.toLong()
            } catch (e: Exception) {
                null
            }
        }

        private fun parseBoolean(str: String?): Boolean {
            return try {
                str!!.toBoolean()
            } catch (e: Exception) {
                throw SAXException("XML parsing error", e)
            }
        }

        private fun appendTag(buffer: StringBuilder, tag: String, close: Boolean, vararg attrs: String?) {
            buffer.append('<').append(tag)
            var i = 0
            while (i < attrs.size - 1) {
                if (attrs[i + 1] != null) {
                    buffer.append(' ')
                        .append(escapeForXml(attrs[i]!!)).append("=\"")
                        .append(escapeForXml(attrs[i + 1]!!)).append('"')
                }
                i += 2
            }
            if (close) {
                buffer.append('/')
            }
            buffer.append(">\n")
        }

        private fun closeTag(buffer: StringBuilder, tag: String) {
            buffer.append("</").append(tag).append(">")
        }

        private fun appendTagWithContent(buffer: StringBuilder, tag: String, content: String?) {
            if (content != null) {
                buffer
                    .append('<').append(tag).append('>')
                    .append(escapeForXml(content))
                    .append("</").append(tag).append(">\n")
            }
        }

        private fun appendTagWithContent(buffer: StringBuilder, tag: String, content: Any?) {
            if (content != null) {
                appendTagWithContent(buffer, tag, content.toString())
            }
        }

        private fun escapeForXml(data: String): CharSequence {
            val buffer = StringBuilder()

            for (i in data.indices) {
                val ch = data[i]
                when (ch) {
                    '	', '\n' -> buffer.append(ch)
                    '&' -> buffer.append("&amp;")
                    '<' -> buffer.append("&lt;")
                    '>' -> buffer.append("&gt;")
                    '"' -> buffer.append("&quot;")
                    '\'' -> buffer.append("&apos;")
                    else -> {
                        if ((ch >= ' ' && ch <= '퟿') ||
                            (ch >= '฀' && ch <= '�')) {
                            buffer.append(ch)
                        }
                    }
                }
            }
            return buffer
        }

        private fun clear(buffer: StringBuilder) {
            buffer.delete(0, buffer.length)
        }

        private fun string(buffer: StringBuilder): String? =
            if (buffer.isNotEmpty()) buffer.toString() else null
    }

    private fun builder(): StringBuilder =
        StringBuilder("<?xml version='1.1' encoding='UTF-8'?>")

    override fun serialize(query: BookQuery): String {
        val buffer = builder()
        appendTag(buffer, "query", false,
            "limit", query.limit.toString(),
            "page", query.page.toString()
        )
        serialize(buffer, query.filter)
        closeTag(buffer, "query")
        return buffer.toString()
    }

    private fun serialize(buffer: StringBuilder, filter: Filter?) {
        when (filter) {
            is Filter.Empty -> {
                appendTag(buffer, "filter", true, "type", "empty")
            }
            is Filter.Not -> {
                appendTag(buffer, "not", false)
                serialize(buffer, filter.base)
                closeTag(buffer, "not")
            }
            is Filter.And -> {
                appendTag(buffer, "and", false)
                serialize(buffer, filter.first)
                serialize(buffer, filter.second)
                closeTag(buffer, "and")
            }
            is Filter.Or -> {
                appendTag(buffer, "or", false)
                serialize(buffer, filter.first)
                serialize(buffer, filter.second)
                closeTag(buffer, "or")
            }
            is Filter.ByAuthor -> {
                val author = filter.author
                appendTag(buffer, "filter", true,
                    "type", "author",
                    "displayName", author.displayName,
                    "sorkKey", author.sortKey
                )
            }
            is Filter.ByTag -> {
                val lst = mutableListOf<String>()
                var t: Tag? = filter.tag
                while (t != null) {
                    lst.add(0, t.Name)
                    t = t.parent
                }
                val params = arrayOfNulls<String>(lst.size * 2 + 2)
                var index = 0
                params[index++] = "type"
                params[index++] = "tag"
                var num = 0
                for (name in lst) {
                    params[index++] = "name$num"
                    params[index++] = name
                    num++
                }
                appendTag(buffer, "filter", true, *params)
            }
            is Filter.ByLabel -> {
                appendTag(buffer, "filter", true,
                    "type", "label",
                    "name", filter.label
                )
            }
            is Filter.BySeries -> {
                appendTag(buffer, "filter", true,
                    "type", "series",
                    "title", filter.series.getTitle()
                )
            }
            is Filter.ByPattern -> {
                appendTag(buffer, "filter", true,
                    "type", "pattern",
                    "pattern", filter.pattern
                )
            }
            is Filter.ByTitlePrefix -> {
                appendTag(buffer, "filter", true,
                    "type", "title-prefix",
                    "prefix", filter.prefix
                )
            }
            is Filter.HasBookmark -> {
                appendTag(buffer, "filter", true, "type", "has-bookmark")
            }
            is Filter.HasPhysicalFile -> {
                appendTag(buffer, "filter", true, "type", "has-physical-file")
            }
            else -> throw RuntimeException("Unsupported filter type: ${filter?.javaClass}")
        }
    }

    override fun deserializeBookQuery(xml: String): BookQuery? {
        return try {
            val deserializer = BookQueryDeserializer()
            Xml.parse(xml, deserializer)
            deserializer.query
        } catch (e: SAXException) {
            System.err.println(xml)
            e.printStackTrace()
            null
        }
    }

    override fun serialize(query: BookmarkQuery): String {
        val buffer = builder()
        appendTag(buffer, "query", false,
            "visible", query.visible.toString(),
            "limit", query.limit.toString(),
            "page", query.page.toString()
        )
        query.book?.let { serialize(buffer, it) }
        closeTag(buffer, "query")
        return buffer.toString()
    }

    override fun deserializeBookmarkQuery(xml: String, creator: BookCreator<out AbstractBook>): BookmarkQuery? {
        return try {
            val deserializer = BookmarkQueryDeserializer(creator)
            Xml.parse(xml, deserializer)
            deserializer.query
        } catch (e: SAXException) {
            System.err.println(xml)
            e.printStackTrace()
            null
        }
    }

    override fun serialize(book: AbstractBook): String {
        val buffer = builder()
        serialize(buffer, book)
        return buffer.toString()
    }

    private fun serialize(buffer: StringBuilder, book: AbstractBook) {
        appendTag(
            buffer, "entry", false,
            "xmlns:dc", XMLNamespaces.DublinCore,
            "xmlns:calibre", XMLNamespaces.CalibreMetadata
        )

        appendTagWithContent(buffer, "id", book.getId())
        appendTagWithContent(buffer, "title", book.getTitle())
        appendTagWithContent(buffer, "dc:language", book.language)
        appendTagWithContent(buffer, "dc:encoding", book.getEncodingNoDetection())

        for (uid in book.uids()) {
            appendTag(buffer, "dc:identifier", false, "scheme", uid.type)
            buffer.append(escapeForXml(uid.id))
            closeTag(buffer, "dc:identifier")
        }

        for (author in book.authors()) {
            appendTag(buffer, "author", false)
            appendTagWithContent(buffer, "uri", author.sortKey)
            appendTagWithContent(buffer, "name", author.displayName)
            closeTag(buffer, "author")
        }

        for (tag in book.tags()) {
            appendTag(
                buffer, "category", true,
                "term", tag.toString("/"),
                "label", tag.Name
            )
        }

        for (label in book.labels()) {
            appendTag(
                buffer, "label", true,
                "uid", label.uid,
                "name", label.name
            )
        }

        book.getSeriesInfo()?.let { seriesInfo ->
            appendTagWithContent(buffer, "calibre:series", seriesInfo.series.getTitle())
            seriesInfo.index?.let {
                appendTagWithContent(buffer, "calibre:series_index", it.toPlainString())
            }
        }

        if (book.hasBookmark) {
            appendTag(buffer, "has-bookmark", true)
        }

        appendTag(
            buffer, "link", true,
            "href", "file://${book.getPath()}",
            "type", "application/epub+zip",
            "rel", "http://opds-spec.org/acquisition"
        )

        book.getProgress()?.let { progress ->
            appendTag(
                buffer, "progress", true,
                "numerator", progress.numerator.toString(),
                "denominator", progress.denominator.toString()
            )
        }

        closeTag(buffer, "entry")
    }

    override fun <B : AbstractBook> deserializeBook(xml: String, creator: BookCreator<B>): B? {
        return try {
            val deserializer = BookDeserializer(creator)
            Xml.parse(xml, deserializer)
            deserializer.book
        } catch (e: SAXException) {
            System.err.println(xml)
            e.printStackTrace()
            null
        }
    }

    override fun serialize(bookmark: Bookmark): String {
        val buffer = builder()
        appendTag(
            buffer, "bookmark", false,
            "id", bookmark.id.toString(),
            "uid", bookmark.uid,
            "versionUid", bookmark.getVersionUid(),
            "visible", bookmark.isVisible.toString()
        )
        appendTag(
            buffer, "book", true,
            "id", bookmark.bookId.toString(),
            "title", bookmark.bookTitle
        )
        appendTagWithContent(buffer, "text", bookmark.getText())
        appendTagWithContent(buffer, "original-text", bookmark.getOriginalText())
        appendTag(
            buffer, "history", true,
            "ts-creation", timestampByDate(bookmark.getTimestamp(Bookmark.DateType.Creation)),
            "ts-modification", timestampByDate(bookmark.getTimestamp(Bookmark.DateType.Modification)),
            "ts-access", timestampByDate(bookmark.getTimestamp(Bookmark.DateType.Access)),
            "date-creation", formatDate(bookmark.getTimestamp(Bookmark.DateType.Creation)),
            "date-modification", formatDate(bookmark.getTimestamp(Bookmark.DateType.Modification)),
            "date-access", formatDate(bookmark.getTimestamp(Bookmark.DateType.Access))
        )
        appendTag(
            buffer, "start", true,
            "model", bookmark.modelId,
            "paragraph", bookmark.paragraphIndex.toString(),
            "element", bookmark.elementIndex.toString(),
            "char", bookmark.charIndex.toString()
        )
        val end = bookmark.getEnd()
        if (end != null) {
            appendTag(
                buffer, "end", true,
                "paragraph", end.paragraphIndex.toString(),
                "element", end.elementIndex.toString(),
                "char", end.charIndex.toString()
            )
        } else {
            appendTag(
                buffer, "end", true,
                "length", bookmark.length.toString()
            )
        }
        appendTag(
            buffer, "style", true,
            "id", bookmark.getStyleId().toString()
        )
        closeTag(buffer, "bookmark")
        return buffer.toString()
    }

    override fun deserializeBookmark(xml: String): Bookmark? {
        return try {
            val deserializer = BookmarkDeserializer()
            Xml.parse(xml, deserializer)
            deserializer.bookmark
        } catch (e: SAXException) {
            System.err.println(xml)
            e.printStackTrace()
            null
        }
    }

    override fun serialize(style: HighlightingStyle): String {
        val buffer = builder()
        val bgColor = style.getBackgroundColor()
        val fgColor = style.getForegroundColor()
        appendTag(buffer, "style", true,
            "id", style.id.toString(),
            "timestamp", style.lastUpdateTimestamp.toString(),
            "name", style.getNameOrNull(),
            "bg-color", bgColor?.intValue()?.toString() ?: "-1",
            "fg-color", fgColor?.intValue()?.toString() ?: "-1"
        )
        return buffer.toString()
    }

    override fun deserializeStyle(xml: String): HighlightingStyle? {
        return try {
            val deserializer = StyleDeserializer()
            Xml.parse(xml, deserializer)
            deserializer.style
        } catch (e: SAXException) {
            System.err.println(xml)
            e.printStackTrace()
            null
        }
    }

    private class BookDeserializer<B : AbstractBook>(
        private val bookCreator: BookCreator<B>
    ) : DefaultHandler() {

        private val title = StringBuilder()
        private val language = StringBuilder()
        private val encoding = StringBuilder()
        private val uid = StringBuilder()
        private val uidList = mutableListOf<UID>()
        private val authors = mutableListOf<Author>()
        private val tags = mutableListOf<Tag>()
        private val labels = mutableListOf<Label>()
        private val authorSortKey = StringBuilder()
        private val authorName = StringBuilder()
        private val seriesTitle = StringBuilder()
        private val seriesIndex = StringBuilder()

        private var state = State.READ_NOTHING
        private var id: Long = -1
        private var url: String? = null
        private var scheme: String? = null
        private var hasBookmark = false
        private var progress: RationalNumber? = null
        var book: B? = null
            private set

        override fun startDocument() {
            book = null
            id = -1
            url = null
            clear(title)
            clear(language)
            clear(encoding)
            clear(seriesTitle)
            clear(seriesIndex)
            clear(uid)
            uidList.clear()
            authors.clear()
            tags.clear()
            labels.clear()
            hasBookmark = false
            progress = null
            state = State.READ_NOTHING
        }

        override fun endDocument() {
            if (id == -1L) return
            book = bookCreator.createBook(id, url, string(title), string(encoding), string(language))
            for (author in authors) {
                book!!.addAuthorWithNoCheck(author)
            }
            for (tag in tags) {
                book!!.addTagWithNoCheck(tag)
            }
            for (label in labels) {
                book!!.addLabelWithNoCheck(label)
            }
            for (uidItem in uidList) {
                book!!.addUidWithNoCheck(uidItem)
            }
            book!!.setSeriesInfoWithNoCheck(string(seriesTitle), string(seriesIndex))
            book!!.setProgressWithNoCheck(progress)
            book!!.hasBookmark = hasBookmark
        }

        override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
            when (state) {
                State.READ_NOTHING -> {
                    if (localName != "entry") {
                        throw SAXException("Unexpected tag $localName")
                    }
                    state = State.READ_ENTRY
                }
                State.READ_ENTRY -> {
                    when (localName) {
                        "id" -> state = State.READ_ID
                        "title" -> state = State.READ_TITLE
                        "identifier" -> if (XMLNamespaces.DublinCore == uri) {
                            state = State.READ_UID
                            scheme = attributes.getValue("scheme")
                        }
                        "language" -> if (XMLNamespaces.DublinCore == uri) {
                            state = State.READ_LANGUAGE
                        }
                        "encoding" -> if (XMLNamespaces.DublinCore == uri) {
                            state = State.READ_ENCODING
                        }
                        "author" -> {
                            state = State.READ_AUTHOR
                            clear(authorName)
                            clear(authorSortKey)
                        }
                        "category" -> {
                            val term = attributes.getValue("term")
                            if (term != null) {
                                tags.add(Tag.getTag(term.split("/").toTypedArray()))
                            }
                        }
                        "label" -> {
                            val name = attributes.getValue("name")
                            if (name != null) {
                                val labelUid = attributes.getValue("uid")
                                if (labelUid != null) {
                                    labels.add(Label(labelUid, name))
                                } else {
                                    labels.add(Label(name))
                                }
                            }
                        }
                        "series" -> if (XMLNamespaces.CalibreMetadata == uri) {
                            state = State.READ_SERIES_TITLE
                        }
                        "series_index" -> if (XMLNamespaces.CalibreMetadata == uri) {
                            state = State.READ_SERIES_INDEX
                        }
                        "has-bookmark" -> hasBookmark = true
                        "link" -> url = attributes.getValue("href")
                        "progress" -> progress = RationalNumber.create(
                            parseLong(attributes.getValue("numerator")),
                            parseLong(attributes.getValue("denominator"))
                        )
                        else -> throw SAXException("Unexpected tag $localName")
                    }
                }
                State.READ_AUTHOR -> {
                    when (localName) {
                        "uri" -> state = State.READ_AUTHOR_URI
                        "name" -> state = State.READ_AUTHOR_NAME
                        else -> throw SAXException("Unexpected tag $localName")
                    }
                }
                else -> {}
            }
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            when (state) {
                State.READ_ENTRY -> {
                    if (localName == "entry") {
                        state = State.READ_NOTHING
                    }
                }
                State.READ_AUTHOR_URI, State.READ_AUTHOR_NAME -> {
                    state = State.READ_AUTHOR
                }
                State.READ_AUTHOR -> {
                    if (authorSortKey.isNotEmpty() && authorName.isNotEmpty()) {
                        Author.create(authorName.toString(), authorSortKey.toString())?.let {
                            authors.add(it)
                        }
                    }
                    state = State.READ_ENTRY
                }
                State.READ_UID -> {
                    uidList.add(UID(scheme ?: "", uid.toString()))
                    clear(uid)
                    state = State.READ_ENTRY
                }
                else -> state = State.READ_ENTRY
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            when (state) {
                State.READ_ID -> id = parseLongSafe(String(ch, start, length), -1)
                State.READ_TITLE -> title.append(ch, start, length)
                State.READ_UID -> uid.append(ch, start, length)
                State.READ_LANGUAGE -> language.append(ch, start, length)
                State.READ_ENCODING -> encoding.append(ch, start, length)
                State.READ_AUTHOR_URI -> authorSortKey.append(ch, start, length)
                State.READ_AUTHOR_NAME -> authorName.append(ch, start, length)
                State.READ_SERIES_TITLE -> seriesTitle.append(ch, start, length)
                State.READ_SERIES_INDEX -> seriesIndex.append(ch, start, length)
                else -> {}
            }
        }

        private enum class State {
            READ_NOTHING,
            READ_ENTRY,
            READ_ID,
            READ_UID,
            READ_TITLE,
            READ_LANGUAGE,
            READ_ENCODING,
            READ_AUTHOR,
            READ_AUTHOR_URI,
            READ_AUTHOR_NAME,
            READ_SERIES_TITLE,
            READ_SERIES_INDEX
        }
    }

    private class BookQueryDeserializer : DefaultHandler() {
        private val stateStack = mutableListOf<State>()
        private val filterStack = mutableListOf<Filter?>()
        private var filter: Filter? = null
        private var limit = -1
        private var page = -1
        var query: BookQuery? = null
            private set

        override fun startDocument() {
            stateStack.clear()
        }

        override fun endDocument() {
            if (filter != null && limit > 0 && page >= 0) {
                query = BookQuery(filter!!, limit, page)
            }
        }

        private fun setFilterToStack() {
            if (filterStack.isNotEmpty() && filterStack.last() == null) {
                filterStack[filterStack.size - 1] = filter
            }
        }

        override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
            if (stateStack.isEmpty()) {
                if (localName == "query") {
                    limit = parseInt(attributes.getValue("limit"))
                    page = parseInt(attributes.getValue("page"))
                    stateStack.add(State.READ_QUERY)
                } else {
                    throw SAXException("Unexpected tag $localName")
                }
            } else {
                when (localName) {
                    "filter" -> {
                        val type = attributes.getValue("type")
                        filter = when (type) {
                            "empty" -> Filter.Empty()
                            "author" -> Filter.ByAuthor(
                                Author.create(
                                    attributes.getValue("displayName"),
                                    attributes.getValue("sorkKey")
                                ) ?: Author.NULL
                            )
                            "tag" -> {
                                val names = mutableListOf<String>()
                                var num = 0
                                var n: String?
                                while (attributes.getValue("name$num").also { n = it } != null) {
                                    names.add(n!!)
                                    num++
                                }
                                Filter.ByTag(Tag.getTag(names.toTypedArray()))
                            }
                            "label" -> Filter.ByLabel(attributes.getValue("name"))
                            "series" -> Filter.BySeries(Series(attributes.getValue("title")))
                            "pattern" -> Filter.ByPattern(attributes.getValue("pattern"))
                            "title-prefix" -> Filter.ByTitlePrefix(attributes.getValue("prefix"))
                            "has-bookmark" -> Filter.HasBookmark()
                            "has-physical-file" -> Filter.HasPhysicalFile()
                            else -> Filter.Empty()
                        }
                        stateStack.add(State.READ_FILTER_SIMPLE)
                    }
                    "not" -> {
                        filterStack.add(null)
                        stateStack.add(State.READ_FILTER_NOT)
                    }
                    "and" -> {
                        filterStack.add(null)
                        stateStack.add(State.READ_FILTER_AND)
                    }
                    "or" -> {
                        filterStack.add(null)
                        stateStack.add(State.READ_FILTER_OR)
                    }
                    else -> throw SAXException("Unexpected tag $localName")
                }
            }
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            if (stateStack.isEmpty()) {
                throw SAXException("Unexpected end of tag $localName")
            }
            when (stateStack.removeAt(stateStack.size - 1)) {
                State.READ_QUERY -> {}
                State.READ_FILTER_NOT -> filter = Filter.Not(filterStack.removeAt(filterStack.size - 1) ?: Filter.Empty())
                State.READ_FILTER_AND -> filter = Filter.And(filterStack.removeAt(filterStack.size - 1) ?: Filter.Empty(), filter ?: Filter.Empty())
                State.READ_FILTER_OR -> filter = Filter.Or(filterStack.removeAt(filterStack.size - 1) ?: Filter.Empty(), filter ?: Filter.Empty())
                State.READ_FILTER_SIMPLE -> {}
            }
            setFilterToStack()
        }

        private enum class State {
            READ_QUERY,
            READ_FILTER_NOT,
            READ_FILTER_AND,
            READ_FILTER_OR,
            READ_FILTER_SIMPLE
        }
    }

    private class BookmarkQueryDeserializer(
        creator: BookCreator<out AbstractBook>
    ) : DefaultHandler() {

        private val bookDeserializer = BookDeserializer<AbstractBook>(creator as BookCreator<AbstractBook>)
        private var isVisible = false
        private var limit = 0
        private var page = 0
        var query: BookmarkQuery? = null
            private set

        override fun startDocument() {
            query = null
            bookDeserializer.startDocument()
        }

        override fun endDocument() {
            bookDeserializer.endDocument()
            query = BookmarkQuery(bookDeserializer.book, limit, isVisible, page)
        }

        override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
            if (localName == "query") {
                isVisible = parseBoolean(attributes.getValue("visible"))
                limit = parseInt(attributes.getValue("limit"))
                page = parseInt(attributes.getValue("page"))
            } else {
                bookDeserializer.startElement(uri, localName, qName, attributes)
            }
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            if (localName != "query") {
                bookDeserializer.endElement(uri, localName, qName)
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            bookDeserializer.characters(ch, start, length)
        }
    }

    private class BookmarkDeserializer : DefaultHandler() {

        private val text = StringBuilder()
        private var state = State.READ_NOTHING
        var bookmark: Bookmark? = null
            private set

        private var id: Long = -1
        private var uid: String? = null
        private var versionUid: String? = null
        private var bookId: Long = -1
        private var bookTitle: String? = null
        private var originalText: StringBuilder? = null
        private var creationTimestamp: Long? = null
        private var modificationTimestamp: Long? = null
        private var accessTimestamp: Long? = null
        private var modelId: String? = null
        private var startParagraphIndex = 0
        private var startElementIndex = 0
        private var startCharIndex = 0
        private var endParagraphIndex = -1
        private var endElementIndex = -1
        private var endCharIndex = -1
        private var isVisible = false
        private var style = 1

        override fun startDocument() {
            bookmark = null
            id = -1
            uid = null
            versionUid = null
            bookId = -1
            bookTitle = null
            clear(text)
            originalText = null
            creationTimestamp = null
            modificationTimestamp = null
            accessTimestamp = null
            modelId = null
            startParagraphIndex = 0
            startElementIndex = 0
            startCharIndex = 0
            endParagraphIndex = -1
            endElementIndex = -1
            endCharIndex = -1
            isVisible = false
            style = 1
            state = State.READ_NOTHING
        }

        override fun endDocument() {
            if (bookId == -1L) return
            bookmark = Bookmark(
                id, uid, versionUid,
                bookId, bookTitle, text.toString(),
                originalText?.toString(),
                creationTimestamp!!, modificationTimestamp, accessTimestamp,
                modelId,
                startParagraphIndex, startElementIndex, startCharIndex,
                endParagraphIndex, endElementIndex, endCharIndex,
                isVisible,
                style
            )
        }

        override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
            when (state) {
                State.READ_NOTHING -> {
                    if (localName != "bookmark") {
                        throw SAXException("Unexpected tag $localName")
                    }
                    id = parseLong(attributes.getValue("id"))
                    uid = attributes.getValue("uid")
                    versionUid = attributes.getValue("versionUid")
                    isVisible = parseBoolean(attributes.getValue("visible"))
                    state = State.READ_BOOKMARK
                }
                State.READ_BOOKMARK -> {
                    when (localName) {
                        "book" -> {
                            bookId = parseLong(attributes.getValue("id"))
                            bookTitle = attributes.getValue("title")
                        }
                        "text" -> state = State.READ_TEXT
                        "original-text" -> {
                            state = State.READ_ORIGINAL_TEXT
                            originalText = StringBuilder()
                        }
                        "history" -> {
                            if (attributes.getValue("ts-creation") != null) {
                                creationTimestamp = parseLong(attributes.getValue("ts-creation"))
                                modificationTimestamp = parseLongObjectSafe(attributes.getValue("ts-modification"))
                                accessTimestamp = parseLongObjectSafe(attributes.getValue("ts-access"))
                            } else {
                                creationTimestamp = parseDate(attributes.getValue("date-creation"))
                                modificationTimestamp = parseDateSafe(attributes.getValue("date-modification"))
                                accessTimestamp = parseDateSafe(attributes.getValue("date-access"))
                            }
                        }
                        "start" -> {
                            modelId = attributes.getValue("model")
                            startParagraphIndex = parseInt(attributes.getValue("paragraph"))
                            startElementIndex = parseInt(attributes.getValue("element"))
                            startCharIndex = parseInt(attributes.getValue("char"))
                        }
                        "end" -> {
                            val para = attributes.getValue("paragraph")
                            if (para != null) {
                                endParagraphIndex = parseInt(para)
                                endElementIndex = parseInt(attributes.getValue("element"))
                                endCharIndex = parseInt(attributes.getValue("char"))
                            } else {
                                endParagraphIndex = parseInt(attributes.getValue("length"))
                                endElementIndex = -1
                                endCharIndex = -1
                            }
                        }
                        "style" -> style = parseInt(attributes.getValue("id"))
                        else -> throw SAXException("Unexpected tag $localName")
                    }
                }
                State.READ_TEXT, State.READ_ORIGINAL_TEXT -> {
                    throw SAXException("Unexpected tag $localName")
                }
            }
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            when (state) {
                State.READ_NOTHING -> throw SAXException("Unexpected closing tag $localName")
                State.READ_BOOKMARK -> {
                    if (localName == "bookmark") {
                        state = State.READ_NOTHING
                    }
                }
                State.READ_TEXT, State.READ_ORIGINAL_TEXT -> state = State.READ_BOOKMARK
                else -> {}
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            when (state) {
                State.READ_TEXT -> text.append(ch, start, length)
                State.READ_ORIGINAL_TEXT -> originalText?.append(ch, start, length)
                else -> {}
            }
        }

        private enum class State {
            READ_NOTHING,
            READ_BOOKMARK,
            READ_TEXT,
            READ_ORIGINAL_TEXT
        }
    }

    private class StyleDeserializer : DefaultHandler() {
        var style: HighlightingStyle? = null
            private set

        override fun startDocument() {
            style = null
        }

        override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
            if (localName == "style") {
                val id = parseIntSafe(attributes.getValue("id"), -1)
                if (id != -1) {
                    val timestamp = parseLongSafe(attributes.getValue("timestamp"), 0L)
                    val bg = parseIntSafe(attributes.getValue("bg-color"), -1)
                    val fg = parseIntSafe(attributes.getValue("fg-color"), -1)
                    style = HighlightingStyle(
                        id, timestamp, attributes.getValue("name"),
                        if (bg != -1) ZLColor(bg) else null,
                        if (fg != -1) ZLColor(fg) else null
                    )
                }
            }
        }
    }
}
