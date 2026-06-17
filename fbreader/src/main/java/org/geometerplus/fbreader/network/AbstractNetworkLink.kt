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

import org.geometerplus.fbreader.network.tree.NetworkItemsLoader
import org.geometerplus.fbreader.network.urlInfo.UrlInfo
import org.geometerplus.fbreader.network.urlInfo.UrlInfoCollection
import org.geometerplus.fbreader.network.urlInfo.UrlInfoWithDate
import org.geometerplus.zlibrary.core.language.Language
import org.geometerplus.zlibrary.core.network.ZLNetworkRequest
import java.util.Locale
import java.util.regex.Pattern

abstract class AbstractNetworkLink(
    id: Int,
    title: String,
    summary: String?,
    language: String?,
    infos: UrlInfoCollection<UrlInfoWithDate>
) : INetworkLink {

    protected val languageInternal: String = language ?: "multi"
    protected val infosInternal: UrlInfoCollection<UrlInfoWithDate> = UrlInfoCollection(infos)
    protected var titleInternal: String = title
    protected var summaryInternal: String? = summary
    private var idInternal: Int = id

    companion object {
        private fun getLanguageOrder(language: String?): Int {
            if (Language.MULTI_CODE == language) {
                return 1
            }
            if (language == Locale.getDefault().language) {
                return 0
            }
            return 2
        }
    }

    override val id: Int
        get() = idInternal

    override fun setId(id: Int) {
        idInternal = id
    }

    override val shortName: String
        get() = hostName ?: "CATALOG_$idInternal"

    override val stringId: String
        get() = hostName ?: "CATALOG_$idInternal"

    override val hostName: String?
        get() {
            val catalogUrl = getUrl(UrlInfo.Type.Catalog) ?: return null
            val m = Pattern.compile("^[a-zA-Z]+://([^/]+).*").matcher(catalogUrl)
            return if (m.matches()) m.group(1) else null
        }

    override val title: String
        get() = titleInternal

    override val summary: String?
        get() = summaryInternal

    override val language: String?
        get() = languageInternal

    fun urlInfoMap(): UrlInfoCollection<UrlInfoWithDate> = UrlInfoCollection(infosInternal)

    override fun getUrl(type: UrlInfo.Type): String? = getUrlInfo(type).url

    override fun getUrlInfo(type: UrlInfo.Type): UrlInfoWithDate {
        return infosInternal.getInfo(type) ?: UrlInfoWithDate.NULL
    }

    override val urlKeys: Set<UrlInfo.Type>
        get() = infosInternal.getAllInfos().map { it.infoType }.toSet()

    override val basketItem: BasketItem?
        get() = null

    fun bookListRequest(bookIds: List<String>, data: NetworkOperationData): ZLNetworkRequest? = null

    override fun createOperationData(loader: NetworkItemsLoader?): NetworkOperationData =
        NetworkOperationData(this, loader)

    override fun toString(): String {
        var icon = getUrl(UrlInfo.Type.Catalog)
        if (icon != null) {
            if (icon.length > 64) {
                icon = icon.substring(0, 61) + "..."
            }
            icon = icon.replace("\n", "")
        }
        return "AbstractNetworkLink: {" +
                "id=$stringId" +
                "; title=$titleInternal" +
                "; summary=$summaryInternal" +
                "; icon=$icon" +
                "; infos=$infosInternal" +
                "}"
    }

    private fun getTitleForComparison(): String {
        val title = title
        for (index in title.indices) {
            val ch = title[index]
            if (ch.code < 128 && Character.isLetter(ch)) {
                return title.substring(index)
            }
        }
        return title
    }

    override fun compareTo(other: INetworkLink): Int {
        var diff = getLanguageOrder(language) - getLanguageOrder(other.language)
        if (diff != 0) return diff
        diff = getTitleForComparison().compareTo((other as AbstractNetworkLink).getTitleForComparison(), ignoreCase = true)
        if (diff != 0) return diff
        return id - other.id
    }
}
