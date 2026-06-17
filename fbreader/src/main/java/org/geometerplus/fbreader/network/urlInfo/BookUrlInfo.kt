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

import android.net.Uri
import org.geometerplus.fbreader.Paths
import org.geometerplus.fbreader.formats.PluginCollection
import org.geometerplus.zlibrary.core.filetypes.FileTypeCollection
import org.geometerplus.zlibrary.core.util.MimeType
import org.geometerplus.zlibrary.core.util.SystemInfo
import java.io.File

// resolvedReferenceType -- reference type without any ambiguity (for example, DOWNLOAD_FULL_OR_DEMO is ambiguous)

open class BookUrlInfo(
    type: Type,
    url: String?,
    mime: MimeType
) : UrlInfo(type, url, mime) {

    companion object {
        private const val serialVersionUID: Long = -893514485257788221L
        private const val TOESCAPE = "<>:\"|?*\\"

        @JvmStatic
        fun isMimeSupported(mime: MimeType?, systemInfo: SystemInfo): Boolean {
            if (mime == null) {
                return false
            }
            val type = FileTypeCollection.Instance.typeForMime(mime) ?: return false
            return PluginCollection.Instance(systemInfo).getPlugin(type) != null
        }

        private fun mimePriority(mime: MimeType?): Int {
            return when {
                mime == null -> -1
                MimeType.TYPES_MOBIPOCKET.contains(mime) -> 1
                MimeType.TYPES_FB2.contains(mime) -> 2
                MimeType.TYPES_FB2_ZIP.contains(mime) -> 3
                MimeType.TYPES_EPUB.contains(mime) -> 4
                else -> 0
            }
        }

        @JvmStatic
        fun isMimeBetterThan(mime0: MimeType?, mime1: MimeType?): Boolean = mimePriority(mime0) > mimePriority(mime1)

        @JvmStatic
        fun makeBookFileName(url: String, mime: MimeType?, resolvedReferenceType: Type?): String? {
            val uri = Uri.parse(url)

            var host = uri.host ?: "host.unknown"

            val path = StringBuilder(host)
            if (host.startsWith("www.")) {
                path.delete(0, 4)
            }
            path.insert(0, File.separator)

            val port = uri.port
            if (port != -1) {
                path.append("_").append(port)
                path.insert(0, File.separator)
            }

            if (resolvedReferenceType == Type.BookDemo) {
                path.insert(0, "Demos")
                path.insert(0, File.separator)
            }
            path.insert(0, Paths.downloadsDirectoryOption.value)

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
                        path.setCharAt(index, File.separatorChar)
                        nameIndex = index + 1
                    }
                }
                ++index
            }

            val type = if (mime != null) FileTypeCollection.Instance.typeForMime(mime) else null
            var ext = type?.defaultExtension(mime)?.let { ".$it" }

            if (ext == null) {
                val j = path.indexOf(".", nameIndex) // using not lastIndexOf to preserve extensions like `.fb2.zip`
                if (j != -1) {
                    ext = path.substring(j)
                    path.delete(j, path.length)
                } else {
                    return null
                }
            } else if (path.length > ext.length && path.substring(path.length - ext.length) == ext) {
                path.delete(path.length - ext.length, path.length)
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
            return path.append(ext).toString()
        }
    }

    // Url with no user-dependent info; is overridden in DecoratedBookUrlInfo
    open fun cleanUrl(): String? = url

    fun makeBookFileName(resolvedReferenceType: Type?): String? {
        val cleanUrl = cleanUrl()
        return if (cleanUrl != null) makeBookFileName(cleanUrl, mime, resolvedReferenceType) else null
    }

    fun localCopyFileName(resolvedReferenceType: Type?): String? {
        val fileName = makeBookFileName(resolvedReferenceType) ?: return null
        return if (File(fileName).exists()) fileName else null
    }

    override fun toString(): String = "BookReference[type=$infoType;mime=$mime;URL=$url]"
}
