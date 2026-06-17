package org.geometerplus.fbreader.formats.fb2

import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.xml.ZLStringMap
import org.geometerplus.zlibrary.core.xml.ZLXMLProcessor
import org.geometerplus.zlibrary.core.xml.ZLXMLReaderAdapter

import java.io.IOException

class FB2AnnotationReader : ZLXMLReaderAdapter() {
    private val buffer = StringBuilder()
    private var readState = READ_NOTHING

    override fun dontCacheAttributeValues(): Boolean = true

    fun readAnnotation(file: ZLFile): String? {
        readState = READ_NOTHING
        buffer.delete(0, buffer.length)
        if (readDocument(file)) {
            val len = buffer.length
            if (len > 1) {
                if (buffer[len - 1] == '\n') {
                    buffer.delete(len - 1, len)
                }
                return buffer.toString()
            }
        }
        return null
    }

    override fun startElementHandler(tag: String, attributes: ZLStringMap): Boolean {
        if ("body".equals(tag, ignoreCase = true)) {
            return true
        } else if ("annotation".equals(tag, ignoreCase = true)) {
            readState = READ_ANNOTATION
        } else if (readState == READ_ANNOTATION) {
            // TODO: add tag to buffer
            buffer.append(" ")
        }
        return false
    }

    override fun endElementHandler(tag: String): Boolean {
        if (readState != READ_ANNOTATION) {
            return false
        }
        if ("annotation".equals(tag, ignoreCase = true)) {
            return true
        } else if ("p".equals(tag, ignoreCase = true)) {
            buffer.append("\n")
        } else {
            // TODO: add tag to buffer
            buffer.append(" ")
        }
        return false
    }

    override fun characterDataHandler(data: CharArray, start: Int, length: Int) {
        if (readState == READ_ANNOTATION) {
            buffer.append(String(data, start, length).trim())
        }
    }

    private fun readDocument(file: ZLFile): Boolean {
        return try {
            ZLXMLProcessor.read(this, file, 512)
            true
        } catch (e: IOException) {
            false
        }
    }

    companion object {
        private const val READ_NOTHING = 0
        private const val READ_ANNOTATION = 1
    }
}
