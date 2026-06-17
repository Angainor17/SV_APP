package org.geometerplus.zlibrary.core.util

import org.fbreader.util.ComparisonUtil
import java.util.Arrays
import java.util.TreeMap

class MimeType private constructor(
    val name: String?,
    private val myParameters: Map<String, String>?
) {

    fun clean(): MimeType {
        if (myParameters == null) {
            return this
        }
        return get(name)
    }

    fun getParameter(key: String): String? = myParameters?.get(key)

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is MimeType) {
            return false
        }
        return ComparisonUtil.equal(name, o.name) &&
                MiscUtil.mapsEquals(myParameters, o.myParameters)
    }

    fun weakEquals(type: MimeType): Boolean = ComparisonUtil.equal(name, type.name)

    override fun hashCode(): Int = ComparisonUtil.hashCode(name)

    override fun toString(): String {
        if (myParameters == null) {
            return name ?: ""
        }

        val buffer = StringBuilder(name)
        for ((key, value) in myParameters) {
            buffer.append(';')
            buffer.append(key)
            buffer.append('=')
            buffer.append(value)
        }
        return buffer.toString()
    }

    companion object {
        // MIME images
        const val IMAGE_PREFIX = "image/"

        @JvmField
        val NULL = MimeType(null, null)

        private val ourSimpleTypesMap = HashMap<String, MimeType>()

        // MIME types / application
        @JvmField val APP_ZIP = get("application/zip")
        @JvmField val APP_RAR = get("application/x-rar-compressed")
        // unofficial, http://en.wikipedia.org/wiki/EPUB
        @JvmField val APP_EPUB_ZIP = get("application/epub+zip")
        // unofficial, used by flibusta catalog
        @JvmField val APP_EPUB = get("application/epub")
        @JvmField val TYPES_EPUB = Arrays.asList(APP_EPUB_ZIP, APP_EPUB)
        @JvmField val APP_MOBIPOCKET = get("application/x-mobipocket-ebook")
        @JvmField val TYPES_MOBIPOCKET = listOf(APP_MOBIPOCKET)
        @JvmField val APP_FB2 = get("application/fb2")
        @JvmField val APP_XFB2 = get("application/x-fb2")
        @JvmField val APP_FICTIONBOOK = get("application/x-fictionbook")
        @JvmField val APP_FICTIONBOOK_XML = get("application/x-fictionbook+xml")
        @JvmField val APP_FB2_XML = get("application/fb2+xml")
        @JvmField val APP_PDF = get("application/pdf")
        @JvmField val APP_XPDF = get("application/x-pdf")
        @JvmField val TEXT_PDF = get("text/pdf")
        @JvmField val APP_VND_PDF = get("application/vnd.pdf")
        @JvmField val TYPES_PDF = Arrays.asList(APP_PDF, APP_XPDF, TEXT_PDF, APP_VND_PDF)
        @JvmField val APP_RTF = get("application/rtf")
        @JvmField val APP_TXT = get("application/txt")
        @JvmField val APP_DJVU = get("application/djvu")
        @JvmField val APP_HTML = get("application/html")
        @JvmField val APP_HTMLHTM = get("application/html+htm")
        @JvmField val APP_DOC = get("application/doc")
        @JvmField val APP_MSWORD = get("application/msword")
        @JvmField val TYPES_DOC = Arrays.asList(APP_MSWORD, APP_DOC)
        @JvmField val APP_FB2_ZIP = get("application/fb2+zip")
        @JvmField val TYPES_FB2_ZIP = listOf(APP_FB2_ZIP)
        @JvmField val APP_ATOM_XML = get("application/atom+xml")
        @JvmField val APP_ATOM_XML_ENTRY = get("application/atom+xml;type=entry")
        @JvmField val OPDS = get("application/atom+xml;profile=opds")
        @JvmField val APP_RSS_XML = get("application/rss+xml")
        @JvmField val APP_OPENSEARCHDESCRIPTION = get("application/opensearchdescription+xml")
        @JvmField val APP_LITRES = get("application/litres+xml")
        @JvmField val APP_CBZ = get("application/x-cbz")
        @JvmField val APP_CBR = get("application/x-cbr")
        @JvmField val TYPES_COMIC_BOOK = Arrays.asList(APP_CBZ, APP_CBR)
        @JvmField val TEXT_XML = get("text/xml")
        @JvmField val TEXT_HTML = get("text/html")
        @JvmField val TYPES_HTML = Arrays.asList(TEXT_HTML, APP_HTML, APP_HTMLHTM)
        @JvmField val TEXT_XHTML = get("text/xhtml")
        @JvmField val TEXT_PLAIN = get("text/plain")
        @JvmField val TYPES_TXT = Arrays.asList(TEXT_PLAIN, APP_TXT)
        @JvmField val TEXT_RTF = get("text/rtf")
        @JvmField val TYPES_RTF = Arrays.asList(APP_RTF, TEXT_RTF)
        @JvmField val TEXT_FB2 = get("text/fb2+xml")
        @JvmField val TYPES_FB2 = Arrays.asList(APP_FICTIONBOOK, APP_FICTIONBOOK_XML, APP_FB2, APP_XFB2, APP_FB2_XML, TEXT_FB2)
        @JvmField val IMAGE_PNG = get("image/png")
        @JvmField val IMAGE_JPEG = get("image/jpeg")
        @JvmField val IMAGE_AUTO = get("image/auto")
        @JvmField val IMAGE_PALM = get("image/palm")
        @JvmField val IMAGE_VND_DJVU = get("image/vnd.djvu")
        @JvmField val IMAGE_XDJVU = get("image/x-djvu")
        @JvmField val TYPES_DJVU = Arrays.asList(IMAGE_VND_DJVU, IMAGE_XDJVU, APP_DJVU)
        @JvmField val VIDEO_MP4 = get("video/mp4")
        @JvmField val VIDEO_WEBM = get("video/webm")
        @JvmField val VIDEO_OGG = get("video/ogg")
        @JvmField val TYPES_VIDEO = Arrays.asList(VIDEO_WEBM, VIDEO_OGG, VIDEO_MP4)
        @JvmField val UNKNOWN = get("*/*")

        @JvmStatic
        fun get(text: String?): MimeType {
            if (text == null) {
                return NULL
            }

            val items = text.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (items.isEmpty()) {
                return NULL
            }

            val name = items[0].intern()
            var parameters: MutableMap<String, String>? = null
            for (i in 1 until items.size) {
                val pair = items[i].split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (pair.size == 2) {
                    if (parameters == null) {
                        parameters = TreeMap()
                    }
                    parameters[pair[0].trim()] = pair[1].trim()
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
