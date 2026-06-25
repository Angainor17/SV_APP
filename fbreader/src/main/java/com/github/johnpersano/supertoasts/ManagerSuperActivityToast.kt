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

import android.app.Activity
import android.os.Handler
import android.os.Message
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import java.util.LinkedList

/**
 * Manages the life of a SuperActivityToast.
 * Handles queueing, showing, and dismissing toasts within an Activity.
 * Initial code derived from the Crouton library.
 *
 * ## Features
 * - Queue-based toast display
 * - Animation support (FADE, FLYIN, SCALE, POPUP)
 * - Activity-specific toast management
 * - Automatic cleanup on activity destruction
 */
class ManagerSuperActivityToast private constructor() : Handler() {

    companion object {
        @Suppress("unused")
        private const val TAG = "ManagerSuperActivityToast"

        @Volatile
        private var instance: ManagerSuperActivityToast? = null

        /**
         * Singleton method to ensure all SuperActivityToasts are passed through the same manager.
         */
        @Synchronized
        internal fun getInstance(): ManagerSuperActivityToast {
            return instance ?: ManagerSuperActivityToast().also { instance = it }
        }
    }

    private val list: LinkedList<SuperActivityToast> = LinkedList()

    /**
     * Add a SuperActivityToast to the list. Will show immediately if no other SuperActivityToasts
     * are in the list.
     */
    internal fun add(superActivityToast: SuperActivityToast) {
        list.add(superActivityToast)
        showNextSuperToast()
    }

    /**
     * Shows the next SuperActivityToast in the list.
     * Called by add() and when the dismiss animation of a previously showing SuperActivityToast ends.
     */
    private fun showNextSuperToast() {
        val superActivityToast = list.peek() ?: return

        if (list.isEmpty() || superActivityToast.getActivity() == null) {
            return
        }

        if (!superActivityToast.isShowing()) {
            val message = obtainMessage(Messages.DISPLAY)
            message.obj = superActivityToast
            sendMessage(message)
        }
    }

    override fun handleMessage(message: Message) {
        val superActivityToast = message.obj as? SuperActivityToast ?: return

        when (message.what) {
            Messages.DISPLAY -> displaySuperToast(superActivityToast)
            Messages.REMOVE -> removeSuperToast(superActivityToast)
            else -> super.handleMessage(message)
        }
    }

    /**
     * Displays a SuperActivityToast.
     */
    private fun displaySuperToast(superActivityToast: SuperActivityToast) {
        if (superActivityToast.isShowing()) {
            return
        }

        val viewGroup = superActivityToast.getViewGroup()
        val toastView = superActivityToast.getView()

        if (viewGroup != null) {
            try {
                viewGroup.addView(toastView)

                if (!superActivityToast.getShowImmediate()) {
                    toastView.startAnimation(getShowAnimation(superActivityToast))
                }
            } catch (e: IllegalStateException) {
                cancelAllSuperActivityToastsForActivity(superActivityToast.getActivity())
                return
            }
        }

        if (!superActivityToast.isIndeterminate()) {
            val message = obtainMessage(Messages.REMOVE)
            message.obj = superActivityToast
            sendMessageDelayed(message, superActivityToast.getDuration() + getShowAnimation(superActivityToast).duration)
        }
    }

    /**
     * Hide and remove the SuperActivityToast.
     */
    internal fun removeSuperToast(superActivityToast: SuperActivityToast) {
        if (!superActivityToast.isShowing()) {
            list.remove(superActivityToast)
            return
        }

        removeMessages(Messages.REMOVE, superActivityToast)

        val viewGroup = superActivityToast.getViewGroup()
        val toastView = superActivityToast.getView()

        if (viewGroup != null) {
            val animation = getDismissAnimation(superActivityToast)

            animation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {}

                override fun onAnimationEnd(animation: Animation) {
                    superActivityToast.getOnDismissWrapper()?.onDismiss(superActivityToast.getView())
                    showNextSuperToast()
                }

                override fun onAnimationRepeat(animation: Animation) {}
            })

