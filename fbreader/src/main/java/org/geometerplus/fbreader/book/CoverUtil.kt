/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.fbreader.book

import org.geometerplus.fbreader.formats.IFormatPluginCollection
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.image.ZLImage
import java.lang.ref.WeakReference
import java.util.WeakHashMap

object CoverUtil {
    private val NULL_IMAGE = WeakReference<ZLImage>(null)
    private val ourCovers = WeakHashMap<ZLFile, WeakReference<ZLImage>>()

    @JvmStatic
    fun getCover(book: AbstractBook?, collection: IFormatPluginCollection): ZLImage? {
        if (book == null) {
            return null
        }
        return synchronized(book) {
            getCover(ZLFile.createFileByPath(book.getPath()), collection)
        }
    }

    @JvmStatic
    fun getCover(file: ZLFile, collection: IFormatPluginCollection): ZLImage? {
        val cover = ourCovers[file]
        if (cover === NULL_IMAGE) {
            return null
        } else if (cover != null) {
            val image = cover.get()
            if (image != null) {
                return image
            }
        }
        var image: ZLImage? = null
        try {
            image = collection.getPlugin(file)!!.readCover(file)
        } catch (e: Exception) {
            // ignore
        }
        ourCovers[file] = if (image != null) WeakReference(image) else NULL_IMAGE
        return image
    }
}
