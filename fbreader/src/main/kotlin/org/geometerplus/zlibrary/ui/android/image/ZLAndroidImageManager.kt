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

package org.geometerplus.zlibrary.ui.android.image

import org.geometerplus.zlibrary.core.image.ZLImage
import org.geometerplus.zlibrary.core.image.ZLImageManager
import org.geometerplus.zlibrary.core.image.ZLImageProxy
import org.geometerplus.zlibrary.core.image.ZLStreamImage

class ZLAndroidImageManager : ZLImageManager() {
    private var myLoader: ZLAndroidImageLoader? = null

    override fun getImageData(image: ZLImage): ZLAndroidImageData? = when {
        image is ZLImageProxy -> image.realImage?.let { getImageData(it) }
        image is ZLStreamImage -> InputStreamImageData(image)
        image is ZLBitmapImage -> BitmapImageData.get(image)
        // unknown image type or null
        else -> null
    }

    fun startImageLoading(
        synchronizer: ZLImageProxy.Synchronizer,
        image: ZLImageProxy,
        postLoadingRunnable: Runnable?,
    ) {
        if (myLoader == null) {
            myLoader = ZLAndroidImageLoader()
        }
        myLoader!!.startImageLoading(synchronizer, image, postLoadingRunnable)
    }
}
