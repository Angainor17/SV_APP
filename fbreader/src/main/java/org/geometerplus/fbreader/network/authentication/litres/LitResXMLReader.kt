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

package org.geometerplus.fbreader.network.authentication.litres

import org.geometerplus.fbreader.network.NetworkBookItem
import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.fbreader.network.atom.FormattedBuffer
import org.geometerplus.fbreader.network.opds.OPDSBookItem
import org.geometerplus.fbreader.network.opds.OPDSNetworkLink
import org.geometerplus.fbreader.network.urlInfo.BookUrlInfo
import org.geometerplus.fbreader.network.urlInfo.UrlInfo
import org.geometerplus.fbreader.network.urlInfo.UrlInfoCollection
import org.geometerplus.zlibrary.core.util.MimeType
import org.geometerplus.zlibrary.core.xml.ZLStringMap

internal class LitResXMLReader(private val library: NetworkLibrary, val link: OPDSNetworkLink) : LitResAuthenticationXMLReader() {

    val books = mutableListOf<NetworkBookItem>()
    private val urls = UrlInfoCollection<UrlInfo>()
    private val buffer = StringBuilder()
    private val annotationBuffer = FormattedBuffer(library, FormattedBuffer.Type.XHtml)
    private var index = 0
    private var bookId: String? = null
    private var title: String? = null
    private var seriesTitle: String? = null
    private var indexInSeries = 0
    private var summary: CharSequence? = null
    private var authorFirstName: String? = null
    private var authorMiddleName: String? = null
    private var authorLastName: String? = null
    private val authors = mutableListOf<NetworkBookItem.AuthorData>()
    private val tags = mutableListOf<String>()
    private var state = START

    override fun startElementHandler(tag: String, attributes: ZLStringMap): Boolean {
        val internedTag = tag.intern()

        when (state) {
            START -> {
                if (TAG_CATALOG == internedTag) {
                    state = CATALOG
                }
            }
            CATALOG -> {
                if (TAG_BOOK == internedTag) {
                    bookId = attributes.getValue("hub_id")
                    urls.addInfo(UrlInfo(
                        UrlInfo.Type.Image, attributes.getValue("cover"), MimeType.IMAGE_AUTO
                    ))

                    urls.addInfo(BookUrlInfo(
                        UrlInfo.Type.BookConditional,
                        "https://robot.litres.ru/pages/catalit_download_book/?art=$bookId",
                        MimeType.APP_FB2_ZIP
                    ))
                    state = BOOK
                }
            }
            BOOK -> {
                if (TAG_TEXT_DESCRIPTION == internedTag) {
                    state = BOOK_DESCRIPTION
                }
            }
            BOOK_DESCRIPTION -> {
                if (TAG_HIDDEN == internedTag) {
                    state = HIDDEN
                }
            }
            HIDDEN -> {
                if (TAG_TITLE_INFO == internedTag) {
                    state = TITLE_INFO
                }
            }
            TITLE_INFO -> {
                when (internedTag) {
                    TAG_GENRE -> state = GENRE
                    TAG_AUTHOR -> state = AUTHOR
                    TAG_BOOK_TITLE -> state = BOOK_TITLE
                    TAG_ANNOTATION -> state = ANNOTATION
                    TAG_DATE -> state = DATE
                    TAG_LANGUAGE -> state = LANGUAGE
                    TAG_SEQUENCE -> {
                        seriesTitle = attributes.getValue("name")
                        if (seriesTitle != null) {
                            indexInSeries = 0
                            val indexInSeriesStr = attributes.getValue("number")
                            if (indexInSeriesStr != null) {
                                try {
                                    indexInSeries = indexInSeriesStr.toInt()
                                } catch (e: NumberFormatException) {
                                }
                            }
                        }
                    }
                }
            }
            AUTHOR -> {
                when (internedTag) {
                    TAG_FIRST_NAME -> state = FIRST_NAME
                    TAG_MIDDLE_NAME -> state = MIDDLE_NAME
                    TAG_LAST_NAME -> state = LAST_NAME
                }
            }
            ANNOTATION -> {
                annotationBuffer.appendText(buffer.toString())
                annotationBuffer.appendStartTag(internedTag, attributes)
            }
        }

        buffer.delete(0, buffer.length)
        return false
    }

