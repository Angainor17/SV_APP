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

package org.geometerplus.zlibrary.core.xml

import org.geometerplus.zlibrary.core.filesystem.ZLFile
import java.io.IOException
import java.io.InputStream
import java.io.Reader

object ZLXMLProcessor {
    @JvmStatic
    fun getEntityMap(dtdList: List<String>): Map<String, CharArray> {
        return try {
            ZLXMLParser.getDTDMap(dtdList)
        } catch (e: IOException) {
            emptyMap()
        }
    }

    @Throws(IOException::class)
    @JvmStatic
    fun read(xmlReader: ZLXMLReader, stream: InputStream, bufferSize: Int) {
        val parser = ZLXMLParser(xmlReader, stream, bufferSize)
        try {
            xmlReader.startDocumentHandler()
            parser.doIt()
            xmlReader.endDocumentHandler()
        } finally {
            parser.finish()
        }
    }

    @Throws(IOException::class)
    @JvmStatic
    fun read(xmlReader: ZLXMLReader, reader: Reader, bufferSize: Int) {
        val parser = ZLXMLParser(xmlReader, reader, bufferSize)
        try {
            xmlReader.startDocumentHandler()
            parser.doIt()
            xmlReader.endDocumentHandler()
        } finally {
            parser.finish()
        }
    }

    @Throws(IOException::class)
    @JvmStatic
    fun read(xmlReader: ZLXMLReader, file: ZLFile) {
        read(xmlReader, file, 65536)
    }

    @Throws(IOException::class)
    @JvmStatic
    fun read(xmlReader: ZLXMLReader, file: ZLFile, bufferSize: Int) {
        val stream = file.getInputStream()!!
        try {
            read(xmlReader, stream, bufferSize)
        } finally {
            try {
                stream.close()
            } catch (e: IOException) {
            }
        }
    }
}
