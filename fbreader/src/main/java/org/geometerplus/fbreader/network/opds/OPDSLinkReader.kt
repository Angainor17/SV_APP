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
import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.zlibrary.core.filesystem.ZLPhysicalFile
import org.geometerplus.zlibrary.core.network.ZLNetworkContext
import org.geometerplus.zlibrary.core.network.ZLNetworkException
import org.geometerplus.zlibrary.core.network.ZLNetworkRequest

import java.io.File
import java.io.IOException

object OPDSLinkReader {
    const val CATALOGS_URL = "https://data.fbreader.org/catalogs/generic-2.0.xml"
    val FILE_NAME = "fbreader_catalogs-" + CATALOGS_URL.substring(CATALOGS_URL.lastIndexOf("/") + 1)

    @Throws(ZLNetworkException::class)
    @JvmStatic
    fun loadOPDSLinks(library: NetworkLibrary, nc: ZLNetworkContext, cacheMode: CacheMode): List<INetworkLink> {
        val xmlReader = OPDSLinkXMLReader(library)

        val dirFile = File(library.systemInfo.networkCacheDirectory())
        if (!dirFile.exists() && !dirFile.mkdirs()) {
            nc.perform(object : ZLNetworkRequest.Get(CATALOGS_URL) {
                override fun handleStream(inputStream: java.io.InputStream, length: Int) {
                    xmlReader.read(inputStream)
                }
            })
            return xmlReader.links()
        }

        var cacheIsGood = false
        var oldCache: File? = null
        val catalogsFile = File(dirFile, FILE_NAME)
        if (catalogsFile.exists()) {
            when (cacheMode) {
                CacheMode.UPDATE -> {
                    val diff = System.currentTimeMillis() - catalogsFile.lastModified()
                    if (diff >= 0 && diff <= 7 * 24 * 60 * 60 * 1000L) { // one week
                        return emptyList()
                    }
                    // FALLTHROUGH to CLEAR
                }
                CacheMode.CLEAR -> {
                    oldCache = File(dirFile, "_$FILE_NAME")
                    oldCache.delete()
                    if (!catalogsFile.renameTo(oldCache)) {
                        catalogsFile.delete()
                        oldCache = null
                    }
                }
                CacheMode.LOAD -> {
                    cacheIsGood = true
                }
            }
        }

        if (!cacheIsGood) {
            try {
                nc.downloadToFile(CATALOGS_URL, catalogsFile)
            } catch (e: ZLNetworkException) {
                if (oldCache == null) {
                    throw e
                }
                catalogsFile.delete()
                if (!oldCache.renameTo(catalogsFile)) {
                    oldCache.delete()
                    oldCache = null
                    throw e
                }
            } finally {
                oldCache?.delete()
                oldCache = null
            }
        }

        return try {
            xmlReader.read(ZLPhysicalFile(catalogsFile))
            xmlReader.links()
        } catch (e: IOException) {
            emptyList()
        }
    }

    enum class CacheMode {
        LOAD,
        UPDATE,
        CLEAR
    }
}