            toastView.startAnimation(animation)
            viewGroup.removeView(toastView)
            list.poll()
        }
    }

    /**
     * Removes all SuperActivityToasts and clears the list.
     */
    internal fun cancelAllSuperActivityToasts() {
        removeMessages(Messages.DISPLAY)
        removeMessages(Messages.REMOVE)

        for (superActivityToast in list) {
            if (superActivityToast.isShowing()) {
                superActivityToast.getViewGroup()?.removeView(superActivityToast.getView())
                superActivityToast.getViewGroup()?.invalidate()
            }
        }

        list.clear()
    }

    /**
     * Removes all SuperActivityToasts and clears the list for a specific activity.
     */
    internal fun cancelAllSuperActivityToastsForActivity(activity: Activity?) {
        if (activity == null) return

        val iterator = list.iterator()
        while (iterator.hasNext()) {
            val superActivityToast = iterator.next()
            if (superActivityToast.getActivity() != null && superActivityToast.getActivity() == activity) {
                if (superActivityToast.isShowing()) {
                    superActivityToast.getViewGroup()?.removeView(superActivityToast.getView())
                }
                removeMessages(Messages.DISPLAY, superActivityToast)
                removeMessages(Messages.REMOVE, superActivityToast)
                iterator.remove()
            }
        }
    }

    /**
     * Used in SuperActivityToast saveState().
     */
    internal fun getList(): LinkedList<SuperActivityToast> = list

    /**
     * Returns an animation based on the [SuperToast.Animations] enum.
     */
    private fun getShowAnimation(superActivityToast: SuperActivityToast): Animation {
        return when (superActivityToast.getAnimations()) {
            SuperToast.Animations.FLYIN -> {
                AnimationSet(true).apply {
                    addAnimation(TranslateAnimation(
                        Animation.RELATIVE_TO_SELF, 0.75f, Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f
                    ))
                    addAnimation(AlphaAnimation(0f, 1f))
                    interpolator = DecelerateInterpolator()
                    duration = 250
                }
            }
            SuperToast.Animations.SCALE -> {
                AnimationSet(true).apply {
                    addAnimation(ScaleAnimation(
                        0.9f, 1.0f, 0.9f, 1.0f,
                        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
                    ))
                    addAnimation(AlphaAnimation(0f, 1f))
                    interpolator = DecelerateInterpolator()
                    duration = 250
                }
            }
            SuperToast.Animations.POPUP -> {
                AnimationSet(true).apply {
                    addAnimation(TranslateAnimation(
                        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 0.1f, Animation.RELATIVE_TO_SELF, 0.0f
                    ))
                    addAnimation(AlphaAnimation(0f, 1f))
                    interpolator = DecelerateInterpolator()
                    duration = 250
                }
            }
            else -> {
                AlphaAnimation(0f, 1f).apply {
                    duration = 500
                    interpolator = DecelerateInterpolator()
                }
            }
        }
    }

    /**
     * Returns a dismiss animation based on the [SuperToast.Animations] enum.
     */
    private fun getDismissAnimation(superActivityToast: SuperActivityToast): Animation {
        return when (superActivityToast.getAnimations()) {
            SuperToast.Animations.FLYIN -> {
                AnimationSet(true).apply {
                    addAnimation(TranslateAnimation(
                        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.75f,
                        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f
                    ))
                    addAnimation(AlphaAnimation(1f, 0f))
                    interpolator = AccelerateInterpolator()
                    duration = 250
                }
            }
            SuperToast.Animations.SCALE -> {
                AnimationSet(true).apply {
                    addAnimation(ScaleAnimation(
                        1.0f, 0.9f, 1.0f, 0.9f,
                        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
                    ))
                    addAnimation(AlphaAnimation(1f, 0f))
                    interpolator = DecelerateInterpolator()
                    duration = 250
                }
            }
            SuperToast.Animations.POPUP -> {
                AnimationSet(true).apply {
                    addAnimation(TranslateAnimation(
                        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.1f
                    ))
                    addAnimation(AlphaAnimation(1f, 0f))
                    interpolator = DecelerateInterpolator()
                    duration = 250
                }
            }
            else -> {
                AlphaAnimation(1f, 0f).apply {
                    duration = 500
                    interpolator = AccelerateInterpolator()
                }
            }
        }
    }

    /**
     * Potential messages for the handler to send.
     */
    private object Messages {
        /** Hexadecimal numbers that represent acronyms for the operation. */
        const val DISPLAY = 0x44534154
        const val REMOVE = 0x52534154
    }
}
