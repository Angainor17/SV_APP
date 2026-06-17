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

import android.widget.ImageView
import org.geometerplus.fbreader.tree.FBTree
import org.geometerplus.zlibrary.core.image.ZLImageProxy
import java.util.concurrent.Future

internal class CoverHolder(
    private val myManager: CoverManager,
    val coverView: ImageView,
    @Volatile
    private var myKey: FBTree.Key
) {
    val key: FBTree.Key
        get() = myKey

    var coverBitmapTask: Future<*>? = null
    private var coverSyncRunnable: CoverSyncRunnable? = null
    private var coverBitmapRunnable: Runnable? = null

    init {
        myManager.setupCoverView(coverView)
        myManager.cache.holdersCounter++
    }

    @Synchronized
    fun setKey(key: FBTree.Key) {
        if (myKey != key) {
            coverBitmapTask?.cancel(true)
            coverBitmapTask = null
        }
        coverBitmapRunnable = null
        myKey = key
    }

    internal inner class CoverSyncRunnable(private val myImage: ZLImageProxy) : Runnable {
        private val myKey: FBTree.Key

        init {
            synchronized(this@CoverHolder) {
                myKey = key
                coverSyncRunnable = this
            }
        }

        override fun run() {
            synchronized(this@CoverHolder) {
                try {
                    if (coverSyncRunnable !== this) return
                    if (key != myKey) return
                    if (!myImage.isSynchronized) return
                    myManager.runOnUiThread {
                        synchronized(this@CoverHolder) {
                            if (key == myKey) {
                                myManager.setCoverForView(this@CoverHolder, myImage)
                            }
                        }
                    }
                } finally {
                    if (coverSyncRunnable === this) {
                        coverSyncRunnable = null
                    }
                }
            }
        }
    }

    internal inner class CoverBitmapRunnable(private val myImage: ZLImageProxy) : Runnable {
        private val myKey: FBTree.Key

        init {
            synchronized(this@CoverHolder) {
                myKey = key
                coverBitmapRunnable = this
            }
        }

        override fun run() {
            synchronized(this@CoverHolder) {
                if (coverBitmapRunnable !== this) return
            }
            try {
                if (!myImage.isSynchronized) return
                val coverBitmap = myManager.getBitmap(myImage)
                if (coverBitmap == null) {
                    // If bitmap is null, then there's no image
                    // and CoverView already has a stock image
                    myManager.cache.putBitmap(myKey, null)
                    return
                }
                if (Thread.currentThread().isInterrupted) {
                    // We have been cancelled
                    return
                }
                myManager.cache.putBitmap(myKey, coverBitmap)
                myManager.runOnUiThread {
                    synchronized(this@CoverHolder) {
                        if (key == myKey) {
                            coverView.setImageBitmap(coverBitmap)
                        }
                    }
                }
            } finally {
                synchronized(this@CoverHolder) {
                    if (coverBitmapRunnable === this) {
                        coverBitmapRunnable = null
                        coverBitmapTask = null
                    }
                }
            }
        }
    }
}
