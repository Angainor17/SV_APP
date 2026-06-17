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

package org.geometerplus.fbreader.network.rss

import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.fbreader.network.atom.ATOMFeedHandler
import org.geometerplus.fbreader.network.atom.ATOMId
import org.geometerplus.fbreader.network.atom.FormattedBuffer
import org.geometerplus.zlibrary.core.xml.ZLStringMap
import org.geometerplus.zlibrary.core.xml.ZLXMLReaderAdapter

open class RSSXMLReader<MetadataType : RSSChannelMetadata, EntryType : RSSItem>(
    library: NetworkLibrary,
    private val feedHandler: ATOMFeedHandler<MetadataType, EntryType>,
    readEntryNotFeed: Boolean
) : ZLXMLReaderAdapter() {

    protected open val formattedBuffer = FormattedBuffer(library)
    private val buffer = StringBuilder()
    protected var state: Int = START
    private var namespaceMap: Map<String, String>? = null
    private var item: EntryType? = null
    private var author: RSSAuthor? = null
    private var category: RSSCategory? = null
    private var id: ATOMId? = null

    companion object {
        protected const val RSS = 1
        protected const val CHANNEL = 2
        protected const val C_TITLE = 3
        protected const val C_LINK = 4
        protected const val ITEM = 5
        protected const val TITLE = 6
        protected const val LINK = 7
        protected const val COMMENTS = 8
        protected const val PUBDATE = 9
        protected const val CATEGORY = 10
        protected const val GUID = 11
        protected const val DESCRIPTION = 12
        protected const val CONTENT = 13
        protected const val COMMENTS_RSS = 14

        protected const val TAG_RSS = "rss"
        protected const val TAG_CHANNEL = "channel"
        protected const val TAG_ITEM = "item"
        protected const val TAG_TITLE = "title"
        protected const val TAG_CATEGORY = "category"
        protected const val TAG_LINK = "link"
        protected const val TAG_GUID = "guid"
        protected const val TAG_DESCRIPTION = "description"
        protected const val TAG_PUBDATE = "pubDate"

        private const val START = 0
    }

    override fun startElementHandler(tag: String, attributes: ZLStringMap): Boolean {
        val index = tag.indexOf(':')
        val tagPrefix: String
        val localTag: String
        if (index != -1) {
            tagPrefix = tag.substring(0, index).intern()
            localTag = tag.substring(index + 1).intern()
        } else {
            tagPrefix = ""
            localTag = tag.intern()
        }

        return startElementHandler(getNamespace(tagPrefix), localTag, attributes, extractBufferContent())
    }

    override fun endElementHandler(tag: String): Boolean {
        val index = tag.indexOf(':')
        val tagPrefix: String
        val localTag: String
        if (index != -1) {
            tagPrefix = tag.substring(0, index).intern()
            localTag = tag.substring(index + 1).intern()
        } else {
            tagPrefix = ""
            localTag = tag.intern()
        }
        return endElementHandler(getNamespace(tagPrefix), localTag, extractBufferContent())
    }

    override fun characterDataHandler(data: CharArray, start: Int, length: Int) {
        buffer.append(data, start, length)
    }

    open fun startElementHandler(ns: String?, tag: String, attributes: ZLStringMap, bufferContent: String?): Boolean {
        when (state) {
            START -> {
                if (testTag(TAG_RSS, tag, ns, null)) {
                    state = RSS
                }
            }
            RSS -> {
                if (testTag(TAG_CHANNEL, tag, ns, null)) {
                    state = CHANNEL
                }
            }
            CHANNEL -> {
                when {
                    testTag(TAG_TITLE, tag, ns, null) -> state = C_TITLE
                    testTag(TAG_LINK, tag, ns, null) -> state = C_LINK
                    testTag(TAG_ITEM, tag, ns, null) -> {
                        item = feedHandler.createEntry(attributes)
                        state = ITEM
                    }
                }
            }
            ITEM -> {
                when {
                    testTag(TAG_TITLE, tag, ns, null) -> {
                        author = RSSAuthor(attributes)
                        state = TITLE
                    }
                    testTag(TAG_LINK, tag, ns, null) -> state = LINK
                    testTag(TAG_DESCRIPTION, tag, ns, null) -> state = DESCRIPTION
                    testTag(TAG_CATEGORY, tag, ns, null) -> state = CATEGORY
                    testTag(TAG_GUID, tag, ns, null) -> {
                        id = ATOMId()
                        state = GUID
                    }
                    testTag(TAG_PUBDATE, tag, ns, null) -> state = PUBDATE
                }
            }
        }
        return false
    }

    open fun endElementHandler(ns: String?, tag: String, bufferContent: String?): Boolean {
        when (state) {
            RSS -> {
                if (testTag(TAG_RSS, tag, ns, null)) {
                    state = START
                }
            }
            CHANNEL -> {
                if (testTag(TAG_CHANNEL, tag, ns, null)) {
                    state = RSS
                }
            }
            C_TITLE -> {
                if (testTag(TAG_TITLE, tag, ns, null)) {
                    state = CHANNEL
                }
            }
            C_LINK -> {
                if (testTag(TAG_LINK, tag, ns, null)) {
                    state = CHANNEL
                }
            }
            ITEM -> {
                if (testTag(TAG_ITEM, tag, ns, null)) {
                    item?.let { feedHandler.processFeedEntry(it) }
                    state = CHANNEL
                }
            }
            TITLE -> {
                if (testTag(TAG_TITLE, tag, ns, null)) {
                    parseTitle(bufferContent)
                    state = ITEM
                }
            }
            GUID -> {
                if (testTag(TAG_GUID, tag, ns, null)) {
                    id?.uri = bufferContent
                    item?.id = id
                    id = null
                    state = ITEM
                }
            }
            DESCRIPTION -> {
                if (testTag(TAG_DESCRIPTION, tag, ns, null)) {
                    formattedBuffer.reset(FormattedBuffer.Type.Html)
                    formattedBuffer.appendText(makeFormat(bufferContent))
                    item?.summary = formattedBuffer.getText()
                    state = ITEM
                }
            }
            CATEGORY -> {
                if (testTag(TAG_CATEGORY, tag, ns, null)) {
                    bufferContent?.split(", ")?.forEach { str ->
                        val source = ZLStringMap()
                        source.put(RSSCategory.LABEL, str)
                        category = RSSCategory(source)
                        category?.let { item?.categories?.add(it) }
                        category = null
                    }
                    state = ITEM
                }
            }
            PUBDATE -> {
                if (testTag(TAG_PUBDATE, tag, ns, null)) {
                    state = ITEM
                }
            }
            LINK -> {
                if (testTag(TAG_LINK, tag, ns, null)) {
                    state = ITEM
                }
            }
        }
        return false
    }

    private fun parseTitle(bufferContent: String?) {
        val marks = arrayOf("~ by:", "By")
        var found = false

        for (mark in marks) {
            val foundIndex = bufferContent?.indexOf(mark) ?: -1
            if (foundIndex >= 0) {
                if (author != null) {
                    val title = bufferContent?.substring(0, foundIndex)
                    item?.title = title
                    val authorName = bufferContent?.substring(foundIndex + mark.length)
                    author?.name = authorName?.trim()
                    author?.let { item?.authors?.add(it) }
                    author = null
                }
                found = true
                break
            }
        }

        if (!found) {
            item?.title = bufferContent
        }
    }

    private fun makeFormat(buffer: String?): String? {
        //TODO: maybe need to make the text more readable?
        val s1 = StringBuilder(buffer ?: "")
        val marks = arrayOf("Author:", "Price:", "Rating:")

        for (mark in marks) {
            val index = s1.indexOf(mark)
            if (index >= 0) {
                s1.insert(index, "<br/>")
            }
        }

        return s1.toString()
    }

    fun testTag(name: String, tag: String, ns: String?, nsName: String?): Boolean =
        name == tag && ns == nsName

    override fun processNamespaces(): Boolean = true

    override fun namespaceMapChangedHandler(namespaceMap: Map<String, String>) {
        this.namespaceMap = namespaceMap
    }

    protected open fun getNamespace(prefix: String): String? {
        val ns = namespaceMap?.get(prefix) ?: return null
        return ns.intern()
    }

    private fun extractBufferContent(): String? {
        val bufferContentArray = buffer.toString().toCharArray()
        buffer.clear()
        if (bufferContentArray.isEmpty()) {
            return null
        }
        return String(bufferContentArray)
    }
}
