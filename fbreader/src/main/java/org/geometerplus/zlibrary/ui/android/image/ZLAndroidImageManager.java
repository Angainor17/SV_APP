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

package org.geometerplus.zlibrary.ui.android.image;

import org.geometerplus.zlibrary.core.image.ZLImage;
import org.geometerplus.zlibrary.core.image.ZLImageManager;
import org.geometerplus.zlibrary.core.image.ZLImageProxy;
import org.geometerplus.zlibrary.core.image.ZLStreamImage;

public final class ZLAndroidImageManager extends ZLImageManager {
    private ZLAndroidImageLoader myLoader;

    @Override
    public ZLAndroidImageData getImageData(ZLImage image) {
        if (image instanceof ZLImageProxy) {
            return getImageData(((ZLImageProxy) image).getRealImage());
        } else if (image instanceof ZLStreamImage) {
            return new InputStreamImageData((ZLStreamImage) image);
        } else if (image instanceof ZLBitmapImage) {
            return BitmapImageData.get((ZLBitmapImage) image);
        } else {
            // unknown image type or null
            return null;
        }
    }

    public void startImageLoading(ZLImageProxy.Synchronizer syncronizer, ZLImageProxy image, Runnable postLoadingRunnable) {
        if (myLoader == null) {
            myLoader = new ZLAndroidImageLoader();
        }
        myLoader.startImageLoading(syncronizer, image, postLoadingRunnable);
    }
}
