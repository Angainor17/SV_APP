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

package org.geometerplus.zlibrary.ui.android.image

import android.os.Handler
import android.os.Looper
import android.os.Message
import org.geometerplus.zlibrary.core.image.ZLImageProxy
import java.lang.ref.WeakReference
import java.util.HashMap
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

class ZLAndroidImageLoader {
    private val myPool: ExecutorService =
        Executors.newFixedThreadPool(IMAGE_LOADING_THREADS_NUMBER, MinPriorityThreadFactory())
    private val mySinglePool: ExecutorService =
        Executors.newFixedThreadPool(1, MinPriorityThreadFactory())
    private val myOnImageSyncRunnables = HashMap<String, LinkedList<Runnable>>()
    private val myImageSynchronizedHandler = ImageSynchronizedHandler(this)

    fun startImageLoading(
        synchronizer: ZLImageProxy.Synchronizer,
        image: ZLImageProxy,
        postAction: Runnable?,
    ) {
        synchronized(myOnImageSyncRunnables) {
            var runnables = myOnImageSyncRunnables[image.id]
            if (runnables != null) {
                if (postAction != null && !runnables.contains(postAction)) {
                    runnables.add(postAction)
                }
                return
            }

            runnables = LinkedList()
            if (postAction != null) {
                runnables.add(postAction)
            }
            myOnImageSyncRunnables[image.id] = runnables
        }

        val pool =
            if (image.sourceType() == ZLImageProxy.SourceType.FILE) mySinglePool else myPool
        pool.execute {
            synchronizer.synchronize(image) {
                myImageSynchronizedHandler.fireMessage(image.id)
            }
        }
    }

    private class MinPriorityThreadFactory : ThreadFactory {
        private val myDefaultThreadFactory = Executors.defaultThreadFactory()

        override fun newThread(r: Runnable): Thread {
            val th = myDefaultThreadFactory.newThread(r)
            th.priority = Thread.MIN_PRIORITY
            return th
        }
    }

    private class ImageSynchronizedHandler(loader: ZLAndroidImageLoader) : Handler(Looper.getMainLooper()) {
        private val loaderRef = WeakReference(loader)

        override fun handleMessage(message: Message) {
            val imageUrl = message.obj as String
            val loader = loaderRef.get() ?: return

            val runnables: LinkedList<Runnable>? = synchronized(loader.myOnImageSyncRunnables) {
                loader.myOnImageSyncRunnables.remove(imageUrl)
            }
            if (runnables != null) {
                for (runnable in runnables) {
                    runnable.run()
                }
            }
        }

        fun fireMessage(imageUrl: String) {
            sendMessage(obtainMessage(0, imageUrl))
        }
    }

    companion object {
        private const val IMAGE_LOADING_THREADS_NUMBER = 3 // TODO: how many threads ???
    }
}
