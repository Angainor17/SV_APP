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

import android.util.Xml
import org.geometerplus.zlibrary.core.constants.XMLNamespaces
import org.geometerplus.zlibrary.core.util.RationalNumber
import org.geometerplus.zlibrary.core.util.ZLColor
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import java.text.DateFormat
import java.util.ArrayList
import java.util.Date
import java.util.LinkedList
import java.util.Locale

private val ourDateFormatter: DateFormat =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.FULL, Locale.ENGLISH)

private fun timestampByDate(date: Long?): String? =
    if (date != null) date.toString() else null

private fun dateByTimestamp(str: String?): Date? {
    return try {
        if (str != null) Date(str.toLong()) else null
    } catch (e: Exception) {
        throw SAXException("XML parsing error", e)
    }
}

private fun formatDate(timestamp: Long?): String? =
    if (timestamp != null) ourDateFormatter.format(Date(timestamp)) else null

private fun parseDate(str: String?): Long {
    return try {
        ourDateFormatter.parse(str!!).time
    } catch (e: Exception) {
        throw SAXException("XML parsing error", e)
    }
}

private fun parseDateSafe(str: String?): Long? {
    return try {
        if (str != null) ourDateFormatter.parse(str).time else null
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
        str!!.toInt()
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
        str!!.toLong()
    } catch (e: Exception) {
        defaultValue
    }
}

private fun parseLongObjectSafe(str: String?): Long? {
    return try {
        str!!.toLong()
    } catch (e: Exception) {
        null
    }
}

private fun parseBoolean(str: String?): Boolean = str?.toBoolean() ?: false

