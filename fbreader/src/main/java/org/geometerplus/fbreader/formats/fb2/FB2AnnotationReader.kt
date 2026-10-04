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

package org.geometerplus.fbreader.formats.fb2

import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.xml.ZLStringMap
import org.geometerplus.zlibrary.core.xml.ZLXMLProcessor
import org.geometerplus.zlibrary.core.xml.ZLXMLReaderAdapter
import java.io.IOException

class FB2AnnotationReader : ZLXMLReaderAdapter() {
    private val myBuffer = StringBuilder()
    private var myReadState = READ_NOTHING

    override fun dontCacheAttributeValues(): Boolean = true

    fun readAnnotation(file: ZLFile): String? {
        myReadState = READ_NOTHING
        myBuffer.delete(0, myBuffer.length)
        if (readDocument(file)) {
            val len = myBuffer.length
            if (len > 1) {
                if (myBuffer[len - 1] == '\n') {
                    myBuffer.delete(len - 1, len)
                }
                return myBuffer.toString()
            }
        }
        return null
    }

    override fun startElementHandler(tag: String, attributes: ZLStringMap): Boolean {
        if ("body".equals(tag, ignoreCase = true)) {
            return true
        } else if ("annotation".equals(tag, ignoreCase = true)) {
            myReadState = READ_ANNOTATION
        } else if (myReadState == READ_ANNOTATION) {
            // TODO: add tag to buffer
            myBuffer.append(" ")
        }
        return false
    }

    override fun endElementHandler(tag: String): Boolean {
        if (myReadState != READ_ANNOTATION) {
            return false
        }
        if ("annotation".equals(tag, ignoreCase = true)) {
            return true
        } else if ("p".equals(tag, ignoreCase = true)) {
            myBuffer.append("\n")
        } else {
            // TODO: add tag to buffer
            myBuffer.append(" ")
        }
        return false
    }

    override fun characterDataHandler(ch: CharArray, start: Int, length: Int) {
        if (myReadState == READ_ANNOTATION) {
            myBuffer.append(String(ch, start, length).trim())
        }
    }

    private fun readDocument(file: ZLFile): Boolean =
        try {
            ZLXMLProcessor.read(this, file, 512)
            true
        } catch (e: IOException) {
            false
        }

    companion object {
        private const val READ_NOTHING = 0
        private const val READ_ANNOTATION = 1
    }
}
