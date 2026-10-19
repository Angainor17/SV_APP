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

package org.geometerplus.android.util

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import org.fbreader.util.Pair
import org.geometerplus.zlibrary.core.application.ZLApplication
import org.geometerplus.zlibrary.core.resources.ZLResource
import java.lang.ref.WeakReference
import java.util.LinkedList
import java.util.Queue

object UIUtil {
    private val ourMonitor = Any()
    private val ourTaskQueue: Queue<Pair<Runnable, String>> = LinkedList()
    private var ourProgress: ProgressDialog? = null
    @Volatile
    private var ourProgressHandler: Handler? = null

    private fun init(): Boolean {
        if (ourProgressHandler != null) {
            return true
        }
        return try {
            ourProgressHandler = ProgressHandler(ourTaskQueue, ourMonitor)
            true
        } catch (t: Throwable) {
            t.printStackTrace()
            false
        }
    }

    @JvmStatic
    fun wait(key: String, param: String, action: Runnable, context: Context) {
        waitInternal(getWaitMessage(key).replace("%s", param), action, context)
    }

    @JvmStatic
    fun wait(key: String, action: Runnable, context: Context) {
        waitInternal(getWaitMessage(key), action, context)
    }

    private fun getWaitMessage(key: String): String =
        ZLResource.resource("dialog").getResource("waitMessage").getResource(key).getValue()

    private fun waitInternal(message: String, action: Runnable, context: Context) {
        if (!init()) {
            action.run()
            return
        }

        synchronized(ourMonitor) {
            ourTaskQueue.offer(Pair(action, message))
            if (ourProgress == null) {
                ourProgress = ProgressDialog.show(context, null, message, true, false)
            } else {
                return
            }
        }
        val currentProgress = ourProgress
        Thread(Runnable {
            while (ourProgress === currentProgress && !ourTaskQueue.isEmpty()) {
                val p = ourTaskQueue.poll()!!
                p.First.run()
                synchronized(ourMonitor) {
                    ourProgressHandler!!.sendEmptyMessage(0)
                    try {
                        (ourMonitor as java.lang.Object).wait()
                    } catch (e: InterruptedException) {
                    }
                }
            }
        }).start()
    }

    @JvmStatic
    fun createExecutor(activity: Activity, key: String): ZLApplication.SynchronousExecutor =
        object : ZLApplication.SynchronousExecutor {
            private val myResource = ZLResource.resource("dialog").getResource("waitMessage")
            private val myMessage = myResource.getResource(key).getValue()
            @Volatile
            private var myProgress: ProgressDialog? = null

            override fun execute(action: Runnable, uiPostAction: Runnable?) {
                activity.runOnUiThread {
                    myProgress = ProgressDialog.show(activity, null, myMessage, true, false)
                    val runner = Thread {
                        action.run()
                        activity.runOnUiThread {
                            try {
                                myProgress!!.dismiss()
                                myProgress = null
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            if (uiPostAction != null) {
                                uiPostAction.run()
                            }
                        }
                    }
                    runner.priority = Thread.MAX_PRIORITY
                    runner.start()
                }
            }

            private fun setMessage(progress: ProgressDialog?, message: String) {
                if (progress == null) {
                    return
                }
                activity.runOnUiThread {
                    progress.setMessage(message)
                }
            }

            override fun executeAux(key: String, runnable: Runnable) {
                setMessage(myProgress, myResource.getResource(key).getValue())
                runnable.run()
                setMessage(myProgress, myMessage)
            }
        }

    private class ProgressHandler(queue: Queue<Pair<Runnable, String>>, monitor: Any) :
        Handler(Looper.getMainLooper()) {

        private val taskQueueRef = WeakReference(queue)
        private val monitorRef = WeakReference(monitor)

        override fun handleMessage(message: Message) {
            try {
                val queue = taskQueueRef.get()
                val monitor = monitorRef.get()
                if (queue == null || monitor == null) return

                synchronized(monitor) {
                    if (queue.isEmpty()) {
                        ourProgress!!.dismiss()
                        ourProgress = null
                    } else {
                        ourProgress!!.setMessage(queue.peek().Second)
                    }
                    (monitor as java.lang.Object).notify()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ourProgress = null
            }
        }
    }
}
