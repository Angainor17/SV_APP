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

import org.geometerplus.fbreader.fbreader.options.SyncOptions
import org.geometerplus.fbreader.network.INetworkLink
import org.geometerplus.fbreader.network.ISyncNetworkLink
import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.fbreader.network.sync.SyncUtil
import org.geometerplus.fbreader.network.urlInfo.UrlInfo
import org.geometerplus.fbreader.network.urlInfo.UrlInfoCollection
import org.geometerplus.fbreader.network.urlInfo.UrlInfoWithDate
import org.geometerplus.zlibrary.core.network.QuietNetworkContext
import org.geometerplus.zlibrary.core.network.ZLNetworkContext
import org.geometerplus.zlibrary.core.resources.ZLResource
import org.geometerplus.zlibrary.core.util.MimeType

class OPDSSyncNetworkLink private constructor(
    library: NetworkLibrary,
    id: Int,
    title: String,
    infos: UrlInfoCollection<UrlInfoWithDate>
) : OPDSNetworkLink(library, id, title, null, null, infos), ISyncNetworkLink {

    constructor(library: NetworkLibrary) : this(library, -1, resource.getValue(), initialUrlInfos())

    override val summary: String?
        get() {
            val account = SyncUtil.getAccountName(QuietNetworkContext())
            return account ?: resource.getResource("summary").value
        }

    override val type: INetworkLink.Type
        get() = INetworkLink.Type.Sync

    override fun isLoggedIn(context: ZLNetworkContext?): Boolean {
        return SyncUtil.getAccountName(context!!) != null
    }

    override fun logout(context: ZLNetworkContext?) {
        SyncUtil.logout(context!!)
    }

    companion object {
        private val resource: ZLResource
            get() = NetworkLibrary.resource().getResource("sync")

        private fun initialUrlInfos(): UrlInfoCollection<UrlInfoWithDate> {
            val infos = UrlInfoCollection<UrlInfoWithDate>()
            infos.addInfo(UrlInfoWithDate(
                UrlInfo.Type.Catalog,
                SyncOptions.OPDS_URL,
                MimeType.OPDS
            ))
            infos.addInfo(UrlInfoWithDate(
                UrlInfo.Type.Search,
                SyncOptions.BASE_URL + "opds/search/%s",
                MimeType.OPDS
            ))
            infos.addInfo(UrlInfoWithDate(
                UrlInfo.Type.Image,
                SyncOptions.BASE_URL + "static/images/logo-120x120.png",
                MimeType.IMAGE_PNG
            ))
            infos.addInfo(UrlInfoWithDate(
                UrlInfo.Type.SearchIcon,
                SyncOptions.BASE_URL + "static/images/folders-light/search.png",
                MimeType.IMAGE_PNG
            ))
            return infos
        }
    }
}
