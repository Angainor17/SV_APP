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

import org.geometerplus.fbreader.network.urlInfo.UrlInfo
import org.geometerplus.fbreader.network.urlInfo.UrlInfoCollection
import org.geometerplus.fbreader.network.urlInfo.UrlInfoWithDate
import org.geometerplus.zlibrary.core.network.ZLNetworkContext
import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.util.MimeType

interface ICustomNetworkLink : INetworkLink {

    fun setTitle(title: String)

    fun setSummary(summary: String?)

    fun urlInfoMap(): UrlInfoCollection<UrlInfoWithDate>

    fun setUrl(type: UrlInfo.Type, url: String?, mime: MimeType?)

    fun removeUrl(type: UrlInfo.Type)

    fun isObsolete(milliSeconds: Long): Boolean

    @Throws(ZLNetworkException::class)
    fun reloadInfo(nc: ZLNetworkContext, urlsOnly: Boolean, quietly: Boolean)

    // returns true if next methods have changed link's data:
    //   setTitle, setSummary, setIcon, setLink, removeLink
    fun hasChanges(): Boolean

    // resets hasChanged() result
    fun resetChanges()
}
