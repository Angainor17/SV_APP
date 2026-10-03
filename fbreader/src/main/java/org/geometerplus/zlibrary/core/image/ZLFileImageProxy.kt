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

package org.geometerplus.zlibrary.core.image

import org.geometerplus.zlibrary.core.filesystem.ZLFile

abstract class ZLFileImageProxy protected constructor(
    @JvmField protected val File: ZLFile,
) : ZLImageSimpleProxy() {

    @Volatile
    private var myImage: ZLFileImage? = null

    override val realImage: ZLFileImage?
        get() = myImage

    override fun getURI(): String = "cover:" + File.getPath()

    @Synchronized
    override fun synchronize() {
        if (myImage == null) {
            myImage = retrieveRealImage()
            setSynchronized()
        }
    }

    override fun sourceType(): ZLImageProxy.SourceType = ZLImageProxy.SourceType.FILE

    override val id: String
        get() = File.getPath()

    protected abstract fun retrieveRealImage(): ZLFileImage
}
