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

import org.geometerplus.zlibrary.core.filesystem.ZLResourceFile
import org.geometerplus.zlibrary.core.util.ZLArrayUtils
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.util.ArrayList
import java.util.HashMap
import java.util.LinkedList
import java.util.Queue

class ZLXMLParser {
    private val myXMLReader: ZLXMLReader
    private val myStreamReader: Reader
    private val myProcessNamespaces: Boolean
    private val myBuffer: CharArray
    private val myTagName = getMutableString()
    private val myCData = getMutableString()
    private val myAttributeName = getMutableString()
    private val myAttributeValue = getMutableString()
    private val myEntityName = getMutableString()
    private var myBufferDescriptionLength = 0

    @Throws(IOException::class)
    constructor(xmlReader: ZLXMLReader, reader: Reader, bufferSize: Int) {
        myXMLReader = xmlReader
        myProcessNamespaces = xmlReader.processNamespaces()
        myBuffer = getBuffer(bufferSize)
        myBufferDescriptionLength = 0
        myStreamReader = reader
    }

    @Throws(IOException::class)
    constructor(xmlReader: ZLXMLReader, stream: InputStream, bufferSize: Int) {
        myXMLReader = xmlReader
        myProcessNamespaces = xmlReader.processNamespaces()
        val buffer = getBuffer(bufferSize)
        myBuffer = buffer

        var encoding = "utf-8"
        var found = false
        var len = 0
        while (len < 256) {
            val c = stream.read().toChar()
            buffer[len++] = c
            if (c == '>') {
                found = true
                break
            }
        }
        myBufferDescriptionLength = len
        if (found) {
            val xmlDescription = String(buffer, 0, len).trim()
            if (xmlDescription.startsWith("<?xml") && xmlDescription.endsWith("?>")) {
                myBufferDescriptionLength = 0
                val index = xmlDescription.indexOf("encoding")
                if (index > 0) {
                    val startIndex = xmlDescription.indexOf('"', index)
                    if (startIndex > 0) {
                        val endIndex = xmlDescription.indexOf('"', startIndex + 1)
                        if (endIndex > 0) {
                            encoding = xmlDescription.substring(startIndex + 1, endIndex)
                        }
                    }
                }
            }
        }

        myStreamReader = InputStreamReader(stream, encoding)
    }

    fun finish() {
        storeBuffer(myBuffer)
        storeString(myTagName)
        storeString(myAttributeName)
        storeString(myAttributeValue)
        storeString(myEntityName)
    }

