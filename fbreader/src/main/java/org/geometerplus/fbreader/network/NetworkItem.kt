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

abstract class NetworkItem protected constructor(
    @JvmField val link: INetworkLink?,
    @JvmField val title: CharSequence,
    summary: CharSequence?,
    urls: UrlInfoCollection<*>?
) {
    private val urlsInternal: UrlInfoCollection<UrlInfo>? = if (urls != null && !urls.isEmpty()) {
        UrlInfoCollection(urls)
    } else {
        null
    }

    private var summaryInternal: CharSequence? = summary

    open fun getSummary(): CharSequence? = summaryInternal

    protected fun setSummary(summary: CharSequence?) {
        summaryInternal = summary
    }

    protected fun addUrls(urls: UrlInfoCollection<*>) {
        urlsInternal?.upgrade(urls)
    }

    fun getAllInfos(): List<UrlInfo> = urlsInternal?.getAllInfos() ?: emptyList()

    fun getAllInfos(type: UrlInfo.Type): List<UrlInfo> = urlsInternal?.getAllInfos(type) ?: emptyList()

    fun getUrl(type: UrlInfo.Type): String? = urlsInternal?.getUrl(type)
}
