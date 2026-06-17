package org.geometerplus.android.util

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.os.Handler
import android.os.Message
import org.fbreader.util.Pair
import org.geometerplus.zlibrary.core.application.ZLApplication
import org.geometerplus.zlibrary.core.resources.ZLResource
import java.util.LinkedList

@Suppress("DEPRECATION")
object UIUtil {
    private val ourMonitor = Any()
    private val ourTaskQueue = LinkedList<Pair<Runnable, String>>()
    private var ourProgress: ProgressDialog? = null
    @Volatile
    private var ourProgressHandler: Handler? = null

    private fun init(): Boolean {
        if (ourProgressHandler != null) {
            return true
        }
        return try {
            ourProgressHandler = object : Handler() {
                override fun handleMessage(message: Message) {
                    try {
                        synchronized(ourMonitor) {
                            if (ourTaskQueue.isEmpty()) {
                                ourProgress?.dismiss()
                                ourProgress = null
                            } else {
                                ourProgress?.setMessage(ourTaskQueue.peek()?.Second)
                            }
                            (ourMonitor as Object).notify()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        ourProgress = null
                    }
                }
            }
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
        ZLResource.resource("dialog").getResource("waitMessage").getResource(key).value

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
        Thread {
            while (ourProgress == currentProgress && ourTaskQueue.isNotEmpty()) {
                val p = ourTaskQueue.poll() ?: break
                p.First.run()
                synchronized(ourMonitor) {
                    ourProgressHandler?.sendEmptyMessage(0)
                    try {
                        (ourMonitor as Object).wait()
                    } catch (e: InterruptedException) {
                    }
                }
            }
        }.start()
    }

    @JvmStatic
    fun createExecutor(activity: Activity, key: String): ZLApplication.SynchronousExecutor {
        return object : ZLApplication.SynchronousExecutor {
            private val myResource = ZLResource.resource("dialog").getResource("waitMessage")
            private val myMessage = myResource.getResource(key).value
            @Volatile
            private var myProgress: ProgressDialog? = null

            override fun execute(action: Runnable, uiPostAction: Runnable?) {
                activity.runOnUiThread {
                    myProgress = ProgressDialog.show(activity, null, myMessage, true, false)
                    val runner = object : Thread() {
                        override fun run() {
                            action.run()
                            activity.runOnUiThread {
                                try {
                                    myProgress?.dismiss()
                                    myProgress = null
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                uiPostAction?.run()
                            }
                        }
                    }
                    runner.priority = Thread.MAX_PRIORITY
                    runner.start()
                }
            }

            override fun executeAux(key: String, runnable: Runnable) {
                setMessage(myProgress, myResource.getResource(key).value)
                runnable.run()
                setMessage(myProgress, myMessage)
            }

            private fun setMessage(progress: ProgressDialog?, message: String) {
                if (progress == null) {
                    return
                }
                activity.runOnUiThread {
                    progress.setMessage(message)
                }
            }
        }
    }
}
