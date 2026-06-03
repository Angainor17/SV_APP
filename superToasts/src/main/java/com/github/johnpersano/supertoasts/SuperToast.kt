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

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.github.johnpersano.R
import com.github.johnpersano.supertoasts.util.Style

/**
 * SuperToasts are designed to replace stock Android Toasts.
 * If you need to display a SuperToast inside of an Activity
 * please see [SuperActivityToast].
 *
 * ## Usage Example
 * ```kotlin
 * // Simple toast
 * SuperToast.create(context, "Hello World", SuperToast.Duration.SHORT).show()
 *
 * // With style
 * val toast = SuperToast(context, Style.getStyle(Style.BLUE))
 * toast.setText("Styled toast")
 * toast.show()
 *
 * // Cancel all toasts
 * SuperToast.cancelAllSuperToasts()
 * ```
 */
@Suppress("unused")
class SuperToast {

    companion object {
        private const val TAG = "SuperToast"
        private const val ERROR_CONTEXTNULL = " - You cannot use a null context."

        /**
         * Dismisses and removes all showing/pending SuperToasts.
         */
        @JvmStatic
        fun cancelAllSuperToasts() {
            ManagerSuperToast.getInstance().cancelAllSuperToasts()
        }

        /**
         * Returns a standard SuperToast.
         *
         * @param context [Context]
         * @param textCharSequence [CharSequence]
         * @param durationInteger [Duration] constant
         * @return [SuperToast]
         */
        @JvmStatic
        fun create(context: Context, textCharSequence: CharSequence, durationInteger: Int): SuperToast {
            val superToast = SuperToast(context)
            superToast.setText(textCharSequence)
            superToast.setDuration(durationInteger)
            return superToast
        }

        /**
         * Returns a standard SuperToast with specified animations.
         *
         * @param context [Context]
         * @param textCharSequence [CharSequence]
         * @param durationInteger [Duration] constant
         * @param animations [Animations]
         * @return [SuperToast]
         */
        @JvmStatic
        fun create(
            context: Context,
            textCharSequence: CharSequence,
            durationInteger: Int,
            animations: Animations
        ): SuperToast {
            val superToast = SuperToast(context)
            superToast.setText(textCharSequence)
            superToast.setDuration(durationInteger)
            superToast.setAnimations(animations)
            return superToast
        }

        /**
         * Returns a SuperToast with a specified style.
         *
         * @param context [Context]
         * @param textCharSequence [CharSequence]
         * @param durationInteger [Duration] constant
         * @param style [Style]
         * @return [SuperToast]
         */
        @JvmStatic
        fun create(
            context: Context,
            textCharSequence: CharSequence,
            durationInteger: Int,
            style: Style
        ): SuperToast {
            val superToast = SuperToast(context)
            superToast.setText(textCharSequence)
            superToast.setDuration(durationInteger)
            superToast.setStyle(style)
            return superToast
        }
    }

    internal var animations: Animations = Animations.FADE
    private val context: Context
    private var gravity: Int = Gravity.BOTTOM or Gravity.CENTER
    internal var duration: Int = Duration.SHORT
    private var typefaceStyle: Int = 0
    private var background: Int = 0
    private var xOffset: Int = 0
    private var yOffset: Int = 0
    private val rootLayout: LinearLayout
    internal var onDismissListener: OnDismissListener? = null
    private val messageTextView: TextView
    internal val toastView: View
    internal val windowManager: WindowManager
    internal var windowManagerParams: WindowManager.LayoutParams? = null

    /**
     * Instantiates a new SuperToast.
     *
     * @param context [Context]
     */
    constructor(context: Context) {
        if (context == null) {
            throw IllegalArgumentException(TAG + ERROR_CONTEXTNULL)
        }

        this.context = context

        yOffset = context.resources.getDimensionPixelSize(R.dimen.toast_hover)

        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        toastView = layoutInflater.inflate(R.layout.supertoast, null)

        windowManager = toastView.context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        rootLayout = toastView.findViewById(R.id.root_layout)

        messageTextView = toastView.findViewById(R.id.message_textview)
    }

    /**
     * Instantiates a new SuperToast with a specified style.
     *
     * @param context [Context]
     * @param style [Style]
     */
    constructor(context: Context, style: Style) : this(context) {
        setStyle(style)
    }

    /**
     * Shows the SuperToast. If another SuperToast is showing than
     * this one will be added to a queue and shown when the previous SuperToast
     * is dismissed.
     */
    fun show() {
        val params = WindowManager.LayoutParams()
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        params.format = PixelFormat.TRANSLUCENT
        params.windowAnimations = getAnimation()
        params.type = WindowManager.LayoutParams.TYPE_TOAST
        params.gravity = gravity
        params.x = xOffset
        params.y = yOffset
        windowManagerParams = params

        ManagerSuperToast.getInstance().add(this)
    }

    /**
     * Returns the message text of the SuperToast.
     *
     * @return [CharSequence]
     */
    fun getText(): CharSequence = messageTextView.text

