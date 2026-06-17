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

package org.geometerplus.fbreader.network

import org.geometerplus.fbreader.network.opds.OPDSCustomNetworkLink
import org.geometerplus.fbreader.network.opds.OPDSPredefinedNetworkLink
import org.geometerplus.fbreader.network.urlInfo.UrlInfo
import org.geometerplus.fbreader.network.urlInfo.UrlInfoCollection
import org.geometerplus.fbreader.network.urlInfo.UrlInfoWithDate

abstract class NetworkDatabase protected constructor(protected val library: NetworkLibrary) {

    companion object {
        private var ourInstance: NetworkDatabase? = null

        @JvmStatic
        fun Instance(): NetworkDatabase? = ourInstance
    }

    init {
        ourInstance = this
    }

    protected abstract fun executeAsTransaction(actions: Runnable)

    protected fun createLink(
        id: Int,
        type: INetworkLink.Type,
        predefinedId: String?,
        title: String?,
        summary: String?,
        language: String?,
        infos: UrlInfoCollection<UrlInfoWithDate>
    ): INetworkLink? {
        if (title == null || infos.getInfo(UrlInfo.Type.Catalog) == null) {
            return null
        }
        return when (type) {
            INetworkLink.Type.Predefined -> OPDSPredefinedNetworkLink(
                library, id, predefinedId!!, title, summary, language, infos
            )

            else -> OPDSCustomNetworkLink(
                library = library, id = id, title = title, summary = summary, language = language, infos = infos
            )
        }
    }

    abstract fun listLinks(): List<INetworkLink>

    abstract fun saveLink(link: INetworkLink)

    abstract fun deleteLink(link: INetworkLink)

    protected abstract fun getLinkExtras(link: INetworkLink): Map<String, String>

    protected abstract fun setLinkExtras(link: INetworkLink, extras: Map<String, String>)
}
