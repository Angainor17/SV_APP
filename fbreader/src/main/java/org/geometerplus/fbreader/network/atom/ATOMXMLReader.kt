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

package org.geometerplus.fbreader.network.atom

import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.zlibrary.core.constants.XMLNamespaces
import org.geometerplus.zlibrary.core.util.MimeType
import org.geometerplus.zlibrary.core.xml.ZLStringMap
import org.geometerplus.zlibrary.core.xml.ZLXMLReaderAdapter

open class ATOMXMLReader<MetadataType : ATOMFeedMetadata, EntryType : ATOMEntry>(
    library: NetworkLibrary,
    private val feedHandler: ATOMFeedHandler<MetadataType, EntryType>,
    readEntryNotFeed: Boolean
) : ZLXMLReaderAdapter() {

    protected open val formattedBuffer = FormattedBuffer(library)
    private val buffer = StringBuilder()

    protected var state: Int = if (readEntryNotFeed) FEED else START
    protected var feedMetadataProcessed = false

    private var feed: MetadataType? = null
    private var entry: EntryType? = null
    private var author: ATOMAuthor? = null
    private var id: ATOMId? = null
    private var link: ATOMLink? = null
    private var category: ATOMCategory? = null
    private var updated: ATOMUpdated? = null
    private var published: ATOMPublished? = null
    private var icon: ATOMIcon? = null
    private var namespaceMap: Map<String, String>? = null

    companion object {
        protected const val FEED = 1
        protected const val F_ENTRY = 2
        protected const val FE_LINK = 17
        protected const val FE_CONTENT = 20
        protected const val ATOM_STATE_FIRST_UNUSED = 26

        protected const val TAG_FEED = "feed"
        protected const val TAG_ENTRY = "entry"
        protected const val TAG_AUTHOR = "author"
        protected const val TAG_NAME = "name"
        protected const val TAG_URI = "uri"
        protected const val TAG_EMAIL = "email"
        protected const val TAG_ID = "id"
        protected const val TAG_CATEGORY = "category"
        protected const val TAG_LINK = "link"
        protected const val TAG_PUBLISHED = "published"
        protected const val TAG_SUMMARY = "summary"
        protected const val TAG_CONTENT = "content"
        protected const val TAG_TITLE = "title"
        protected const val TAG_UPDATED = "updated"
        protected const val TAG_SUBTITLE = "subtitle"
        protected const val TAG_ICON = "icon"

        private const val START = 0
        private const val F_ID = 3
        private const val F_LINK = 4
        private const val F_CATEGORY = 5
        private const val F_TITLE = 6
        private const val F_UPDATED = 7
        private const val F_AUTHOR = 8
        private const val F_SUBTITLE = 9
        private const val F_ICON = 10
        private const val FA_NAME = 11
        private const val FA_URI = 12
        private const val FA_EMAIL = 13
        private const val FE_AUTHOR = 14
        private const val FE_ID = 15
        private const val FE_CATEGORY = 16
        private const val FE_PUBLISHED = 18
        private const val FE_SUMMARY = 19
        private const val FE_TITLE = 21
        private const val FE_UPDATED = 22
        private const val FEA_NAME = 23
        private const val FEA_URI = 24
        private const val FEA_EMAIL = 25

        @JvmStatic
        fun intern(str: String?): String? {
            if (str == null || str.isEmpty()) {
                return null
            }
            return str.intern()
        }
    }

    protected open fun getATOMFeedHandler(): ATOMFeedHandler<MetadataType, EntryType> = feedHandler
    protected open fun getATOMFeed(): MetadataType? = feed
    protected open fun getATOMEntry(): EntryType? = entry
    protected open fun getATOMLink(): ATOMLink? = link

    override fun processNamespaces(): Boolean = true

    override fun namespaceMapChangedHandler(namespaceMap: Map<String, String>) {
        this.namespaceMap = namespaceMap
    }

    protected open fun getNamespace(prefix: String): String? {
        val ns = namespaceMap?.get(prefix) ?: return null
        return ns.intern()
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

    private fun extractBufferContent(): String? {
        val bufferContentArray = buffer.toString().toCharArray()
        buffer.clear()
        if (bufferContentArray.isEmpty()) {
            return null
        }
        return String(bufferContentArray)
    }

    open fun startElementHandler(ns: String?, tag: String, attributes: ZLStringMap, bufferContent: String?): Boolean {
        var interruptReading = false
        when (state) {
            START -> {
                if (ns == XMLNamespaces.Atom && tag == TAG_FEED) {
                    feedHandler.processFeedStart()
                    feed = feedHandler.createFeed(attributes)
                    state = FEED
                    feedMetadataProcessed = false
                }
            }
            FEED -> {
                if (ns == XMLNamespaces.Atom) {
                    when (tag) {
                        TAG_AUTHOR -> {
                            author = ATOMAuthor(attributes)
                            state = F_AUTHOR
                        }
                        TAG_ID -> {
                            id = ATOMId(attributes)
                            state = F_ID
                        }
                        TAG_ICON -> {
                            icon = ATOMIcon(attributes)
                            state = F_ICON
                        }
                        TAG_LINK -> {
                            link = feedHandler.createLink(attributes)
                            state = F_LINK
                        }
                        TAG_CATEGORY -> {
                            category = ATOMCategory(attributes)
                            state = F_CATEGORY
                        }
                        TAG_TITLE -> {
                            setFormattingType(attributes.getValue("type"))
                            state = F_TITLE
                        }
                        TAG_SUBTITLE -> {
                            setFormattingType(attributes.getValue("type"))
                            state = F_SUBTITLE
                        }
                        TAG_UPDATED -> {
                            updated = ATOMUpdated(attributes)
                            state = F_UPDATED
                        }
                        TAG_ENTRY -> {
                            entry = feedHandler.createEntry(attributes)
                            state = F_ENTRY
                            if (feed != null && !feedMetadataProcessed) {
                                interruptReading = feedHandler.processFeedMetadata(feed!!, true)
                                feedMetadataProcessed = true
                            }
                        }
                    }
                }
            }
            F_ENTRY -> {
                if (ns == XMLNamespaces.Atom) {
                    when (tag) {
                        TAG_AUTHOR -> {
                            author = ATOMAuthor(attributes)
                            state = FE_AUTHOR
                        }
                        TAG_ID -> {
                            id = ATOMId(attributes)
                            state = FE_ID
                        }
                        TAG_CATEGORY -> {
                            category = ATOMCategory(attributes)
                            state = FE_CATEGORY
                        }
                        TAG_LINK -> {
                            link = feedHandler.createLink(attributes)
                            state = FE_LINK
                        }
                        TAG_PUBLISHED -> {
                            published = ATOMPublished(attributes)
                            state = FE_PUBLISHED
                        }
                        TAG_SUMMARY -> {
                            setFormattingType(attributes.getValue("type"))
                            state = FE_SUMMARY
                        }
                        TAG_CONTENT -> {
                            setFormattingType(attributes.getValue("type"))
                            state = FE_CONTENT
                        }
                        TAG_TITLE -> {
                            setFormattingType(attributes.getValue("type"))
                            state = FE_TITLE
                        }
                        TAG_UPDATED -> {
                            updated = ATOMUpdated(attributes)
                            state = FE_UPDATED
                        }
                    }
                }
            }
            F_AUTHOR -> {
                if (ns == XMLNamespaces.Atom) {
                    when (tag) {
                        TAG_NAME -> state = FA_NAME
                        TAG_URI -> state = FA_URI
                        TAG_EMAIL -> state = FA_EMAIL
                    }
                }
            }
            FE_AUTHOR -> {
                if (ns == XMLNamespaces.Atom) {
                    when (tag) {
                        TAG_NAME -> state = FEA_NAME
                        TAG_URI -> state = FEA_URI
                        TAG_EMAIL -> state = FEA_EMAIL
                    }
                }
            }
            FE_CONTENT, FE_SUMMARY, FE_TITLE, F_TITLE, F_SUBTITLE -> {
                formattedBuffer.appendText(bufferContent)
                formattedBuffer.appendStartTag(tag, attributes)
            }
        }
        return interruptReading
    }

    open fun endElementHandler(ns: String?, tag: String, bufferContent: String?): Boolean {
        var interruptReading = false
        when (state) {
            FEED -> {
                if (ns == XMLNamespaces.Atom && tag == TAG_FEED) {
                    feed?.let { interruptReading = feedHandler.processFeedMetadata(it, false) }
                    feed = null
                    feedHandler.processFeedEnd()
                    state = START
                }
            }
            F_ENTRY -> {
                if (ns == XMLNamespaces.Atom && tag == TAG_ENTRY) {
                    entry?.let { interruptReading = feedHandler.processFeedEntry(it) }
                    entry = null
                    state = FEED
                }
            }
            F_ID -> {
                if (ns == XMLNamespaces.Atom && tag == TAG_ID) {
                    if (bufferContent != null && feed != null) {
                        id?.uri = bufferContent
                        id?.let { feed?.id = it }
                    }
                    id = null
                    state = FEED
                }
            }
            F_ICON -> {
                if (ns == XMLNamespaces.Atom && tag == TAG_ICON) {
                    if (bufferContent != null && feed != null) {
                        icon?.uri = bufferContent
                        icon?.let { feed?.icon = it }
                    }
                    icon = null
                    state = FEED
                }
            }
            F_LINK -> {
                if (ns == XMLNamespaces.Atom && tag == TAG_LINK) {
                    link?.let { feed?.links?.add(it) }
                    link = null
                    state = FEED
                }
            }
            F_CATEGORY -> {
                if (ns == XMLNamespaces.Atom && tag == TAG_CATEGORY) {
                    category?.let { feed?.categories?.add(it) }
                    category = null
                    state = FEED
                }
            }
            F_TITLE -> {
                formattedBuffer.appendText(bufferContent)
                if (ns == XMLNamespaces.Atom && tag == TAG_TITLE) {
                    val title = formattedBuffer.getText()
                    feed?.title = title
                    state = FEED
                } else {
                    formattedBuffer.appendEndTag(tag)
                }
            }
            F_SUBTITLE -> {
                formattedBuffer.appendText(bufferContent)
                if (ns == XMLNamespaces.Atom && tag == TAG_SUBTITLE) {
                    val subtitle = formattedBuffer.getText()
                    feed?.subtitle = subtitle
                    state = FEED
                } else {
                    formattedBuffer.appendEndTag(tag)
                }
            }
            F_UPDATED -> {
                if (ns == XMLNamespaces.Atom && tag == TAG_UPDATED) {
                    updated?.let { u ->
                        if (ATOMDateConstruct.parse(bufferContent, u) && feed != null) {
                            feed?.updated = u
                        }
                    }
                    updated = null
                    state = FEED
                }
            }
            F_AUTHOR -> {
                if (ns == XMLNamespaces.Atom && tag == TAG_AUTHOR) {
                    if (feed != null && author?.name != null) {
                        author?.let { feed?.authors?.add(it) }
                    }
                    author = null
                    state = FEED
                }
            }
            FA_NAME -> {
                if (ns == XMLNamespaces.Atom && tag == TAG_NAME) {
                    author?.name = bufferContent
                    state = F_AUTHOR
                }
            }
            FEA_NAME -> {
                if (ns == XMLNamespaces.Atom && tag == TAG_NAME) {
                    author?.name = bufferContent
                    state = FE_AUTHOR
                }
            }
            FA_URI -> {
                if (ns == XMLNamespaces.Atom && tag == TAG_URI) {
                    author?.uri = bufferContent
                    state = F_AUTHOR
                }
            }
            FEA_URI -> {
                if (ns == XMLNamespaces.Atom && tag == TAG_URI) {
                    author?.uri = bufferContent
                    state = FE_AUTHOR
                }
            }
            FA_EMAIL -> {
                if (ns == XMLNamespaces.Atom && tag == TAG_EMAIL) {
                    author?.email = bufferContent
                    state = F_AUTHOR
                }
            }
            FEA_EMAIL -> {
                if (ns == XMLNamespaces.Atom && tag == TAG_EMAIL) {
                    author?.email = bufferContent
                    state = FE_AUTHOR
                }
            }
            FE_AUTHOR -> {
                if (ns == XMLNamespaces.Atom && tag == TAG_AUTHOR) {
                    if (author?.name != null) {
                        entry?.authors?.add(author!!)
                    }
                    author = null
                    state = F_ENTRY
                }
            }
            FE_ID -> {
                if (ns == XMLNamespaces.Atom && tag == TAG_ID) {
                    if (bufferContent != null) {
                        id?.uri = bufferContent
                        id?.let { entry?.id = it }
                    }
                    id = null
                    state = F_ENTRY
                }
            }
            FE_CATEGORY -> {
                if (ns == XMLNamespaces.Atom && tag == TAG_CATEGORY) {
                    category?.let { entry?.categories?.add(it) }
                    category = null
                    state = F_ENTRY
                }
            }
            FE_LINK -> {
                if (ns == XMLNamespaces.Atom && tag == TAG_LINK) {
                    link?.let { entry?.links?.add(it) }
                    link = null
                    state = F_ENTRY
                }
            }
            FE_PUBLISHED -> {
                if (ns == XMLNamespaces.Atom && tag == TAG_PUBLISHED) {
                    published?.let { p ->
                        if (ATOMDateConstruct.parse(bufferContent, p)) {
                            entry?.published = p
                        }
                    }
                    published = null
                    state = F_ENTRY
                }
            }
            FE_SUMMARY -> {
                formattedBuffer.appendText(bufferContent)
                if (ns == XMLNamespaces.Atom && tag == TAG_SUMMARY) {
                    entry?.summary = formattedBuffer.getText()
                    state = F_ENTRY
                } else {
                    formattedBuffer.appendEndTag(tag)
                }
            }
            FE_CONTENT -> {
                formattedBuffer.appendText(bufferContent)
                if (ns == XMLNamespaces.Atom && tag == TAG_CONTENT) {
                    entry?.content = formattedBuffer.getText()
                    state = F_ENTRY
                } else {
                    formattedBuffer.appendEndTag(tag)
                }
            }
            FE_TITLE -> {
                formattedBuffer.appendText(bufferContent)
                if (ns == XMLNamespaces.Atom && tag == TAG_TITLE) {
                    entry?.title = formattedBuffer.getText()
                    state = F_ENTRY
                } else {
                    formattedBuffer.appendEndTag(tag)
                }
            }
            FE_UPDATED -> {
                if (ns == XMLNamespaces.Atom && tag == TAG_UPDATED) {
                    try {
                        updated?.let { u ->
                            if (ATOMDateConstruct.parse(bufferContent, u)) {
                                entry?.updated = u
                            }
                        }
                    } catch (e: Exception) {
                        // this ATOMDateConstruct.parse() throws OOB exception time to time
                    }
                    updated = null
                    state = F_ENTRY
                }
            }
        }
        return interruptReading
    }

    override fun characterDataHandler(data: CharArray, start: Int, length: Int) {
        buffer.append(data, start, length)
    }

    open fun setFormattingType(type: String?) {
        when {
            ATOMConstants.TYPE_HTML == type || MimeType.TEXT_HTML.name == type ->
                formattedBuffer.reset(FormattedBuffer.Type.Html)
            ATOMConstants.TYPE_XHTML == type || MimeType.TEXT_XHTML.name == type ->
                formattedBuffer.reset(FormattedBuffer.Type.XHtml)
            else -> formattedBuffer.reset(FormattedBuffer.Type.Text)
        }
    }
}