    /**
     * Sets the message text of the SuperToast.
     *
     * @param text [CharSequence]
     */
    fun setText(text: CharSequence) {
        messageTextView.text = text
    }

    /**
     * Returns the message typeface style of the SuperToast.
     *
     * @return [android.graphics.Typeface] int
     */
    fun getTypefaceStyle(): Int = typefaceStyle

    /**
     * Sets the message typeface style of the SuperToast.
     *
     * @param typeface [android.graphics.Typeface] int
     */
    fun setTypefaceStyle(typeface: Int) {
        this.typefaceStyle = typeface
        messageTextView.setTypeface(messageTextView.typeface, typeface)
    }

    /**
     * Returns the message text color of the SuperToast.
     *
     * @return int
     */
    fun getTextColor(): Int = messageTextView.currentTextColor

    /**
     * Sets the message text color of the SuperToast.
     *
     * @param textColor [android.graphics.Color]
     */
    fun setTextColor(textColor: Int) {
        messageTextView.setTextColor(textColor)
    }

    /**
     * Returns the text size of the SuperToast message in pixels.
     *
     * @return float
     */
    fun getTextSize(): Float = messageTextView.textSize

    /**
     * Sets the text size of the SuperToast message.
     *
     * @param textSize int
     */
    fun setTextSize(textSize: Int) {
        messageTextView.textSize = textSize.toFloat()
    }

    /**
     * Returns the duration of the SuperToast.
     *
     * @return int
     */
    fun getDuration(): Int = this.duration

    /**
     * Sets the duration that the SuperToast will show.
     *
     * @param duration [Duration] constant
     */
    fun setDuration(duration: Int) {
        this.duration = duration
    }

