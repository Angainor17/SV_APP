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

package org.geometerplus.android.fbreader.covers

import android.app.Activity
import android.graphics.Bitmap
import android.widget.ImageView
import org.geometerplus.fbreader.tree.FBTree
import org.geometerplus.zlibrary.core.image.ZLImage
import org.geometerplus.zlibrary.core.image.ZLImageProxy
import org.geometerplus.zlibrary.ui.android.image.ZLAndroidImageManager
import java.util.concurrent.Executors

class CoverManager(
    private val myActivity: Activity,
    private val myImageSynchronizer: ZLImageProxy.Synchronizer,
    private val myCoverWidth: Int,
    private val myCoverHeight: Int
) {

    internal val cache = CoverCache()
    private val myPool = Executors.newFixedThreadPool(1, MinPriorityThreadFactory())

    fun runOnUiThread(runnable: Runnable) {
        myActivity.runOnUiThread(runnable)
    }

    fun setupCoverView(coverView: ImageView) {
        coverView.layoutParams.width = myCoverWidth
        coverView.layoutParams.height = myCoverHeight
        coverView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        coverView.requestLayout()
    }

    fun getBitmap(image: ZLImage): Bitmap? {
        val mgr = ZLAndroidImageManager.Instance() as ZLAndroidImageManager
        val data = mgr.getImageData(image) ?: return null
        return data.getBitmap(2 * myCoverWidth, 2 * myCoverHeight)
    }

    internal fun setCoverForView(holder: CoverHolder, image: ZLImageProxy) {
        synchronized(holder) {
            try {
                val coverBitmap = cache.getBitmap(holder.key)
                if (coverBitmap != null) {
                    holder.coverView.setImageBitmap(coverBitmap)
                } else if (holder.coverBitmapTask == null) {
                    holder.coverBitmapTask = myPool.submit(holder.CoverBitmapRunnable(image))
                }
            } catch (e: CoverCache.NullObjectException) {
            }
        }
    }

    private fun getHolder(coverView: ImageView, tree: FBTree): CoverHolder {
        var holder = coverView.tag as CoverHolder?
        if (holder == null) {
            holder = CoverHolder(this, coverView, tree.getUniqueKey())
            coverView.tag = holder
        } else {
            holder.setKey(tree.getUniqueKey())
        }
        return holder
    }

    fun trySetCoverImage(coverView: ImageView, tree: FBTree): Boolean {
        val holder = getHolder(coverView, tree)

        var coverBitmap: Bitmap?
        try {
            coverBitmap = cache.getBitmap(holder.key)
        } catch (e: CoverCache.NullObjectException) {
            return false
        }

        if (coverBitmap == null) {
            val cover = tree.getCover()
            if (cover is ZLImageProxy) {
                val img = cover
                if (img.isSynchronized) {
                    setCoverForView(holder, img)
                } else {
                    img.startSynchronization(
                        myImageSynchronizer,
                        holder.CoverSyncRunnable(img)
                    )
                }
            } else if (cover != null) {
                coverBitmap = getBitmap(cover)
            }
        }
        if (coverBitmap != null) {
            holder.coverView.setImageBitmap(coverBitmap)
            return true
        }
        return false
    }

    private class MinPriorityThreadFactory : java.util.concurrent.ThreadFactory {
        private val myDefaultThreadFactory = Executors.defaultThreadFactory()

        override fun newThread(r: Runnable): Thread {
            val th = myDefaultThreadFactory.newThread(r)
            th.priority = Thread.MIN_PRIORITY
            return th
        }
    }
}
