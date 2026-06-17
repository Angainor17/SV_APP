package org.geometerplus.fbreader.formats.oeb

import org.geometerplus.zlibrary.core.constants.XMLNamespaces
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.xml.ZLStringMap
import org.geometerplus.zlibrary.core.xml.ZLXMLProcessor
import org.geometerplus.zlibrary.core.xml.ZLXMLReaderAdapter

import java.io.IOException

internal class OEBAnnotationReader : ZLXMLReaderAdapter(), XMLNamespaces {
    private val buffer = StringBuilder()
    private var readState = READ_NONE

    fun readAnnotation(file: ZLFile): String? {
        readState = READ_NONE
        buffer.delete(0, buffer.length)

        return try {
            ZLXMLProcessor.read(this, file, 512)
            val len = buffer.length
            if (len > 1) {
                if (buffer[len - 1] == '\n') {
                    buffer.delete(len - 1, len)
                }
                buffer.toString()
            } else {
                null
            }
        } catch (e: IOException) {
            null
        }
    }

    override fun processNamespaces(): Boolean = true

    override fun startElementHandler(tag: String, attributes: ZLStringMap): Boolean {
        var lowerTag = tag.lowercase()
        if (testTag(XMLNamespaces.DublinCore, "description", lowerTag) ||
            testTag(XMLNamespaces.DublinCoreLegacy, "description", lowerTag)) {
            readState = READ_DESCRIPTION
        } else if (readState == READ_DESCRIPTION) {
            // TODO: process tags
            buffer.append(" ")
        }
        return false
    }

    override fun characterDataHandler(data: CharArray, start: Int, len: Int) {
        if (readState == READ_DESCRIPTION) {
            buffer.append(String(data, start, len).trim())
        }
    }

    override fun endElementHandler(tag: String): Boolean {
        if (readState != READ_DESCRIPTION) {
            return false
        }
        val lowerTag = tag.lowercase()
        if (testTag(XMLNamespaces.DublinCore, "description", lowerTag) ||
            testTag(XMLNamespaces.DublinCoreLegacy, "description", lowerTag)) {
            return true
        }
        // TODO: process tags
        buffer.append(" ")
        return false
    }

    companion object {
        private const val READ_NONE = 0
        private const val READ_DESCRIPTION = 1
    }
}
