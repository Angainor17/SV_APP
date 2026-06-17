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

package org.geometerplus.fbreader.network.urlInfo

import java.io.Serializable

class UrlInfoCollection<T : UrlInfo>(vararg elements: T) : Serializable {

    companion object {
        private const val serialVersionUID: Long = -834589080548958222L
    }

    private val infos = mutableListOf<T>()

    init {
        for (info in elements) {
            addInfo(info)
        }
    }

    constructor(other: UrlInfoCollection<out T>) : this() {
        infos.addAll(other.infos)
    }

    fun upgrade(other: UrlInfoCollection<out T>) {
        infos.removeAll(other.infos.toSet())
        infos.addAll(other.infos)
    }

    fun addInfo(info: T?) {
        if (info != null && info.infoType != null) {
            infos.add(info)
        }
    }

    fun getInfo(type: UrlInfo.Type): T? {
        for (info in infos) {
            if (info.infoType == type) {
                return info
            }
        }
        return null
    }

    fun getAllInfos(): List<T> = infos.toList()

    fun getAllInfos(type: UrlInfo.Type): List<T> {
        val list = mutableListOf<T>()
        for (info in infos) {
            if (info.infoType == type) {
                list.add(info)
            }
        }
        return list
    }

    fun getUrl(type: UrlInfo.Type): String? = getInfo(type)?.url

    fun clear() {
        infos.clear()
    }

    fun removeAllInfos(type: UrlInfo.Type) {
        val toRemove = mutableListOf<T>()
        for (info in infos) {
            if (info.infoType == type) {
                toRemove.add(info)
            }
        }
        infos.removeAll(toRemove)
    }

    fun isEmpty(): Boolean = infos.isEmpty()
}