    override fun endElementHandler(tag: String): Boolean {
        val internedTag = tag.intern()

        when (state) {
            CATALOG -> {
                if (TAG_CATALOG == internedTag) {
                    state = START
                }
            }
            BOOK -> {
                if (TAG_BOOK == internedTag) {
                    urls.addInfo(UrlInfo(
                        UrlInfo.Type.SingleEntry,
                        "https://data.fbreader.org/catalogs/litres2/full.php5?id=$bookId",
                        MimeType.APP_ATOM_XML_ENTRY
                    ))
                    books.add(OPDSBookItem(
                        library,
                        link,
                        bookId!!,
                        index++,
                        title ?: "",
                        summary,
                        authors,
                        tags,
                        seriesTitle,
                        indexInSeries.toFloat(),
                        urls
                    ))

                    bookId = null
                    title = null
                    seriesTitle = null
                    summary = null
                    indexInSeries = 0
                    authors.clear()
                    tags.clear()
                    urls.clear()
                    state = CATALOG
                }
            }
            BOOK_DESCRIPTION -> {
                if (TAG_TEXT_DESCRIPTION == internedTag) {
                    state = BOOK
                }
            }
            HIDDEN -> {
                if (TAG_HIDDEN == internedTag) {
                    state = BOOK_DESCRIPTION
                }
            }
            TITLE_INFO -> {
                if (TAG_TITLE_INFO == internedTag) {
                    state = HIDDEN
                }
            }
            AUTHOR -> {
                if (TAG_AUTHOR == internedTag) {
                    val displayName = buildString {
                        authorFirstName?.let { append("$it ") }
                        authorMiddleName?.let { append("$it ") }
                        authorLastName?.let { append("$it ") }
                    }.trim()
                    authors.add(NetworkBookItem.AuthorData(displayName, authorLastName ?: ""))
                    authorFirstName = null
                    authorMiddleName = null
                    authorLastName = null
                    state = TITLE_INFO
                }
            }
            FIRST_NAME -> {
                if (TAG_FIRST_NAME == internedTag) {
                    authorFirstName = buffer.toString()
                    state = AUTHOR
                }
            }
            MIDDLE_NAME -> {
                if (TAG_MIDDLE_NAME == internedTag) {
                    authorMiddleName = buffer.toString()
                    state = AUTHOR
                }
            }
            LAST_NAME -> {
                if (TAG_LAST_NAME == internedTag) {
                    authorLastName = buffer.toString()
                    state = AUTHOR
                }
            }
            GENRE -> {
                if (TAG_GENRE == internedTag) {
                    state = TITLE_INFO
                }
            }
            BOOK_TITLE -> {
                if (TAG_BOOK_TITLE == internedTag) {
                    title = buffer.toString()
                    state = TITLE_INFO
                }
            }
            ANNOTATION -> {
                annotationBuffer.appendText(buffer.toString())
                if (TAG_ANNOTATION == internedTag) {
                    summary = annotationBuffer.getText()
                    annotationBuffer.reset()
                    state = TITLE_INFO
                } else {
                    annotationBuffer.appendEndTag(internedTag)
                }
            }
            DATE -> {
                if (TAG_DATE == internedTag) {
                    state = TITLE_INFO
                }
            }
            LANGUAGE -> {
                if (TAG_LANGUAGE == internedTag) {
                    state = TITLE_INFO
                }
            }
        }

        buffer.delete(0, buffer.length)
        return false
    }

    override fun characterDataHandler(data: CharArray, start: Int, length: Int) {
        buffer.append(data, start, length)
    }

    companion object {
        private const val START = 0
        private const val CATALOG = 1
        private const val BOOK = 2
        private const val BOOK_DESCRIPTION = 3
        private const val HIDDEN = 4
        private const val TITLE_INFO = 5
        private const val GENRE = 6
        private const val AUTHOR = 7
        private const val FIRST_NAME = 8
        private const val MIDDLE_NAME = 9
        private const val LAST_NAME = 10
        private const val BOOK_TITLE = 11
        private const val ANNOTATION = 12
        private const val DATE = 13
        private const val LANGUAGE = 14

        private const val TAG_CATALOG = "catalit-fb2-books"
        private const val TAG_BOOK = "fb2-book"
        private const val TAG_TEXT_DESCRIPTION = "text_description"
        private const val TAG_HIDDEN = "hidden"
        private const val TAG_TITLE_INFO = "title-info"
        private const val TAG_GENRE = "genre"
        private const val TAG_AUTHOR = "author"
        private const val TAG_FIRST_NAME = "first-name"
        private const val TAG_MIDDLE_NAME = "middle-name"
        private const val TAG_LAST_NAME = "last-name"
        private const val TAG_BOOK_TITLE = "book-title"
        private const val TAG_ANNOTATION = "annotation"
        private const val TAG_DATE = "date"
        private const val TAG_SEQUENCE = "sequence"
        private const val TAG_LANGUAGE = "lang"
    }
}
