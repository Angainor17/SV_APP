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

import org.geometerplus.fbreader.network.INetworkLink
import org.geometerplus.fbreader.network.IPredefinedNetworkLink
import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.fbreader.network.urlInfo.UrlInfoCollection
import org.geometerplus.fbreader.network.urlInfo.UrlInfoWithDate

class OPDSPredefinedNetworkLink(
    library: NetworkLibrary,
    id: Int,
    private val predefinedId: String,
    title: String,
    summary: String?,
    language: String?,
    infos: UrlInfoCollection<UrlInfoWithDate>
) : OPDSNetworkLink(library, id, title, summary, language, infos), IPredefinedNetworkLink {

    override val type: INetworkLink.Type
        get() = INetworkLink.Type.Predefined

    override fun getPredefinedId(): String = predefinedId

    override val shortName: String
        get() = if (predefinedId.startsWith(ID_PREFIX)) {
            predefinedId.substring(ID_PREFIX.length)
        } else {
            predefinedId
        }

    override val stringId: String
        get() = shortName

    override fun servesHost(hostname: String?): Boolean {
        return hostname != null && hostname.indexOf(shortName) != -1
    }

    companion object {
        private const val ID_PREFIX = "urn:fbreader-org-catalog:"
    }
}
