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

package org.geometerplus.fbreader.network.opds

import org.geometerplus.zlibrary.core.constants.XMLNamespaces
import org.geometerplus.zlibrary.core.util.MimeType
import org.geometerplus.zlibrary.core.util.ZLNetworkUtil
import org.geometerplus.zlibrary.core.xml.ZLStringMap
import org.geometerplus.zlibrary.core.xml.ZLXMLReaderAdapter

internal class OpenSearchXMLReader(
    private val baseURL: String,
    private val descriptions: MutableList<OpenSearchDescription>
) : ZLXMLReaderAdapter() {

    private var state = START

    override fun processNamespaces(): Boolean = true

    private fun parseInt(value: String?): Int {
        if (value.isNullOrEmpty()) {
            return -1
        }
        return try {
            value.toInt()
        } catch (e: NumberFormatException) {
            -1
        }
    }

    override fun startElementHandler(tag: String, attributes: ZLStringMap): Boolean {
        val lowerTag = tag.lowercase()

        when (state) {
            START -> {
                if (testTag(XMLNamespaces.OpenSearch, TAG_DESCRIPTION, lowerTag)) {
                    state = DESCRIPTION
                }
            }
            DESCRIPTION -> {
                if (testTag(XMLNamespaces.OpenSearch, TAG_URL, lowerTag)) {
                    val mime = MimeType.get(attributes.getValue("type"))
                    val rel = attributes.getValue("rel")
                    if ((MimeType.APP_ATOM_XML.weakEquals(mime) || MimeType.TEXT_HTML.weakEquals(mime)) &&
                        (rel == null || rel == "results")) {
                        val template = attributes.getValue("template")
                        if (template != null) {
                            val tmpl = ZLNetworkUtil.url(baseURL, template)
                            if (tmpl != null) {
                                val indexOffset = parseInt(attributes.getValue("indexOffset"))
                                val pageOffset = parseInt(attributes.getValue("pageOffset"))
                                val descr = OpenSearchDescription(tmpl, indexOffset, pageOffset, mime)
                                if (descr.isValid()) {
                                    descriptions.add(0, descr)
                                }
                            }
                        }
                    }
                }
            }
        }

        return false
    }

    override fun endElementHandler(tag: String): Boolean {
        val lowerTag = tag.lowercase()
        when (state) {
            DESCRIPTION -> {
                if (testTag(XMLNamespaces.OpenSearch, TAG_DESCRIPTION, lowerTag)) {
                    state = START
                }
            }
        }
        return false
    }

    companion object {
        private const val START = 0
        private const val DESCRIPTION = 1
        private const val TAG_DESCRIPTION = "opensearchdescription"
        private const val TAG_URL = "url"
    }
}
