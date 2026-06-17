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

import android.net.Uri
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.image.ZLFileImage
import org.geometerplus.zlibrary.core.image.ZLImageSimpleProxy
import org.geometerplus.zlibrary.core.network.QuietNetworkContext
import org.geometerplus.zlibrary.core.util.SystemInfo
import java.io.File

class NetworkImage(
    @JvmField val url: String,
    private val systemInfo: SystemInfo
) : ZLImageSimpleProxy() {

    companion object {
        private const val TOESCAPE = "<>:\"|?*\\"
    }

    @Volatile
    private var storedFilePath: String? = null
    @Volatile
    private var fileImage: ZLFileImage? = null

    init {
        File(systemInfo.networkCacheDirectory()).mkdirs()
    }

    private fun makeImageFilePath(url: String): String {
        val uri = Uri.parse(url)

        val path = StringBuilder(systemInfo.networkCacheDirectory())
        path.append(File.separator)

        val host = uri.host
        path.append(host ?: "host.unknown")

        var index = path.length

        val uriPath = uri.path
        if (uriPath != null) {
            path.append(uriPath)
        }

        var nameIndex = index
        while (index < path.length) {
            val ch = path[index]
            if (TOESCAPE.indexOf(ch) != -1) {
                path.setCharAt(index, '_')
            }
            if (ch == '/') {
                if (index + 1 == path.length) {
                    path.deleteCharAt(index)
                } else {
                    path.setCharAt(index, '_')
                    nameIndex = index + 1
                }
            }
            ++index
        }

        val query = uri.query
        if (query != null) {
            index = 0
            while (index < query.length) {
                var j = query.indexOf("&", index)
                if (j == -1) {
                    j = query.length
                }
                val param = query.substring(index, j)
                if (!param.startsWith("username=")
                    && !param.startsWith("password=")
                    && !param.endsWith("=")
                ) {
                    var k = path.length
                    path.append("_").append(param)
                    while (k < path.length) {
                        val ch = path[k]
                        if (TOESCAPE.indexOf(ch) != -1 || ch == '/') {
                            path.setCharAt(k, '_')
                        }
                        ++k
                    }
                }
                index = j + 1
            }
        }
        return path.toString()
    }

    private fun getFilePath(): String? {
        if (storedFilePath == null) {
            storedFilePath = makeImageFilePath(url)
        }
        return storedFilePath
    }

    override fun isOutdated(): Boolean {
        val path = getFilePath() ?: return true
        return !File(path).exists()
    }

    override fun sourceType(): SourceType = SourceType.NETWORK

    override fun getId(): String = url

    override fun getURI(): String? {
        // TODO: implement
        return null
    }

    override fun synchronize() {
        synchronizeInternal(false)
    }

    fun synchronizeFast() {
        synchronizeInternal(true)
    }

    @Synchronized
    private fun synchronizeInternal(doFast: Boolean) {
        if (isSynchronized) {
            return
        }
        try {
            val path = getFilePath() ?: return
            val index = path.lastIndexOf(File.separator)
            if (index != -1) {
                val dir = path.substring(0, index)
                val dirFile = File(dir)
                if (!dirFile.exists() && !dirFile.mkdirs()) {
                    return
                }
                if (!dirFile.exists() || !dirFile.isDirectory) {
                    return
                }
            }
            val imageFile = File(path)
            if (imageFile.exists()) {
                val diff = System.currentTimeMillis() - imageFile.lastModified()
                val valid = 24 * 60 * 60 * 1000L // one day in milliseconds; FIXME: hardcoded const
                if (diff in 0..valid) {
                    return
                }
                imageFile.delete()
            }
            if (doFast) {
                return
            }
            QuietNetworkContext().downloadToFileQuietly(url, imageFile)
        } finally {
            setSynchronized()
        }
    }

    override fun getRealImage(): ZLFileImage? {
        if (fileImage == null) {
            if (!isSynchronized) {
                return null
            }
            val path = getFilePath() ?: return null
            val file = ZLFile.createFileByPath(path) ?: return null
            fileImage = ZLFileImage(file)
        }
        return fileImage
    }
}