    fun doIt() {
        val xmlReader = myXMLReader
        val entityMap = getDTDMap(xmlReader.externalDTDs())
        xmlReader.collectExternalEntities(entityMap)
        val streamReader = myStreamReader
        val processNamespaces = myProcessNamespaces
        var oldNamespaceMap: HashMap<String, String>? = if (processNamespaces) HashMap() else null
        var currentNamespaceMap: HashMap<String, String>? = null
        val namespaceMapStack = ArrayList<HashMap<String, String>?>()
        val buffer = myBuffer
        val tagName = myTagName
        val cData = myCData
        val attributeName = myAttributeName
        val attributeValue = myAttributeValue
        val dontCacheAttributeValues = xmlReader.dontCacheAttributeValues()
        val entityName = myEntityName
        val strings = HashMap<ZLMutableString, String>()
        val attributes = ZLStringMap()
        var tagStack = arrayOfNulls<String>(10)
        var tagStackSize = 0

        var state = START_DOCUMENT
        var savedState = START_DOCUMENT
        while (true) {
            var count: Int
            if (myBufferDescriptionLength > 0) {
                count = myBufferDescriptionLength
                myBufferDescriptionLength = 0
            } else {
                count = streamReader.read(buffer)
            }
            if (count <= 0) {
                streamReader.close()
                return
            }
            var startPosition = 0
            if (count < buffer.size) {
                startPosition = buffer.size - count
                System.arraycopy(buffer, 0, buffer, startPosition, count)
                count = buffer.size
            }
            try {
                var i = startPosition - 1
                mainLoop@ while (true) {
                    when (state) {
                        START_DOCUMENT -> {
                            while (true) {
                                when (buffer[++i]) {
                                    '<' -> {
                                        state = LANGLE
                                        startPosition = i + 1
                                        continue@mainLoop
                                    }
                                }
                            }
                        }
                        LANGLE -> {
                            when (buffer[++i]) {
                                '/' -> {
                                    state = END_TAG
                                    startPosition = i + 1
                                }
                                '!' -> state = EXCL_TAG_START
                                '?' -> state = Q_TAG
                                else -> {
                                    state = START_TAG
                                    startPosition = i
                                }
                            }
                        }
                        EXCL_TAG_START -> {
                            when (buffer[++i]) {
                                '-' -> state = COMMENT
                                '[' -> {
                                    state = CDATA
                                    startPosition = i + 1
                                }
                                else -> state = EXCL_TAG
                            }
                        }
                        EXCL_TAG -> {
                            while (true) {
                                when (buffer[++i]) {
                                    '>' -> {
                                        state = TEXT
                                        startPosition = i + 1
                                        continue@mainLoop
                                    }
                                }
                            }
                        }
                        CDATA -> {
                            while (true) {
                                when (buffer[++i]) {
                                    ']' -> {
                                        state = END_OF_CDATA1
                                        continue@mainLoop
                                    }
                                }
                            }
                        }
                        END_OF_CDATA1 -> {
                            if (buffer[++i] == ']') {
                                state = END_OF_CDATA2
                            } else {
                                state = CDATA
                            }
                        }
                        END_OF_CDATA2 -> {
                            if (buffer[++i] == '>') {
                                cData.append(buffer, startPosition, i - startPosition)
                                val len = cData.myLength
                                if (len > 8) {
                                    val data = cData.myData
                                    if (String(data, 0, 6) == "CDATA[") {
                                        xmlReader.characterDataHandler(data, 6, len - 8)
                                    }
                                }
                                cData.clear()
                                state = TEXT
                                startPosition = i + 1
                            } else {
                                state = CDATA
                            }
                        }
                        COMMENT -> {
                            while (true) {
                                when (buffer[++i]) {
                                    '-' -> {
                                        state = END_OF_COMMENT1
                                        continue@mainLoop
                                    }
                                }
                            }
                        }
                        END_OF_COMMENT1 -> {
                            if (buffer[++i] == '-') {
                                state = END_OF_COMMENT2
                            } else {
                                state = COMMENT
                            }
                            continue@mainLoop
                        }
                        END_OF_COMMENT2 -> {
                            when (buffer[++i]) {
                                '>' -> {
                                    state = TEXT
                                    startPosition = i + 1
                                }
                                '-' -> {
                                }
                                else -> state = COMMENT
                            }
                        }
                        Q_TAG -> {
                            while (true) {
                                when (buffer[++i]) {
                                    '?' -> {
                                        state = END_OF_Q_TAG
                                        continue@mainLoop
                                    }
                                }
                            }
                        }
                        END_OF_Q_TAG -> {
                            if (buffer[++i] == '>') {
                                state = TEXT
                                startPosition = i + 1
                            } else {
                                state = Q_TAG
                            }
                            continue@mainLoop
                        }
                        START_TAG -> {
                            while (true) {
                                when (buffer[++i]) {
                                    '\b', '\t', '\n', '\u000B', '\u000C', '\r', ' ' -> {
                                        state = WS_AFTER_START_TAG_NAME
                                        tagName.append(buffer, startPosition, i - startPosition)
                                        continue@mainLoop
                                    }
                                    '>' -> {
                                        state = TEXT
                                        tagName.append(buffer, startPosition, i - startPosition)
                                        val stringTagName = convertToString(strings, tagName)
                                        if (tagStackSize == tagStack.size) {
                                            tagStack = ZLArrayUtils.createCopy(
                                                tagStack, tagStackSize, tagStackSize shl 1
                                            )
                                        }
                                        tagStack[tagStackSize++] = stringTagName
                                        if (processNamespaces) {
                                            if (currentNamespaceMap != null) {
                                                oldNamespaceMap = currentNamespaceMap
                                            }
                                            namespaceMapStack.add(currentNamespaceMap)
                                        }
                                        if (processStartTag(xmlReader, stringTagName, attributes, currentNamespaceMap)) {
                                            streamReader.close()
                                            return
                                        }
                                        currentNamespaceMap = null
                                        startPosition = i + 1
                                        continue@mainLoop
                                    }
                                    '/' -> {
                                        state = SLASH
                                        tagName.append(buffer, startPosition, i - startPosition)
                                        if (processFullTag(xmlReader, convertToString(strings, tagName), attributes)) {
                                            streamReader.close()
                                            return
                                        }
                                        currentNamespaceMap = null
                                        continue@mainLoop
                                    }
                                    '&' -> {
                                        savedState = START_TAG
                                        tagName.append(buffer, startPosition, i - startPosition)
                                        state = ENTITY_REF
                                        startPosition = i + 1
                                        continue@mainLoop
                                    }
                                }
                            }
                        }
                        WS_AFTER_START_TAG_NAME -> {
                            when (buffer[++i]) {
                                '>' -> {
                                    val stringTagName = convertToString(strings, tagName)
                                    if (tagStackSize == tagStack.size) {
                                        tagStack = ZLArrayUtils.createCopy(
                                            tagStack, tagStackSize, tagStackSize shl 1
                                        )
                                    }
                                    tagStack[tagStackSize++] = stringTagName
                                    if (processNamespaces) {
                                        if (currentNamespaceMap != null) {
                                            oldNamespaceMap = currentNamespaceMap
                                        }
                                        namespaceMapStack.add(currentNamespaceMap)
                                    }
                                    if (processStartTag(xmlReader, stringTagName, attributes, currentNamespaceMap)) {
                                        streamReader.close()
                                        return
                                    }
                                    currentNamespaceMap = null
                                    state = TEXT
                                    startPosition = i + 1
                                }
                                '/' -> {
                                    state = SLASH
                                    if (processFullTag(xmlReader, convertToString(strings, tagName), attributes)) {
                                        streamReader.close()
                                        return
                                    }
                                    currentNamespaceMap = null
                                }
                                '\b', '\t', '\n', '\u000B', '\u000C', '\r', ' ' -> {
                                }
                                else -> {
                                    state = ATTRIBUTE_NAME
                                    startPosition = i
                                }
                            }
                        }
                        ATTRIBUTE_NAME -> {
                            while (true) {
                                when (buffer[++i]) {
                                    '=' -> {
                                        attributeName.append(buffer, startPosition, i - startPosition)
                                        state = WAIT_ATTRIBUTE_VALUE
                                        continue@mainLoop
                                    }
                                    '&' -> {
                                        attributeName.append(buffer, startPosition, i - startPosition)
                                        savedState = ATTRIBUTE_NAME
                                        state = ENTITY_REF
                                        startPosition = i + 1
                                        continue@mainLoop
                                    }
                                    '\b', '\t', '\n', '\u000B', '\u000C', '\r', ' ' -> {
                                        attributeName.append(buffer, startPosition, i - startPosition)
                                        state = WAIT_EQUALS
                                        continue@mainLoop
                                    }
                                }
                            }
                        }
                        WAIT_EQUALS -> {
                            while (true) {
                                when (buffer[++i]) {
                                    '=' -> {
                                        state = WAIT_ATTRIBUTE_VALUE
                                        continue@mainLoop
                                    }
                                }
                            }
                        }
                        WAIT_ATTRIBUTE_VALUE -> {
                            while (true) {
                                when (buffer[++i]) {
                                    '"' -> {
                                        state = ATTRIBUTE_VALUE_QUOT
                                        startPosition = i + 1
                                        continue@mainLoop
                                    }
                                    '\'' -> {
                                        state = ATTRIBUTE_VALUE_APOS
                                        startPosition = i + 1
                                        continue@mainLoop
                                    }
                                }
                            }
                        }
                        WS_AFTER_ATTRIBUTE_VALUE -> {
                            when (buffer[++i]) {
                                '\b', '\t', '\n', '\u000B', '\u000C', '\r', ' ' ->
                                    state = WS_AFTER_START_TAG_NAME
                                '/', '>' -> {
                                    state = WS_AFTER_START_TAG_NAME
                                    --i
                                }
                                '"' -> {
                                    if (i != 0) {
                                        attributeValue.append(buffer, i - 1, 1)
                                    }
                                    continue@mainLoop
                                }
                                else -> {
                                    state = ATTRIBUTE_NAME
                                    continue@mainLoop
                                }
                            }
                            val aName = convertToString(strings, attributeName)
                            if (processNamespaces && aName == "xmlns") {
                                if (currentNamespaceMap == null) {
                                    currentNamespaceMap = HashMap(oldNamespaceMap!!)
                                }
                                currentNamespaceMap!![""] = attributeValue.toString()
                                attributeValue.clear()
                            } else if (processNamespaces && aName.startsWith("xmlns:")) {
                                if (currentNamespaceMap == null) {
                                    currentNamespaceMap = HashMap(oldNamespaceMap!!)
                                }
                                currentNamespaceMap!![aName.substring(6)] = attributeValue.toString()
                                attributeValue.clear()
                            } else if (dontCacheAttributeValues) {
                                attributes.put(aName, attributeValue.toString())
                                attributeValue.clear()
                            } else {
                                attributes.put(aName, convertToString(strings, attributeValue))
                            }
                        }
                        ATTRIBUTE_VALUE_QUOT -> {
                            while (true) {
                                when (buffer[++i]) {
                                    '"' -> {
                                        attributeValue.append(buffer, startPosition, i - startPosition)
                                        state = WS_AFTER_ATTRIBUTE_VALUE
                                        continue@mainLoop
                                    }
                                    '&' -> {
                                        attributeValue.append(buffer, startPosition, i - startPosition)
                                        savedState = ATTRIBUTE_VALUE_QUOT
                                        state = ENTITY_REF
                                        startPosition = i + 1
                                        continue@mainLoop
                                    }
                                }
                            }
                        }
                        ATTRIBUTE_VALUE_APOS -> {
                            while (true) {
                                when (buffer[++i]) {
                                    '\'' -> {
                                        attributeValue.append(buffer, startPosition, i - startPosition)
                                        state = WS_AFTER_ATTRIBUTE_VALUE
                                        continue@mainLoop
                                    }
                                    '&' -> {
                                        attributeValue.append(buffer, startPosition, i - startPosition)
                                        savedState = ATTRIBUTE_VALUE_APOS
                                        state = ENTITY_REF
                                        startPosition = i + 1
                                        continue@mainLoop
                                    }
                                }
                            }
                        }
                        ENTITY_REF -> {
                            while (true) {
                                when (buffer[++i]) {
                                    ';' -> {
                                        entityName.append(buffer, startPosition, i - startPosition)
                                        state = savedState
                                        startPosition = i + 1
                                        val value = getEntityValue(entityMap, convertToString(strings, entityName))
                                        if (value != null && value.isNotEmpty()) {
                                            when (state) {
                                                ATTRIBUTE_VALUE_QUOT, ATTRIBUTE_VALUE_APOS ->
                                                    attributeValue.append(value, 0, value.size)
                                                ATTRIBUTE_NAME ->
                                                    attributeName.append(value, 0, value.size)
                                                START_TAG ->
                                                    tagName.append(value, 0, value.size)
                                                TEXT ->
                                                    xmlReader.characterDataHandler(value, 0, value.size)
                                            }
                                        }
                                        continue@mainLoop
                                    }
                                }
                            }
                        }
                        SLASH -> {
                            while (true) {
                                when (buffer[++i]) {
                                    '>' -> {
                                        state = TEXT
                                        startPosition = i + 1
                                        continue@mainLoop
                                    }
                                }
                            }
                        }
                        END_TAG -> {
                            while (true) {
                                when (buffer[++i]) {
                                    '>' -> {
                                        if (tagStackSize > 0) {
                                            if (processNamespaces &&
                                                namespaceMapStack.removeAt(tagStackSize - 1) != null
                                            ) {
                                                for (j in (namespaceMapStack.size - 1) downTo 0) {
                                                    val element = namespaceMapStack[j]
                                                    if (element != null) {
                                                        oldNamespaceMap = element
                                                        currentNamespaceMap = oldNamespaceMap
                                                        break
                                                    }
                                                }
                                            }
                                            if (processEndTag(xmlReader, tagStack[--tagStackSize]!!, currentNamespaceMap)) {
                                                streamReader.close()
                                                return
                                            }
                                            currentNamespaceMap = null
                                        }
                                        state = TEXT
                                        startPosition = i + 1
                                        continue@mainLoop
                                    }
                                }
                            }
                        }
                        TEXT -> {
                            while (true) {
                                when (buffer[++i]) {
                                    '<' -> {
                                        if (i > startPosition) {
                                            xmlReader.characterDataHandlerFinal(buffer, startPosition, i - startPosition)
                                        }
                                        state = LANGLE
                                        continue@mainLoop
                                    }
                                    '&' -> {
                                        if (i > startPosition) {
                                            xmlReader.characterDataHandler(buffer, startPosition, i - startPosition)
                                        }
                                        savedState = TEXT
                                        state = ENTITY_REF
                                        startPosition = i + 1
                                        continue@mainLoop
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: ArrayIndexOutOfBoundsException) {
                if (count > startPosition) {
                    when (state) {
                        START_TAG -> tagName.append(buffer, startPosition, count - startPosition)
                        ATTRIBUTE_NAME -> attributeName.append(buffer, startPosition, count - startPosition)
                        ATTRIBUTE_VALUE_QUOT, ATTRIBUTE_VALUE_APOS ->
                            attributeValue.append(buffer, startPosition, count - startPosition)
                        ENTITY_REF -> entityName.append(buffer, startPosition, count - startPosition)
                        CDATA, END_OF_CDATA1, END_OF_CDATA2 ->
                            cData.append(buffer, startPosition, count - startPosition)
                        TEXT -> xmlReader.characterDataHandler(buffer, startPosition, count - startPosition)
                    }
                }
            }
        }
    }

    companion object {
        private const val START_DOCUMENT = 0
        private const val START_TAG = 1
        private const val END_TAG = 2
        private const val TEXT = 3
        //private const val IGNORABLE_WHITESPACE = 4
        //private const val PROCESSING_INSTRUCTION = 5
        private const val COMMENT = 6 // tag of form <!-- -->
        private const val END_OF_COMMENT1 = 7
        private const val END_OF_COMMENT2 = 8
        private const val EXCL_TAG = 9 // tag of form <! >
        private const val EXCL_TAG_START = 10
        private const val Q_TAG = 11 // tag of form <? ?>
        private const val END_OF_Q_TAG = 12
        private const val LANGLE = 13
        private const val WS_AFTER_START_TAG_NAME = 14
        private const val WS_AFTER_ATTRIBUTE_VALUE = 15
        //private const val WS_AFTER_END_TAG_NAME = 16
        private const val WAIT_EQUALS = 17
        private const val WAIT_ATTRIBUTE_VALUE = 18
        private const val SLASH = 19
        private const val ATTRIBUTE_NAME = 20
        private const val ATTRIBUTE_VALUE_QUOT = 21
        private const val ATTRIBUTE_VALUE_APOS = 22
        private const val ENTITY_REF = 23
        private const val CDATA = 24 // <![CDATA[...]]>
        private const val END_OF_CDATA1 = 25
        private const val END_OF_CDATA2 = 26

        private val ourBufferPool = HashMap<Int, Queue<CharArray>>()
        private val ourStringPool = LinkedList<ZLMutableString>()
        private val ourDTDMaps = HashMap<List<String>, HashMap<String, CharArray>>()

        private fun convertToString(
            strings: MutableMap<ZLMutableString, String>,
            container: ZLMutableString,
        ): String {
            val cached = strings[container]
            if (cached != null) {
                container.clear()
                return cached
            }
            val s = container.toString()
            strings[ZLMutableString(container)] = s
            container.clear()
            return s
        }

        @Synchronized
        private fun getBuffer(bufferSize: Int): CharArray {
            val queue = ourBufferPool[bufferSize]
            if (queue != null) {
                val buffer = queue.poll()
                if (buffer != null) {
                    return buffer
                }
            }
            return CharArray(bufferSize)
        }

        @Synchronized
        private fun storeBuffer(buffer: CharArray) {
            val existing = ourBufferPool[buffer.size]
            if (existing != null) {
                existing.add(buffer)
            } else {
                val queue = LinkedList<CharArray>()
                queue.add(buffer)
                ourBufferPool[buffer.size] = queue
            }
        }

        @Synchronized
        private fun getMutableString(): ZLMutableString {
            val string = ourStringPool.poll()
            return string ?: ZLMutableString()
        }

        @Synchronized
        private fun storeString(string: ZLMutableString) {
            ourStringPool.add(string)
        }

        private fun getEntityValue(entityMap: HashMap<String, CharArray>, name: String): CharArray? {
            val cached = entityMap[name]
            if (cached != null) {
                return cached
            }
            if (name.isNotEmpty() && name[0] == '#') {
                try {
                    val number = if (name[1] == 'x') name.substring(2).toInt(16) else name.substring(1).toInt()
                    val value = charArrayOf(number.toChar())
                    entityMap[name] = value
                    return value
                } catch (e: NumberFormatException) {
                }
            }
            return null
        }

        @Synchronized
        @JvmStatic
        @Throws(IOException::class)
        fun getDTDMap(dtdList: List<String>): HashMap<String, CharArray> {
            ourDTDMaps[dtdList]?.let { return it }
            val entityMap = HashMap<String, CharArray>()
            entityMap["amp"] = charArrayOf('&')
            entityMap["apos"] = charArrayOf('\'')
            entityMap["gt"] = charArrayOf('>')
            entityMap["lt"] = charArrayOf('<')
            entityMap["quot"] = charArrayOf('"')
            for (fileName in dtdList) {
                val stream = ZLResourceFile.createResourceFile(fileName).getInputStream()
                if (stream != null) {
                    ZLDTDParser().doIt(stream, entityMap)
                }
            }
            ourDTDMaps[dtdList] = entityMap
            return entityMap
        }

        private fun processFullTag(xmlReader: ZLXMLReader, tagName: String, attributes: ZLStringMap): Boolean {
            if (xmlReader.startElementHandler(tagName, attributes)) {
                return true
            }
            if (xmlReader.endElementHandler(tagName)) {
                return true
            }
            attributes.clear()
            return false
        }

        private fun processStartTag(
            xmlReader: ZLXMLReader,
            tagName: String,
            attributes: ZLStringMap,
            currentNamespaceMap: HashMap<String, String>?,
        ): Boolean {
            if (currentNamespaceMap != null) {
                xmlReader.namespaceMapChangedHandler(currentNamespaceMap)
            }
            if (xmlReader.startElementHandler(tagName, attributes)) {
                return true
            }
            attributes.clear()
            return false
        }

        private fun processEndTag(
            xmlReader: ZLXMLReader,
            tagName: String,
            currentNamespaceMap: HashMap<String, String>?,
        ): Boolean {
            val result = xmlReader.endElementHandler(tagName)
            if (currentNamespaceMap != null) {
                xmlReader.namespaceMapChangedHandler(currentNamespaceMap)
            }
            return result
        }
    }
}
