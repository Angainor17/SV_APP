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

package org.geometerplus.zlibrary.core.util

import org.fbreader.util.ComparisonUtil
import java.util.HashMap
import java.util.TreeMap

class MimeType private constructor(
    @JvmField val Name: String?,
    private val myParameters: Map<String, String>?
) {

    fun clean(): MimeType = if (myParameters == null) this else get(Name)

    fun getParameter(key: String): String? = myParameters?.get(key)

    fun weakEquals(type: MimeType): Boolean = ComparisonUtil.equal(Name, type.Name)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is MimeType) {
            return false
        }
        return ComparisonUtil.equal(Name, other.Name) &&
            MiscUtil.mapsEquals(myParameters, other.myParameters)
    }

    override fun hashCode(): Int = ComparisonUtil.hashCode(Name)

    override fun toString(): String {
        val name = Name ?: return ""
        if (myParameters == null) {
            return name
        }
        val buffer = StringBuilder(name)
        for ((key, value) in myParameters) {
            buffer.append(';').append(key).append('=').append(value)
        }
        return buffer.toString()
    }

    companion object {
        // MIME images
        const val IMAGE_PREFIX = "image/"

        @JvmField
        val NULL: MimeType = MimeType(null, null)

        private val ourSimpleTypesMap = HashMap<String, MimeType>()

        // MIME types / application
        // ???
        @JvmField
        val APP_ZIP: MimeType = get("application/zip")

        @JvmField
        val APP_RAR: MimeType = get("application/x-rar-compressed")

        // unofficial, http://en.wikipedia.org/wiki/EPUB
        @JvmField
        val APP_EPUB_ZIP: MimeType = get("application/epub+zip")

        // unofficial, used by flibusta catalog
        @JvmField
        val APP_EPUB: MimeType = get("application/epub")

        @JvmField
        val TYPES_EPUB: List<MimeType> = listOf(APP_EPUB_ZIP, APP_EPUB)

        // ???
        @JvmField
        val APP_MOBIPOCKET: MimeType = get("application/x-mobipocket-ebook")

        @JvmField
        val TYPES_MOBIPOCKET: List<MimeType> = listOf(APP_MOBIPOCKET)

        // ???
        //@JvmField val APP_MOBI: MimeType = get("application/mobi")
        // unofficial, used by Calibre server
        @JvmField
        val APP_FB2: MimeType = get("application/fb2")

        // ???
        @JvmField
        val APP_XFB2: MimeType = get("application/x-fb2")

        // ???
        @JvmField
        val APP_FICTIONBOOK: MimeType = get("application/x-fictionbook")

        // ???
        @JvmField
        val APP_FICTIONBOOK_XML: MimeType = get("application/x-fictionbook+xml")

        // unofficial, used by FBReder book network
        @JvmField
        val APP_FB2_XML: MimeType = get("application/fb2+xml")

        // http://www.iana.org/assignments/media-types/application/index.html
        @JvmField
        val APP_PDF: MimeType = get("application/pdf")

        // ???
        @JvmField
        val APP_XPDF: MimeType = get("application/x-pdf")

        // ???
        @JvmField
        val TEXT_PDF: MimeType = get("text/pdf")

        // ???
        @JvmField
        val APP_VND_PDF: MimeType = get("application/vnd.pdf")

        @JvmField
        val TYPES_PDF: List<MimeType> = listOf(APP_PDF, APP_XPDF, TEXT_PDF, APP_VND_PDF)

        // http://www.iana.org/assignments/media-types/application/index.html
        @JvmField
        val APP_RTF: MimeType = get("application/rtf")

        // unofficial, used by flibusta catalog
        @JvmField
        val APP_TXT: MimeType = get("application/txt")

        // unofficial, used by flibusta catalog
        @JvmField
        val APP_DJVU: MimeType = get("application/djvu")

        // unofficial, used by flibusta catalog
        @JvmField
        val APP_HTML: MimeType = get("application/html")

        // unofficial, used by flibusta catalog
        @JvmField
        val APP_HTMLHTM: MimeType = get("application/html+htm")

        // unofficial, used by flibusta catalog
        @JvmField
        val APP_DOC: MimeType = get("application/doc")

        // http://www.iana.org/assignments/media-types/application/index.html
        @JvmField
        val APP_MSWORD: MimeType = get("application/msword")

        @JvmField
        val TYPES_DOC: List<MimeType> = listOf(APP_MSWORD, APP_DOC)

        // unofficial, used by data.fbreader.org LitRes catalog & FBReader book nework
        @JvmField
        val APP_FB2_ZIP: MimeType = get("application/fb2+zip")

        @JvmField
        val TYPES_FB2_ZIP: List<MimeType> = listOf(APP_FB2_ZIP)

        // http://www.iana.org/assignments/media-types/application/index.html
        @JvmField
        val APP_ATOM_XML: MimeType = get("application/atom+xml")

        @JvmField
        val APP_ATOM_XML_ENTRY: MimeType = get("application/atom+xml;type=entry")

        @JvmField
        val OPDS: MimeType = get("application/atom+xml;profile=opds")

        // http://tools.ietf.org/id/draft-nottingham-rss-media-type-00.txt
        @JvmField
        val APP_RSS_XML: MimeType = get("application/rss+xml")

        // ???
        @JvmField
        val APP_OPENSEARCHDESCRIPTION: MimeType = get("application/opensearchdescription+xml")

        // unofficial, used by data.fbreader.org LitRes catalog
        @JvmField
        val APP_LITRES: MimeType = get("application/litres+xml")

        // ???
        @JvmField
        val APP_CBZ: MimeType = get("application/x-cbz")

        @JvmField
        val APP_CBR: MimeType = get("application/x-cbr")

        @JvmField
        val TYPES_COMIC_BOOK: List<MimeType> = listOf(APP_CBZ, APP_CBR)

        // MIME types / text
        // ???
        @JvmField
        val TEXT_XML: MimeType = get("text/xml")

        // http://www.iana.org/assignments/media-types/text/index.html
        @JvmField
        val TEXT_HTML: MimeType = get("text/html")

        @JvmField
        val TYPES_HTML: List<MimeType> = listOf(TEXT_HTML, APP_HTML, APP_HTMLHTM)

        // ???
        @JvmField
        val TEXT_XHTML: MimeType = get("text/xhtml")

        // http://www.iana.org/assignments/media-types/text/index.html
        @JvmField
        val TEXT_PLAIN: MimeType = get("text/plain")

        @JvmField
        val TYPES_TXT: List<MimeType> = listOf(TEXT_PLAIN, APP_TXT)

        // http://www.iana.org/assignments/media-types/text/index.html
        @JvmField
        val TEXT_RTF: MimeType = get("text/rtf")

        @JvmField
        val TYPES_RTF: List<MimeType> = listOf(APP_RTF, TEXT_RTF)

        // unofficial, used by Calibre OPDS server
        @JvmField
        val TEXT_FB2: MimeType = get("text/fb2+xml")

        @JvmField
        val TYPES_FB2: List<MimeType> = listOf(
            APP_FICTIONBOOK, APP_FICTIONBOOK_XML, APP_FB2, APP_XFB2, APP_FB2_XML, TEXT_FB2
        )

        // http://www.iana.org/assignments/media-types/image/index.html
        @JvmField
        val IMAGE_PNG: MimeType = get("image/png")

        // http://www.iana.org/assignments/media-types/image/index.html
        @JvmField
        val IMAGE_JPEG: MimeType = get("image/jpeg")

        // ???
        @JvmField
        val IMAGE_AUTO: MimeType = get("image/auto")

        // ???
        @JvmField
        val IMAGE_PALM: MimeType = get("image/palm")

        // http://www.iana.org/assignments/media-types/image/index.html
        @JvmField
        val IMAGE_VND_DJVU: MimeType = get("image/vnd.djvu")

        // ???
        @JvmField
        val IMAGE_XDJVU: MimeType = get("image/x-djvu")

        @JvmField
        val TYPES_DJVU: List<MimeType> = listOf(IMAGE_VND_DJVU, IMAGE_XDJVU, APP_DJVU)

        // video
        @JvmField
        val VIDEO_MP4: MimeType = get("video/mp4")

        @JvmField
        val VIDEO_WEBM: MimeType = get("video/webm")

        @JvmField
        val VIDEO_OGG: MimeType = get("video/ogg")

        @JvmField
        val TYPES_VIDEO: List<MimeType> = listOf(VIDEO_WEBM, VIDEO_OGG, VIDEO_MP4)

        @JvmField
        val UNKNOWN: MimeType = get("*/*")

        @JvmStatic
        fun get(text: String?): MimeType {
            if (text == null) {
                return NULL
            }

            val items = text.split(";").dropLastWhile { it.isEmpty() }
            if (items.isEmpty()) {
                return NULL
            }

            val name = items[0].intern()
            var parameters: MutableMap<String, String>? = null
            for (i in 1 until items.size) {
                val pair = items[i].split("=").dropLastWhile { it.isEmpty() }
                if (pair.size == 2) {
                    if (parameters == null) {
                        parameters = TreeMap()
                    }
                    parameters!![pair[0].trim()] = pair[1].trim()
                }
            }

            if (parameters == null) {
                var type = ourSimpleTypesMap[name]
                if (type == null) {
                    type = MimeType(name, null)
                    ourSimpleTypesMap[name] = type
                }
                return type
            }

            return MimeType(name, parameters)
        }
    }
}