    /**
     * Sets an icon resource to the SuperToast with a specified position.
     *
     * @param iconResource [Icon] constant
     * @param iconPosition [IconPosition]
     */
    fun setIcon(iconResource: Int, iconPosition: IconPosition) {
        val icon = context.resources.getDrawable(iconResource)
        when (iconPosition) {
            IconPosition.BOTTOM -> messageTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, icon)
            IconPosition.LEFT -> messageTextView.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
            IconPosition.RIGHT -> messageTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, icon, null)
            IconPosition.TOP -> messageTextView.setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null)
        }
    }

    /**
     * Returns the background resource of the SuperToast.
     *
     * @return int
     */
    fun getBackground(): Int = this.background

    /**
     * Sets the background resource of the SuperToast.
     *
     * @param background [Background] constant
     */
    fun setBackground(background: Int) {
        this.background = background
        rootLayout.setBackgroundResource(background)
    }

    /**
     * Sets the gravity of the SuperToast along with x and y offsets.
     *
     * @param gravity [Gravity] int
     * @param xOffset int
     * @param yOffset int
     */
    fun setGravity(gravity: Int, xOffset: Int, yOffset: Int) {
        this.gravity = gravity
        this.xOffset = xOffset
        this.yOffset = yOffset
    }

    /**
     * Returns the show/hide animations of the SuperToast.
     *
     * @return [Animations]
     */
    fun getAnimations(): Animations = this.animations

    /**
     * Sets the show/hide animations of the SuperToast.
     *
     * @param animations [Animations]
     */
    fun setAnimations(animations: Animations) {
        this.animations = animations
    }

    /**
     * Returns the OnDismissListener set to the SuperToast.
     *
     * @return [OnDismissListener]
     */
    fun getOnDismissListener(): OnDismissListener? = onDismissListener

    /**
     * Sets an OnDismissListener defined in this library
     * to the SuperToast. Does not require wrapper.
     *
     * @param onDismissListener [OnDismissListener]
     */
    fun setOnDismissListener(onDismissListener: OnDismissListener) {
        this.onDismissListener = onDismissListener
    }

    /**
     * Dismisses the SuperToast.
     */
    fun dismiss() {
        ManagerSuperToast.getInstance().removeSuperToast(this)
    }

    /**
     * Returns the SuperToast message textview.
     *
     * @return [TextView]
     */
    fun getTextView(): TextView = messageTextView

    /**
     * Returns the SuperToast view.
     *
     * @return [View]
     */
    fun getView(): View = toastView

    /** Internal property for accessing the view directly */
    internal val view: View
        get() = toastView

    /**
     * Returns true if the SuperToast is showing.
     *
     * @return boolean
     */
    fun isShowing(): Boolean = toastView != null && toastView.isShown

    /**
     * Returns the window manager that the SuperToast is attached to.
     *
     * @return [WindowManager]
     */
    fun getWindowManager(): WindowManager = windowManager

    /**
     * Returns the window manager layout params of the SuperToast.
     *
     * @return [WindowManager.LayoutParams]
     */
    fun getWindowManagerParams(): WindowManager.LayoutParams? = windowManagerParams

    /**
     * Private method used to return a specific animation for a animations enum
     */
    private fun getAnimation(): Int {
        return when (animations) {
            Animations.FLYIN -> android.R.style.Animation_Translucent
            Animations.SCALE -> android.R.style.Animation_Dialog
            Animations.POPUP -> android.R.style.Animation_InputMethod
            else -> android.R.style.Animation_Toast
        }
    }

    /**
     * Private method used to set a default style to the SuperToast
     */
    private fun setStyle(style: Style) {
        this.setAnimations(style.animations)
        this.setTypefaceStyle(style.typefaceStyle)
        this.setTextColor(style.textColor)
        this.setBackground(style.background)
    }

    /**
     * Animations for all types of SuperToasts.
     */
    enum class Animations {
        FADE,
        FLYIN,
        SCALE,
        POPUP
    }

    /**
     * Types for SuperActivityToasts and SuperCardToasts.
     */
    enum class Type {
        /** Standard type used for displaying messages. */
        STANDARD,
        /** Progress type used for showing progress. */
        PROGRESS,
        /** Progress type used for showing progress. */
        PROGRESS_HORIZONTAL,
        /** Button type used for receiving click actions. */
        BUTTON
    }

    /**
     * Positions for icons used in all types of SuperToasts.
     */
    enum class IconPosition {
        /** Set the icon to the left of the text. */
        LEFT,
        /** Set the icon to the right of the text. */
        RIGHT,
        /** Set the icon on top of the text. */
        TOP,
        /** Set the icon on the bottom of the text. */
        BOTTOM
    }

    /**
     * Custom OnClickListener to be used with SuperActivityToasts/SuperCardToasts. Note that
     * SuperActivityToasts/SuperCardToasts must use this with an
     * [com.github.johnpersano.supertoasts.util.OnClickWrapper]
     */
    interface OnClickListener {
        fun onClick(view: View, token: android.os.Parcelable?)
    }

    /**
     * Custom OnDismissListener to be used with any type of SuperToasts. Note that
     * SuperActivityToasts/SuperCardToasts must use this with an
     * [com.github.johnpersano.supertoasts.util.OnDismissWrapper]
     */
    interface OnDismissListener {
        fun onDismiss(view: View)
    }

    /**
     * Backgrounds for all types of SuperToasts.
     */
    class Background {
        companion object {
            @JvmField val BLACK: Int = Style.getBackground(Style.BLACK)
            @JvmField val BLUE: Int = Style.getBackground(Style.BLUE)
            @JvmField val GRAY: Int = Style.getBackground(Style.GRAY)
            @JvmField val GREEN: Int = Style.getBackground(Style.GREEN)
            @JvmField val ORANGE: Int = Style.getBackground(Style.ORANGE)
            @JvmField val PURPLE: Int = Style.getBackground(Style.PURPLE)
            @JvmField val RED: Int = Style.getBackground(Style.RED)
            @JvmField val WHITE: Int = Style.getBackground(Style.WHITE)
        }
    }

    /**
     * Icons for all types of SuperToasts.
     */
    class Icon {
        /**
         * Icons for all types of SuperToasts with a dark background.
         */
        class Dark {
            companion object {
                @JvmField val EDIT: Int = R.drawable.icon_dark_edit
                @JvmField val EXIT: Int = R.drawable.icon_dark_exit
                @JvmField val INFO: Int = R.drawable.icon_dark_info
                @JvmField val REDO: Int = R.drawable.icon_dark_redo
                @JvmField val REFRESH: Int = R.drawable.icon_dark_refresh
                @JvmField val SAVE: Int = R.drawable.icon_dark_save
                @JvmField val SHARE: Int = R.drawable.icon_dark_share
                @JvmField val UNDO: Int = R.drawable.icon_dark_undo
            }
        }

        /**
         * Icons for all types of SuperToasts with a light background.
         */
        class Light {
            companion object {
                @JvmField val EDIT: Int = R.drawable.icon_light_edit
                @JvmField val EXIT: Int = R.drawable.icon_light_exit
                @JvmField val INFO: Int = R.drawable.icon_light_info
                @JvmField val REDO: Int = R.drawable.icon_light_redo
                @JvmField val REFRESH: Int = R.drawable.icon_light_refresh
                @JvmField val SAVE: Int = R.drawable.icon_light_save
                @JvmField val SHARE: Int = R.drawable.icon_light_share
                @JvmField val UNDO: Int = R.drawable.icon_light_undo
            }
        }
    }

    /**
     * Durations for all types of SuperToasts.
     */
    class Duration {
        companion object {
            @JvmField val VERY_SHORT: Int = 1500
            @JvmField val SHORT: Int = 2000
            @JvmField val MEDIUM: Int = 2750
            @JvmField val LONG: Int = 3500
            @JvmField val EXTRA_LONG: Int = 4500
        }
    }

    /**
     * Text sizes for all types of SuperToasts.
     */
    class TextSize {
        companion object {
            @JvmField val EXTRA_SMALL: Int = 12
            @JvmField val SMALL: Int = 14
            @JvmField val MEDIUM: Int = 16
            @JvmField val LARGE: Int = 18
        }
    }
}
