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
import java.io.StringReader
import java.util.Collections
import java.util.HashMap

abstract class ZLXMLReaderAdapter : ZLXMLReader {
    private var myNamespaceMap: Map<String, String> = Collections.emptyMap()

    fun readQuietly(file: ZLFile): Boolean =
        try {
            read(file)
            true
        } catch (e: IOException) {
            false
        }

    fun readQuietly(string: String): Boolean =
        try {
            read(string)
            true
        } catch (e: IOException) {
            false
        }

    @Throws(IOException::class)
    fun read(file: ZLFile) {
        ZLXMLProcessor.read(this, file)
    }

    @Throws(IOException::class)
    fun read(stream: InputStream) {
        ZLXMLProcessor.read(this, stream, 65536)
    }

    @Throws(IOException::class)
    fun read(string: String) {
        ZLXMLProcessor.read(this, StringReader(string), 65536)
    }

    @Throws(IOException::class)
    fun read(reader: Reader) {
        ZLXMLProcessor.read(this, reader, 65536)
    }

    override fun dontCacheAttributeValues(): Boolean = false

    override fun startElementHandler(tag: String, attributes: ZLStringMap): Boolean = false

    override fun endElementHandler(tag: String): Boolean = false

    override fun characterDataHandler(ch: CharArray, start: Int, length: Int) {}

    override fun characterDataHandlerFinal(ch: CharArray, start: Int, length: Int) {
        characterDataHandler(ch, start, length)
    }

    override fun startDocumentHandler() {}

    override fun endDocumentHandler() {}

    override fun processNamespaces(): Boolean = false

    override fun namespaceMapChangedHandler(namespaces: MutableMap<String, String>) {
        myNamespaceMap = namespaces
    }

    fun testTag(namespace: String, name: String, tag: String): Boolean {
        if (name == tag && namespace == myNamespaceMap.get("")) {
            return true
        }
        val nameLen = name.length
        val tagLen = tag.length
        if (tagLen < nameLen + 2) {
            return false
        }
        if (tag.endsWith(name) && tag[tagLen - nameLen - 1] == ':') {
            return namespace == myNamespaceMap.get(tag.substring(0, tagLen - nameLen - 1))
        }
        return false
    }

    fun getAttributeValue(attributes: ZLStringMap, namespace: String?, name: String): String? {
        if (namespace == null) {
            return attributes.getValue(name)
        }

        val size = attributes.getSize()
        if (size == 0) {
            return null
        }
        val postfix = ":" + name
        for (i in size - 1 downTo 0) {
            val key = attributes.getKey(i)
            if (key != null && key.endsWith(postfix)) {
                val nsKey = key.substring(0, key.length - postfix.length)
                if (namespace == myNamespaceMap.get(nsKey)) {
                    return attributes.getValue(i)
                }
            }
        }
        return null
    }

    protected fun getAttributeValue(attributes: ZLStringMap, predicate: Predicate, name: String): String? {
        val size = attributes.getSize()
        if (size == 0) {
            return null
        }
        val postfix = ":" + name
        for (i in size - 1 downTo 0) {
            val key = attributes.getKey(i)
            if (key != null && key.endsWith(postfix)) {
                val ns = myNamespaceMap.get(key.substring(0, key.length - postfix.length))
                if (ns != null && predicate.accepts(ns)) {
                    return attributes.getValue(i)
                }
            }
        }
        return null
    }

    override fun collectExternalEntities(entityMap: HashMap<String, CharArray>) {}

    override fun externalDTDs(): List<String> = Collections.emptyList()

    interface Predicate {
        fun accepts(namespace: String): Boolean
    }
}
