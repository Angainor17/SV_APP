package com.github.axet.bookreader.widgets

import android.animation.TimeAnimator
import android.animation.ValueAnimator
import android.os.Handler

/**
 * Совместимый TimeAnimator для старых версий Android.
 */
class TimeAnimatorCompat {
    private val handler = Handler()
    private var listener: TimeListener? = null
    private val v: ValueAnimator = TimeAnimator()

    private val run = object : Runnable {
        override fun run() {
            listener?.onTimeUpdate(this@TimeAnimatorCompat, 0, 0)
            handler.postDelayed(this, 1000L / 24) // 24 FPS
        }
    }

    fun start() {
        v.start()
    }

    fun cancel() {
        v.cancel()
    }

    fun setTimeListener(l: TimeListener?) {
        (v as TimeAnimator).setTimeListener { animation, totalTime, deltaTime ->
            listener?.onTimeUpdate(this@TimeAnimatorCompat, totalTime, deltaTime)
        }
        listener = l
    }

    /**
     * Интерфейс для получения обновлений времени.
     */
    interface TimeListener {
        fun onTimeUpdate(animation: TimeAnimatorCompat, totalTime: Long, deltaTime: Long)
    }
}
