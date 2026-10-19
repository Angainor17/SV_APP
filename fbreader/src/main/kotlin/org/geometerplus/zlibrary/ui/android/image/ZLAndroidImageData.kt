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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.geometerplus.zlibrary.core.image.ZLImageData
import org.geometerplus.zlibrary.core.view.ZLPaintContext

abstract class ZLAndroidImageData : ZLImageData {
    private var myBitmap: Bitmap? = null
    private var myRealWidth: Int = 0
    private var myRealHeight: Int = 0
    private var myLastRequestedSize: ZLPaintContext.Size? = null
    private var myLastRequestedScaling: ZLPaintContext.ScalingType =
        ZLPaintContext.ScalingType.OriginalSize

    protected abstract fun decodeWithOptions(options: BitmapFactory.Options): Bitmap?

    fun getFullSizeBitmap(): Bitmap? =
        getBitmap(null, ZLPaintContext.ScalingType.OriginalSize)

    fun getBitmap(maxWidth: Int, maxHeight: Int): Bitmap? =
        getBitmap(ZLPaintContext.Size(maxWidth, maxHeight), ZLPaintContext.ScalingType.FitMaximum)

    @Synchronized
    fun getBitmap(maxSize0: ZLPaintContext.Size?, scaling: ZLPaintContext.ScalingType): Bitmap? {
        var maxSize = maxSize0
        if (scaling != ZLPaintContext.ScalingType.OriginalSize) {
            if (maxSize == null || maxSize.Width <= 0 || maxSize.Height <= 0) {
                return null
            }
        }
        if (maxSize == null) {
            maxSize = ZLPaintContext.Size(-1, -1)
        }
        if (maxSize != myLastRequestedSize || scaling != myLastRequestedScaling) {
            myLastRequestedSize = maxSize
            myLastRequestedScaling = scaling

            if (myBitmap != null) {
                myBitmap!!.recycle()
                myBitmap = null
            }
            try {
                val options = BitmapFactory.Options()
                if (myRealWidth <= 0) {
                    options.inJustDecodeBounds = true
                    decodeWithOptions(options)
                    myRealWidth = options.outWidth
                    myRealHeight = options.outHeight
                }
                options.inJustDecodeBounds = false
                var coefficient = 1
                if (scaling == ZLPaintContext.ScalingType.IntegerCoefficient) {
                    if (myRealHeight > maxSize.Height || myRealWidth > maxSize.Width) {
                        coefficient = 1 + maxOf(
                            (myRealHeight - 1) / maxSize.Height,
                            (myRealWidth - 1) / maxSize.Width,
                        )
                    }
                }
                options.inSampleSize = coefficient
                val bitmap = decodeWithOptions(options)
                myBitmap = bitmap
                if (bitmap != null) {
                    when (scaling) {
                        ZLPaintContext.ScalingType.OriginalSize -> {
                        }
                        ZLPaintContext.ScalingType.FitMaximum -> {
                            val bWidth = bitmap.width
                            val bHeight = bitmap.height
                            if (bWidth > 0 && bHeight > 0 &&
                                bWidth != maxSize.Width && bHeight != maxSize.Height
                            ) {
                                val w: Int
                                val h: Int
                                if (bWidth * maxSize.Height > bHeight * maxSize.Width) {
                                    w = maxSize.Width
                                    h = maxOf(1, bHeight * w / bWidth)
                                } else {
                                    h = maxSize.Height
                                    w = maxOf(1, bWidth * h / bHeight)
                                }
                                val scaled = Bitmap.createScaledBitmap(bitmap, w, h, false)
                                if (scaled != null) {
                                    myBitmap = scaled
                                }
                            }
                        }
                        ZLPaintContext.ScalingType.IntegerCoefficient -> {
                            val bWidth = bitmap.width
                            val bHeight = bitmap.height
                            if (bWidth > 0 && bHeight > 0 &&
                                (bWidth > maxSize.Width || bHeight > maxSize.Height)
                            ) {
                                val w: Int
                                val h: Int
                                if (bWidth * maxSize.Height > bHeight * maxSize.Width) {
                                    w = maxSize.Width
                                    h = maxOf(1, bHeight * w / bWidth)
                                } else {
                                    h = maxSize.Height
                                    w = maxOf(1, bWidth * h / bHeight)
                                }
                                val scaled = Bitmap.createScaledBitmap(bitmap, w, h, false)
                                if (scaled != null) {
                                    myBitmap = scaled
                                }
                            }
                        }
                    }
                }
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
            }
        }
        return myBitmap
    }
}
