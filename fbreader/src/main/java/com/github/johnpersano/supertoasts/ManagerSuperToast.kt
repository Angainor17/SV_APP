/**
 * Copyright 2014 John Persano
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.johnpersano.supertoasts

import android.os.Handler
import android.os.Message
import java.util.Queue
import java.util.concurrent.LinkedBlockingQueue

/**
 * Manages the life of a SuperToast.
 * Uses a queue to display toasts one at a time.
 * Initially copied from the Crouton library.
 *
 * ## How it works
 * - Maintains a queue of pending SuperToasts
 * - Shows one toast at a time
 * - Automatically shows next toast when current one is dismissed
 */
class ManagerSuperToast private constructor() : Handler() {

    companion object {
        @Suppress("unused")
        private const val TAG = "ManagerSuperToast"

        @Volatile
        private var instance: ManagerSuperToast? = null

        /**
         * Singleton method to ensure all SuperToasts are passed through the same manager.
         */
        @Synchronized
        internal fun getInstance(): ManagerSuperToast {
            return instance ?: ManagerSuperToast().also { instance = it }
        }
    }

    private val queue: Queue<SuperToast> = LinkedBlockingQueue()

    /**
     * Add SuperToast to queue and try to show it.
     */
    internal fun add(superToast: SuperToast) {
        queue.add(superToast)
        showNextSuperToast()
    }

    /**
     * Shows the next SuperToast in the list.
     */
    private fun showNextSuperToast() {
        if (queue.isEmpty()) {
            return
        }

        val superToast = queue.peek() ?: return

        if (!superToast.isShowing()) {
            val message = obtainMessage(Messages.ADD_SUPERTOAST)
            message.obj = superToast
            sendMessage(message)
        } else {
            sendMessageDelayed(superToast, Messages.DISPLAY_SUPERTOAST, getDuration(superToast))
        }
    }

    /**
     * Show/dismiss a SuperToast after a specific duration.
     */
    private fun sendMessageDelayed(superToast: SuperToast, messageId: Int, delay: Long) {
        val message = obtainMessage(messageId)
        message.obj = superToast
        sendMessageDelayed(message, delay)
    }

    /**
     * Get duration and add one second to compensate for show/hide animations.
     */
    private fun getDuration(superToast: SuperToast): Long {
        return superToast.getDuration() + 1000L
    }

    override fun handleMessage(message: Message) {
        val superToast = message.obj as? SuperToast ?: return

        when (message.what) {
            Messages.DISPLAY_SUPERTOAST -> showNextSuperToast()
            Messages.ADD_SUPERTOAST -> displaySuperToast(superToast)
            Messages.REMOVE_SUPERTOAST -> removeSuperToast(superToast)
            else -> super.handleMessage(message)
        }
    }

    /**
     * Displays a SuperToast.
     */
    private fun displaySuperToast(superToast: SuperToast) {
        if (superToast.isShowing()) {
            return
        }

        val windowManager = superToast.windowManager
        val toastView = superToast.view
        val params = superToast.windowManagerParams

        if (windowManager != null && params != null) {
            windowManager.addView(toastView, params)
        }

        sendMessageDelayed(superToast, Messages.REMOVE_SUPERTOAST, superToast.getDuration() + 500L)
    }

    /**
     * Hide and remove the SuperToast.
     */
    internal fun removeSuperToast(superToast: SuperToast) {
        val windowManager = superToast.windowManager
        val toastView = superToast.view

        if (windowManager != null) {
            queue.poll()
            windowManager.removeView(toastView)
            sendMessageDelayed(superToast, Messages.DISPLAY_SUPERTOAST, 500L)

            superToast.onDismissListener?.onDismiss(superToast.view)
        }
    }

    /**
     * Cancels/removes all showing pending SuperToasts.
     */
    internal fun cancelAllSuperToasts() {
        removeMessages(Messages.ADD_SUPERTOAST)
        removeMessages(Messages.DISPLAY_SUPERTOAST)
        removeMessages(Messages.REMOVE_SUPERTOAST)

        for (superToast in queue) {
            if (superToast.isShowing()) {
                superToast.windowManager?.removeView(superToast.view)
            }
        }

        queue.clear()
    }

    /**
     * Potential messages for the handler to send.
     */
    private object Messages {
        /** Hexadecimal numbers that represent acronyms for the operation. */
        const val DISPLAY_SUPERTOAST = 0x445354
        const val ADD_SUPERTOAST = 0x415354
        const val REMOVE_SUPERTOAST = 0x525354
    }
}
