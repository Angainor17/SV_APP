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

import org.geometerplus.fbreader.network.NetworkLibrary

class SimpleOPDSFeedHandler(
    private val library: NetworkLibrary,
    private val baseURL: String,
    private val networkLink: OPDSNetworkLink? = null
) : AbstractOPDSFeedHandler() {

    private val books = mutableListOf<OPDSBookItem>()
    private var index = 0

    override fun processFeedStart() {}

    override fun processFeedMetadata(feed: OPDSFeedMetadata, beforeEntries: Boolean): Boolean = false

    override fun processFeedEnd() {}

    override fun processFeedEntry(entry: OPDSEntry): Boolean {
        val link = networkLink ?: return false
        val item = OPDSBookItem(library, link, entry, baseURL, index++)
        for (identifier in entry.dcIdentifiers) {
            item.identifiers.add(identifier)
        }
        books.add(item)
        return false
    }

    fun books(): List<OPDSBookItem> = books.toList()
}
