/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.johnpersano.supertoasts.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration

/**
 * A swipe-to-dismiss listener for views.
 * Allows users to dismiss views by swiping them horizontally.
 *
 * Originally from Google's SwipeDismissListView example.
 *
 * ## Usage Example
 * ```kotlin
 * val view = // your view
 * view.setOnTouchListener(SwipeDismissListener(view) { dismissedView ->
 *     // Handle dismiss
 *     (dismissedView.parent as? ViewGroup)?.removeView(dismissedView)
 * })
 * ```
 *
 * ## How it works
 * 1. User touches the view
 * 2. Drags horizontally beyond threshold
 * 3. View animates out and callback is fired
 */
@SuppressLint("NewApi")
@Suppress("unused")
class SwipeDismissListener(
    private val view: View,
    private val callback: OnDismissCallback
) : View.OnTouchListener {

    // Cached ViewConfiguration and system-wide constant values
    private val scaledTouchSlop: Int
    private val minFlingVelocity: Int
    private val maxFlingVelocity: Int
    private val animationTime: Long

    // Transient properties
    private var actionDownXCoordinate: Float = 0f
    private var isSwiping: Boolean = false
    private var velocityTracker: VelocityTracker? = null
    private var translationX: Float = 0f

    init {
        val viewConfiguration = ViewConfiguration.get(view.context)
        scaledTouchSlop = viewConfiguration.scaledTouchSlop
        minFlingVelocity = viewConfiguration.scaledMinimumFlingVelocity
        maxFlingVelocity = viewConfiguration.scaledMaximumFlingVelocity
        animationTime = view.context.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        motionEvent.offsetLocation(translationX, 0f)

        val viewWidth = this.view.width

        when (motionEvent.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                actionDownXCoordinate = motionEvent.rawX
                velocityTracker = VelocityTracker.obtain().also {
                    it.addMovement(motionEvent)
                }
                view.onTouchEvent(motionEvent)
                return true
            }

            MotionEvent.ACTION_UP -> {
                velocityTracker?.let { vt ->
                    val deltaXActionUp = motionEvent.rawX - actionDownXCoordinate
                    vt.addMovement(motionEvent)
                    vt.computeCurrentVelocity(1000)

                    val velocityX = Math.abs(vt.xVelocity)
                    val velocityY = Math.abs(vt.yVelocity)

                    var dismiss = false
                    var dismissRight = false

                    if (Math.abs(deltaXActionUp) > viewWidth / 2) {
                        dismiss = true
                        dismissRight = deltaXActionUp > 0
                    } else if (minFlingVelocity <= velocityX && velocityX <= maxFlingVelocity && velocityY < velocityX) {
                        dismiss = true
                        dismissRight = vt.xVelocity > 0
                    }

                    if (dismiss) {
                        this.view.animate()
                            .translationX((if (dismissRight) viewWidth else -viewWidth).toFloat())
                            .alpha(0f)
                            .setDuration(animationTime)
                            .setListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    performDismiss()
                                }
                            })
                    } else {
                        // User has cancelled action
                        this.view.animate()
                            .translationX(0f)
                            .alpha(1f)
                            .setDuration(animationTime)
                            .setListener(null)
                    }

                    vt.recycle()
                }

                velocityTracker = null
                translationX = 0f
                actionDownXCoordinate = 0f
                isSwiping = false
            }

            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.let { vt ->
                    vt.addMovement(motionEvent)

                    val deltaXActionMove = motionEvent.rawX - actionDownXCoordinate

                    if (Math.abs(deltaXActionMove) > scaledTouchSlop) {
                        isSwiping = true
                        this.view.parent.requestDisallowInterceptTouchEvent(true)

                        // Cancel listview's touch
                        val cancelEvent = MotionEvent.obtain(motionEvent).apply {
                            action = MotionEvent.ACTION_CANCEL or
                                    (motionEvent.actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
                        }
                        this.view.onTouchEvent(cancelEvent)
                        cancelEvent.recycle()
                    }

                    if (isSwiping) {
                        translationX = deltaXActionMove
                        this.view.translationX = deltaXActionMove
                        this.view.alpha = Math.max(
                            0f,
                            Math.min(1f, 1f - 2f * Math.abs(deltaXActionMove) / viewWidth)
                        )
                        return true
                    }
                }
            }
        }

        return false
    }

    private fun performDismiss() {
        val lp = view.layoutParams
        val originalHeight = view.height

        ValueAnimator.ofInt(originalHeight, 1)
            .setDuration(animationTime)
            .apply {
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        callback.onDismiss(view)
                    }
                })

                addUpdateListener { valueAnimator ->
                    lp.height = valueAnimator.animatedValue as Int
                    view.layoutParams = lp
                }
            }
            .start()
    }

    /**
     * Callback interface for dismiss events.
     */
    interface OnDismissCallback {
        /**
         * Called when the view has been swiped away.
         *
         * @param view The view that was dismissed
         */
        fun onDismiss(view: View)
    }
}