private fun appendTag(buffer: StringBuilder, tag: String, close: Boolean, vararg attrs: String?) {
    buffer.append('<').append(tag)
    var i = 0
    while (i < attrs.size - 1) {
        val name = attrs[i]
        val value = attrs[i + 1]
        if (value != null) {
            buffer.append(' ')
                .append(escapeForXml(name!!)).append("=\"")
                .append(escapeForXml(value)).append('"')
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

    val len = data.length
    for (i in 0 until len) {
        val ch = data[i]
        when (ch) {
            '\u0009', '\n' -> buffer.append(ch)
            '&' -> buffer.append("&amp;")
            '<' -> buffer.append("&lt;")
            '>' -> buffer.append("&gt;")
            '"' -> buffer.append("&quot;")
            '\'' -> buffer.append("&apos;")
            else ->
                if ((ch >= '\u0020' && ch <= '\uD7FF') ||
                    (ch >= '\u0E00' && ch <= '\uFFFD')) {
                    buffer.append(ch)
                }
        }
    }

    return buffer
}

private fun clear(buffer: StringBuilder) {
    buffer.delete(0, buffer.length)
}

private fun string(buffer: StringBuilder): String? =
    if (buffer.length != 0) buffer.toString() else null

class XMLSerializer : AbstractSerializer() {

    private fun builder(): StringBuilder =
        StringBuilder("<?xml version='1.1' encoding='UTF-8'?>")

    override fun serialize(query: BookQuery): String {
        val buffer = builder()
        appendTag(buffer, "query", false,
            "limit", query.Limit.toString(),
            "page", query.Page.toString()
        )
        serialize(buffer, query.Filter)
        closeTag(buffer, "query")
        return buffer.toString()
    }

    private fun serialize(buffer: StringBuilder, filter: Filter) {
        when (filter) {
            is Filter.Empty ->
                appendTag(buffer, "filter", true,
                    "type", "empty"
                )
            is Filter.Not -> {
                appendTag(buffer, "not", false)
                serialize(buffer, filter.Base)
                closeTag(buffer, "not")
            }
            is Filter.And -> {
                appendTag(buffer, "and", false)
                serialize(buffer, filter.First)
                serialize(buffer, filter.Second)
                closeTag(buffer, "and")
            }
            is Filter.Or -> {
                appendTag(buffer, "or", false)
                serialize(buffer, filter.First)
                serialize(buffer, filter.Second)
                closeTag(buffer, "or")
            }
            is Filter.ByAuthor -> {
                val author = filter.Author
                appendTag(buffer, "filter", true,
                    "type", "author",
                    "displayName", author.DisplayName,
                    "sorkKey", author.SortKey
                )
            }
            is Filter.ByTag -> {
                val lst = LinkedList<String>()
                var t: Tag? = filter.Tag
                while (t != null) {
                    lst.add(0, t.Name)
                    t = t.Parent
                }
                val params = arrayOfNulls<String>(lst.size * 2 + 2)
                var index = 0
                params[index++] = "type"
                params[index++] = "tag"
                var num = 0
                for (name in lst) {
                    params[index++] = "name" + num++
                    params[index++] = name
                }
                appendTag(buffer, "filter", true, *params)
            }
            is Filter.ByLabel ->
                appendTag(buffer, "filter", true,
                    "type", "label",
                    "name", filter.Label
                )
            is Filter.BySeries ->
                appendTag(buffer, "filter", true,
                    "type", "series",
                    "title", filter.Series.getTitle()
                )
            is Filter.ByPattern ->
                appendTag(buffer, "filter", true,
                    "type", "pattern",
                    "pattern", filter.Pattern
                )
            is Filter.ByTitlePrefix ->
                appendTag(buffer, "filter", true,
                    "type", "title-prefix",
                    "prefix", filter.Prefix
                )
            is Filter.HasBookmark ->
                appendTag(buffer, "filter", true,
                    "type", "has-bookmark"
                )
            is Filter.HasPhysicalFile ->
                appendTag(buffer, "filter", true,
                    "type", "has-physical-file"
                )
            else ->
                throw RuntimeException("Unsupported filter type: " + filter.javaClass)
        }
    }

    override fun deserializeBookQuery(xml: String): BookQuery? {
        return try {
            val deserializer = BookQueryDeserializer()
            Xml.parse(xml, deserializer)
            deserializer.getQuery()
        } catch (e: SAXException) {
            System.err.println(xml)
            e.printStackTrace()
            null
        }
    }

    override fun serialize(query: BookmarkQuery): String {
        val buffer = builder()
        appendTag(buffer, "query", false,
            "visible", query.Visible.toString(),
            "limit", query.Limit.toString(),
            "page", query.Page.toString()
        )
        val book = query.Book
        if (book != null) {
            serialize(buffer, book)
        }
        closeTag(buffer, "query")
        return buffer.toString()
    }

    override fun deserializeBookmarkQuery(
        xml: String,
        creator: AbstractSerializer.BookCreator<out AbstractBook>,
    ): BookmarkQuery? {
        return try {
            val deserializer = BookmarkQueryDeserializer(creator)
            Xml.parse(xml, deserializer)
            deserializer.getQuery()
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
        appendTagWithContent(buffer, "dc:language", book.getLanguage())
        appendTagWithContent(buffer, "dc:encoding", book.getEncodingNoDetection())

        for (uid in book.uids()) {
            appendTag(
                buffer, "dc:identifier", false,
                "scheme", uid.type
            )
            buffer.append(escapeForXml(uid.id))
            closeTag(buffer, "dc:identifier")
        }

        for (author in book.authors()) {
            appendTag(buffer, "author", false)
            appendTagWithContent(buffer, "uri", author.SortKey)
            appendTagWithContent(buffer, "name", author.DisplayName)
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
                "uid", label.Uid,
                "name", label.Name
            )
        }

        val seriesInfo = book.getSeriesInfo()
        if (seriesInfo != null) {
            appendTagWithContent(buffer, "calibre:series", seriesInfo.Series.getTitle())
            seriesInfo.Index?.let {
                appendTagWithContent(buffer, "calibre:series_index", it.toPlainString())
            }
        }

        if (book.HasBookmark) {
            appendTag(buffer, "has-bookmark", true)
        }

        // TODO: serialize description (?)
        // TODO: serialize cover (?)

        appendTag(
            buffer, "link", true,
            "href", "file://" + book.getPath(),
            // TODO: real book mimetype
            "type", "application/epub+zip",
            "rel", "http://opds-spec.org/acquisition"
        )

        val progress = book.getProgress()
        if (progress != null) {
            appendTag(
                buffer, "progress", true,
                "numerator", progress.Numerator.toString(),
                "denominator", progress.Denominator.toString()
            )
        }

        closeTag(buffer, "entry")
    }

    override fun <B : AbstractBook> deserializeBook(
        xml: String,
        creator: AbstractSerializer.BookCreator<B>,
    ): B? {
        return try {
            val deserializer = BookDeserializer(creator)
            Xml.parse(xml, deserializer)
            deserializer.getBook()
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
            "id", bookmark.getId().toString(),
            "uid", bookmark.Uid,
            "versionUid", bookmark.getVersionUid(),
            "visible", bookmark.IsVisible.toString()
        )
        appendTag(
            buffer, "book", true,
            "id", bookmark.BookId.toString(),
            "title", bookmark.BookTitle
        )
        appendTagWithContent(buffer, "text", bookmark.getText())
        appendTagWithContent(buffer, "original-text", bookmark.getOriginalText())
        appendTag(
            buffer, "history", true,
            "ts-creation", timestampByDate(bookmark.getTimestamp(Bookmark.DateType.Creation)),
            "ts-modification", timestampByDate(bookmark.getTimestamp(Bookmark.DateType.Modification)),
            "ts-access", timestampByDate(bookmark.getTimestamp(Bookmark.DateType.Access)),
            // obsolete, old format plugins compatibility
            "date-creation", formatDate(bookmark.getTimestamp(Bookmark.DateType.Creation)),
            "date-modification", formatDate(bookmark.getTimestamp(Bookmark.DateType.Modification)),
            "date-access", formatDate(bookmark.getTimestamp(Bookmark.DateType.Access))
        )
        appendTag(
            buffer, "start", true,
            "model", bookmark.ModelId,
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
                "length", bookmark.getLength().toString()
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
            deserializer.getBookmark()
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
            "id", style.Id.toString(),
            "timestamp", style.LastUpdateTimestamp.toString(),
            "name", style.getNameOrNull(),
            "bg-color", if (bgColor != null) bgColor.intValue().toString() else "-1",
            "fg-color", if (fgColor != null) fgColor.intValue().toString() else "-1"
        )
        return buffer.toString()
    }

    override fun deserializeStyle(xml: String): HighlightingStyle? {
        return try {
            val deserializer = StyleDeserializer()
            Xml.parse(xml, deserializer)
            deserializer.getStyle()
        } catch (e: SAXException) {
            System.err.println(xml)
            e.printStackTrace()
            null
        }
    }

    private class BookDeserializer<B : AbstractBook>(
        private val myBookCreator: AbstractSerializer.BookCreator<B>,
    ) : DefaultHandler() {

        private val myTitle = StringBuilder()
        private val myLanguage = StringBuilder()
        private val myEncoding = StringBuilder()
        private val myUid = StringBuilder()
        private val myUidList = ArrayList<UID>()
        private val myAuthors = ArrayList<Author>()
        private val myTags = ArrayList<Tag>()
        private val myLabels = ArrayList<Label>()
        private val myAuthorSortKey = StringBuilder()
        private val myAuthorName = StringBuilder()
        private val mySeriesTitle = StringBuilder()
        private val mySeriesIndex = StringBuilder()
        private var myState = State.READ_NOTHING
        private var myId: Long = -1L
        private var myUrl: String? = null
        private var myScheme: String? = null
        private var myHasBookmark: Boolean = false
        private var myProgress: RationalNumber? = null
        private var myBook: B? = null

        fun getBook(): B? = if (myState == State.READ_NOTHING) myBook else null

        override fun startDocument() {
            myBook = null

            myId = -1L
            myUrl = null
            clear(myTitle)
            clear(myLanguage)
            clear(myEncoding)
            clear(mySeriesTitle)
            clear(mySeriesIndex)
            clear(myUid)
            myUidList.clear()
            myAuthors.clear()
            myTags.clear()
            myLabels.clear()
            myHasBookmark = false
            myProgress = null

            myState = State.READ_NOTHING
        }

        override fun endDocument() {
            if (myId == -1L) {
                return
            }
            val book = myBookCreator.createBook(
                myId, myUrl, string(myTitle), string(myEncoding), string(myLanguage),
            )
            for (author in myAuthors) {
                book.addAuthorWithNoCheck(author)
            }
            for (tag in myTags) {
                book.addTagWithNoCheck(tag)
            }
            for (label in myLabels) {
                book.addLabelWithNoCheck(label)
            }
            for (uid in myUidList) {
                book.addUidWithNoCheck(uid)
            }
            book.setSeriesInfoWithNoCheck(string(mySeriesTitle), string(mySeriesIndex))
            book.setProgressWithNoCheck(myProgress)
            book.HasBookmark = myHasBookmark
            myBook = book
        }

        override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
            when (myState) {
                State.READ_NOTHING -> {
                    if (localName != "entry") {
                        throw SAXException("Unexpected tag $localName")
                    }
                    myState = State.READ_ENTRY
                }
                State.READ_ENTRY -> {
                    when {
                        localName == "id" -> myState = State.READ_ID
                        localName == "title" -> myState = State.READ_TITLE
                        localName == "identifier" && XMLNamespaces.DublinCore == uri -> {
                            myState = State.READ_UID
                            myScheme = attributes.getValue("scheme")
                        }
                        localName == "language" && XMLNamespaces.DublinCore == uri ->
                            myState = State.READ_LANGUAGE
                        localName == "encoding" && XMLNamespaces.DublinCore == uri ->
                            myState = State.READ_ENCODING
                        localName == "author" -> {
                            myState = State.READ_AUTHOR
                            clear(myAuthorName)
                            clear(myAuthorSortKey)
                        }
                        localName == "category" -> {
                            val term = attributes.getValue("term")
                            if (term != null) {
                                val tag = Tag.getTag(term.split("/").toTypedArray())
                                if (tag != null) {
                                    myTags.add(tag)
                                }
                            }
                        }
                        localName == "label" -> {
                            val name = attributes.getValue("name")
                            if (name != null) {
                                val uid = attributes.getValue("uid")
                                if (uid != null) {
                                    myLabels.add(Label(uid, name))
                                } else {
                                    myLabels.add(Label(name))
                                }
                            }
                        }
                        localName == "series" && XMLNamespaces.CalibreMetadata == uri ->
                            myState = State.READ_SERIES_TITLE
                        localName == "series_index" && XMLNamespaces.CalibreMetadata == uri ->
                            myState = State.READ_SERIES_INDEX
                        localName == "has-bookmark" -> myHasBookmark = true
                        localName == "link" ->
                            // TODO: use "rel" attribute
                            myUrl = attributes.getValue("href")
                        localName == "progress" -> myProgress = RationalNumber.create(
                            parseLong(attributes.getValue("numerator")),
                            parseLong(attributes.getValue("denominator")),
                        )
                        else -> throw SAXException("Unexpected tag $localName")
                    }
                }
                State.READ_AUTHOR -> {
                    when (localName) {
                        "uri" -> myState = State.READ_AUTHOR_URI
                        "name" -> myState = State.READ_AUTHOR_NAME
                        else -> throw SAXException("Unexpected tag $localName")
                    }
                }
                else -> {}
            }
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            when (myState) {
                State.READ_NOTHING ->
                    throw SAXException("Unexpected closing tag $localName")
                State.READ_ENTRY ->
                    if (localName == "entry") {
                        myState = State.READ_NOTHING
                    }
                State.READ_AUTHOR_URI, State.READ_AUTHOR_NAME ->
                    myState = State.READ_AUTHOR
                State.READ_AUTHOR -> {
                    if (myAuthorSortKey.length > 0 && myAuthorName.length > 0) {
                        myAuthors.add(
                            Author(myAuthorName.toString(), myAuthorSortKey.toString())
                        )
                    }
                    myState = State.READ_ENTRY
                }
                State.READ_UID -> {
                    myUidList.add(UID(myScheme!!, myUid.toString()))
                    clear(myUid)
                    myState = State.READ_ENTRY
                }
                else ->
                    myState = State.READ_ENTRY
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            when (myState) {
                State.READ_ID ->
                    myId = parseLongSafe(String(ch, start, length), -1L)
                State.READ_TITLE ->
                    myTitle.append(ch, start, length)
                State.READ_UID ->
                    myUid.append(ch, start, length)
                State.READ_LANGUAGE ->
                    myLanguage.append(ch, start, length)
                State.READ_ENCODING ->
                    myEncoding.append(ch, start, length)
                State.READ_AUTHOR_URI ->
                    myAuthorSortKey.append(ch, start, length)
                State.READ_AUTHOR_NAME ->
                    myAuthorName.append(ch, start, length)
                State.READ_SERIES_TITLE ->
                    mySeriesTitle.append(ch, start, length)
                State.READ_SERIES_INDEX ->
                    mySeriesIndex.append(ch, start, length)
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
            READ_SERIES_INDEX,
        }
    }

    private class BookQueryDeserializer : DefaultHandler() {
        private val myStateStack = LinkedList<State>()
        private val myFilterStack = LinkedList<Filter?>()
        private var myFilter: Filter? = null
        private var myLimit: Int = -1
        private var myPage: Int = -1
        private var myQuery: BookQuery? = null

        fun getQuery(): BookQuery? = myQuery

        override fun startDocument() {
            myStateStack.clear()
        }

        override fun endDocument() {
            val filter = myFilter
            if (filter != null && myLimit > 0 && myPage >= 0) {
                myQuery = BookQuery(filter, myLimit, myPage)
            }
        }

        private fun setFilterToStack() {
            if (myFilterStack.isNotEmpty() && myFilterStack.last() == null) {
                myFilterStack[myFilterStack.size - 1] = myFilter
            }
        }

        override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
            if (myStateStack.isEmpty()) {
                if (localName == "query") {
                    myLimit = parseInt(attributes.getValue("limit"))
                    myPage = parseInt(attributes.getValue("page"))
                    myStateStack.add(State.READ_QUERY)
                } else {
                    throw SAXException("Unexpected tag $localName")
                }
            } else {
                if (localName == "filter") {
                    val type = attributes.getValue("type")
                    when (type) {
                        "empty" -> myFilter = Filter.Empty()
                        "author" -> myFilter = Filter.ByAuthor(
                            Author(
                                attributes.getValue("displayName")!!,
                                attributes.getValue("sorkKey")!!
                            )
                        )
                        "tag" -> {
                            val names = LinkedList<String>()
                            var num = 0
                            while (true) {
                                val n = attributes.getValue("name" + num++)
                                if (n == null) {
                                    break
                                }
                                names.add(n)
                            }
                            myFilter = Filter.ByTag(Tag.getTag(names.toTypedArray())!!)
                        }
                        "label" -> myFilter = Filter.ByLabel(attributes.getValue("name")!!)
                        "series" -> myFilter = Filter.BySeries(
                            Series(attributes.getValue("title")!!)
                        )
                        "pattern" -> myFilter = Filter.ByPattern(attributes.getValue("pattern"))
                        "title-prefix" -> myFilter = Filter.ByTitlePrefix(attributes.getValue("prefix"))
                        "has-bookmark" -> myFilter = Filter.HasBookmark()
                        "has-physical-file" -> myFilter = Filter.HasPhysicalFile()
                        else ->
                            // we create empty filter for all other types
                            // to keep a door to add new filters in a future
                            myFilter = Filter.Empty()
                    }
                    myStateStack.add(State.READ_FILTER_SIMPLE)
                } else if (localName == "not") {
                    myFilterStack.add(null)
                    myStateStack.add(State.READ_FILTER_NOT)
                } else if (localName == "and") {
                    myFilterStack.add(null)
                    myStateStack.add(State.READ_FILTER_AND)
                } else if (localName == "or") {
                    myFilterStack.add(null)
                    myStateStack.add(State.READ_FILTER_OR)
                } else {
                    throw SAXException("Unexpected tag $localName")
                }
            }
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            if (myStateStack.isEmpty()) {
                // should be never thrown
                throw SAXException("Unexpected end of tag $localName")
            }
            when (myStateStack.removeLast()) {
                State.READ_QUERY -> {}
                State.READ_FILTER_NOT ->
                    myFilter = Filter.Not(myFilterStack.removeLast()!!)
                State.READ_FILTER_AND ->
                    myFilter = Filter.And(myFilterStack.removeLast()!!, myFilter!!)
                State.READ_FILTER_OR ->
                    myFilter = Filter.Or(myFilterStack.removeLast()!!, myFilter!!)
                State.READ_FILTER_SIMPLE -> {}
            }
            setFilterToStack()
        }

        private enum class State {
            READ_QUERY,
            READ_FILTER_NOT,
            READ_FILTER_AND,
            READ_FILTER_OR,
            READ_FILTER_SIMPLE,
        }
    }

    private class BookmarkQueryDeserializer(
        creator: AbstractSerializer.BookCreator<out AbstractBook>,
    ) : DefaultHandler() {

        @Suppress("UNCHECKED_CAST")
        private val myBookDeserializer: BookDeserializer<AbstractBook> =
            BookDeserializer(creator as AbstractSerializer.BookCreator<AbstractBook>)

        private var myVisible: Boolean = false
        private var myLimit: Int = 0
        private var myPage: Int = 0
        private var myQuery: BookmarkQuery? = null

        fun getQuery(): BookmarkQuery? = myQuery

        override fun startDocument() {
            myQuery = null
            myBookDeserializer.startDocument()
        }

        override fun endDocument() {
            myBookDeserializer.endDocument()
            myQuery = BookmarkQuery(myBookDeserializer.getBook(), myVisible, myLimit, myPage)
        }

        override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
            if (localName == "query") {
                myVisible = parseBoolean(attributes.getValue("visible"))
                myLimit = parseInt(attributes.getValue("limit"))
                myPage = parseInt(attributes.getValue("page"))
            } else {
                myBookDeserializer.startElement(uri, localName, qName, attributes)
            }
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            if (localName != "query") {
                myBookDeserializer.endElement(uri, localName, qName)
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            myBookDeserializer.characters(ch, start, length)
        }
    }

    private class BookmarkDeserializer : DefaultHandler() {
        private val myText = StringBuilder()
        private var myState = State.READ_NOTHING
        private var myBookmark: Bookmark? = null

        private var myId: Long = -1L
        private var myUid: String? = null
        private var myVersionUid: String? = null
        private var myBookId: Long = -1L
        private var myBookTitle: String? = null
        private var myOriginalText: StringBuilder? = null
        private var myCreationTimestamp: Long? = null
        private var myModificationTimestamp: Long? = null
        private var myAccessTimestamp: Long? = null
        private var myModelId: String? = null
        private var myStartParagraphIndex: Int = 0
        private var myStartElementIndex: Int = 0
        private var myStartCharIndex: Int = 0
        private var myEndParagraphIndex: Int = -1
        private var myEndElementIndex: Int = -1
        private var myEndCharIndex: Int = -1
        private var myIsVisible: Boolean = false
        private var myStyle: Int = 1

        fun getBookmark(): Bookmark? = if (myState == State.READ_NOTHING) myBookmark else null

        override fun startDocument() {
            myBookmark = null

            myId = -1L
            myUid = null
            myVersionUid = null
            myBookId = -1L
            myBookTitle = null
            clear(myText)
            myOriginalText = null
            myCreationTimestamp = null
            myModificationTimestamp = null
            myAccessTimestamp = null
            myModelId = null
            myStartParagraphIndex = 0
            myStartElementIndex = 0
            myStartCharIndex = 0
            myEndParagraphIndex = -1
            myEndElementIndex = -1
            myEndCharIndex = -1
            myIsVisible = false
            myStyle = 1

            myState = State.READ_NOTHING
        }

        override fun endDocument() {
            if (myBookId == -1L) {
                return
            }
            myBookmark = Bookmark(
                myId, myUid, myVersionUid,
                myBookId, myBookTitle, myText.toString(),
                if (myOriginalText != null) myOriginalText.toString() else null,
                myCreationTimestamp!!, myModificationTimestamp, myAccessTimestamp,
                myModelId,
                myStartParagraphIndex, myStartElementIndex, myStartCharIndex,
                myEndParagraphIndex, myEndElementIndex, myEndCharIndex,
                myIsVisible,
                myStyle,
            )
        }

        override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
            when (myState) {
                State.READ_NOTHING -> {
                    if (localName != "bookmark") {
                        throw SAXException("Unexpected tag $localName")
                    }
                    myId = parseLong(attributes.getValue("id"))
                    myUid = attributes.getValue("uid")
                    myVersionUid = attributes.getValue("versionUid")
                    myIsVisible = parseBoolean(attributes.getValue("visible"))
                    myState = State.READ_BOOKMARK
                }
                State.READ_BOOKMARK -> {
                    when {
                        localName == "book" -> {
                            myBookId = parseLong(attributes.getValue("id"))
                            myBookTitle = attributes.getValue("title")
                        }
                        localName == "text" -> myState = State.READ_TEXT
                        localName == "original-text" -> {
                            myState = State.READ_ORIGINAL_TEXT
                            myOriginalText = StringBuilder()
                        }
                        localName == "history" -> {
                            if (attributes.getValue("ts-creation") != null) {
                                myCreationTimestamp = parseLong(attributes.getValue("ts-creation"))
                                myModificationTimestamp = parseLongObjectSafe(attributes.getValue("ts-modification"))
                                myAccessTimestamp = parseLongObjectSafe(attributes.getValue("ts-access"))
                            } else {
                                // obsolete, old format plugins compatibility
                                myCreationTimestamp = parseDate(attributes.getValue("date-creation"))
                                myModificationTimestamp = parseDateSafe(attributes.getValue("date-modification"))
                                myAccessTimestamp = parseDateSafe(attributes.getValue("date-access"))
                            }
                        }
                        localName == "start" -> {
                            myModelId = attributes.getValue("model")
                            myStartParagraphIndex = parseInt(attributes.getValue("paragraph"))
                            myStartElementIndex = parseInt(attributes.getValue("element"))
                            myStartCharIndex = parseInt(attributes.getValue("char"))
                        }
                        localName == "end" -> {
                            val para = attributes.getValue("paragraph")
                            if (para != null) {
                                myEndParagraphIndex = parseInt(para)
                                myEndElementIndex = parseInt(attributes.getValue("element"))
                                myEndCharIndex = parseInt(attributes.getValue("char"))
                            } else {
                                myEndParagraphIndex = parseInt(attributes.getValue("length"))
                                myEndElementIndex = -1
                                myEndCharIndex = -1
                            }
                        }
                        localName == "style" -> myStyle = parseInt(attributes.getValue("id"))
                        else -> throw SAXException("Unexpected tag $localName")
                    }
                }
                State.READ_TEXT, State.READ_ORIGINAL_TEXT ->
                    throw SAXException("Unexpected tag $localName")
            }
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            when (myState) {
                State.READ_NOTHING ->
                    throw SAXException("Unexpected closing tag $localName")
                State.READ_BOOKMARK ->
                    if (localName == "bookmark") {
                        myState = State.READ_NOTHING
                    }
                State.READ_TEXT, State.READ_ORIGINAL_TEXT ->
                    myState = State.READ_BOOKMARK
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            when (myState) {
                State.READ_TEXT ->
                    myText.append(ch, start, length)
                State.READ_ORIGINAL_TEXT ->
                    myOriginalText!!.append(ch, start, length)
                else -> {}
            }
        }

        private enum class State {
            READ_NOTHING,
            READ_BOOKMARK,
            READ_TEXT,
            READ_ORIGINAL_TEXT,
        }
    }

    private class StyleDeserializer : DefaultHandler() {
        private var myStyle: HighlightingStyle? = null

        fun getStyle(): HighlightingStyle? = myStyle

        override fun startDocument() {
            myStyle = null
        }

        override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
            if (localName == "style") {
                val id = parseIntSafe(attributes.getValue("id"), -1)
                if (id != -1) {
                    val timestamp = parseLongSafe(attributes.getValue("timestamp"), 0L)
                    val bg = parseIntSafe(attributes.getValue("bg-color"), -1)
                    val fg = parseIntSafe(attributes.getValue("fg-color"), -1)
                    myStyle = HighlightingStyle(
                        id, timestamp, attributes.getValue("name"),
                        if (bg != -1) ZLColor(bg) else null,
                        if (fg != -1) ZLColor(fg) else null,
                    )
                }
            }
        }
    }
}
