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
import org.geometerplus.fbreader.tree.FBTree
import org.geometerplus.zlibrary.core.image.ZLImage
import org.geometerplus.zlibrary.core.util.MimeType

abstract class NetworkTree : FBTree {
    val library: NetworkLibrary

    protected constructor(library: NetworkLibrary) : super() {
        this.library = library
    }

    protected constructor(parent: NetworkTree) : super(parent) {
        this.library = parent.library
    }

    protected constructor(parent: NetworkTree, position: Int) : super(parent, position) {
        this.library = parent.library
    }

    companion object {
        private const val DATA_PREFIX = "data:"

        @JvmStatic
        fun createCoverForItem(library: NetworkLibrary, item: NetworkItem, thumbnail: Boolean): ZLImage? {
            var coverUrl = item.getUrl(if (thumbnail) UrlInfo.Type.Thumbnail else UrlInfo.Type.Image)
            if (coverUrl == null) {
                coverUrl = item.getUrl(if (thumbnail) UrlInfo.Type.Image else UrlInfo.Type.Thumbnail)
            }
            if (coverUrl == null) {
                return null
            }
            return createCoverFromUrl(library, coverUrl, null)
        }

        @JvmStatic
        fun createCoverFromUrl(library: NetworkLibrary, url: String?, mimeType: MimeType?): ZLImage? {
            if (url == null) {
                return null
            }
            var mime = mimeType
            if (mime == null) {
                mime = MimeType.IMAGE_AUTO
            }
            if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("ftp://")) {
                return library.getImageByUrl(url, mime)
            } else if (url.startsWith(DATA_PREFIX)) {
                val commaIndex = url.indexOf(',')
                if (commaIndex == -1) {
                    return null
                }
                if (mime == MimeType.IMAGE_AUTO) {
                    var index = url.indexOf(';')
                    if (index == -1 || index > commaIndex) {
                        index = commaIndex
                    }
                    // string starts with "data:image/"
                    if (url.startsWith(MimeType.IMAGE_PREFIX, DATA_PREFIX.length)) {
                        mime = MimeType.get(url.substring(DATA_PREFIX.length, index))
                    }
                }
                val key = url.indexOf("base64")
                if (key != -1 && key < commaIndex) {
                    return Base64EncodedImage(
                        library, url.substring(commaIndex + 1), mime
                    )
                }
            }
            return null
        }
    }

    override val summary: String
        get() {
            val builder = StringBuilder()
            var count = 0
            for (subtree in subtrees()) {
                if (count++ > 0) {
                    builder.append(",  ")
                }
                builder.append(subtree.name)
                if (count == 5) {
                    break
                }
            }
            return builder.toString()
        }

    open fun getLink(): INetworkLink? {
        val parentTree = parent as? NetworkTree
        return parentTree?.getLink()
    }

    fun removeTrees(trees: MutableSet<NetworkTree>) {
        if (trees.isEmpty() || subtrees().isEmpty()) {
            return
        }
        val toRemove = mutableListOf<FBTree>()
        for (t in subtrees()) {
            if (trees.contains(t)) {
                toRemove.add(t)
                trees.remove(t)
            }
        }
        for (tree in toRemove) {
            tree.removeSelf()
        }
        if (trees.isEmpty()) {
            return
        }

        val toProcess = mutableListOf<FBTree>()
        toProcess.addAll(subtrees())
        while (toProcess.isNotEmpty()) {
            (toProcess.removeAt(toProcess.size - 1) as NetworkTree).removeTrees(trees)
        }
    }
}
