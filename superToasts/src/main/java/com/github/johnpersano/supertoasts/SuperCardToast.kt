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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.github.johnpersano.R
import com.github.johnpersano.supertoasts.SuperToast.Animations
import com.github.johnpersano.supertoasts.SuperToast.IconPosition
import com.github.johnpersano.supertoasts.SuperToast.Type
import com.github.johnpersano.supertoasts.util.OnClickWrapper
import com.github.johnpersano.supertoasts.util.OnDismissWrapper
import com.github.johnpersano.supertoasts.util.Style
import com.github.johnpersano.supertoasts.util.SwipeDismissListener
import com.github.johnpersano.supertoasts.util.Wrappers

/**
 * SuperCardToasts are designed to be used inside of activities.
 * SuperCardToasts are designed to be displayed at the top of an activity to display messages.
 *
 * ## Important
 * Activity layout should contain a LinearLayout with the id `card_container`.
 *
 * ## Usage Example
 * ```kotlin
 * // In your layout XML, include:
 * // <LinearLayout
 * //     android:id="@+id/card_container"
 * //     android:layout_width="match_parent"
 * //     android:layout_height="wrap_content"
 * //     android:orientation="vertical" />
 *
 * // Simple toast
 * SuperCardToast.create(activity, "Hello World", SuperToast.Duration.SHORT).show()
 *
 * // With swipe to dismiss
 * val toast = SuperCardToast(activity)
 * toast.setText("Swipe me away")
 * toast.setSwipeToDismiss(true)
 * toast.show()
 *
 * // Handle orientation changes
 * override fun onSaveInstanceState(outState: Bundle) {
 *     SuperCardToast.onSaveState(outState)
 *     super.onSaveInstanceState(outState)
 * }
 * ```
 *
 * ## Differences from SuperActivityToast
 * - Displayed at the top of the activity (requires `card_container` in layout)
 * - Supports swipe-to-dismiss gesture
 * - Better suited for card-style notifications
 */
@Suppress("unused")
class SuperCardToast {

    companion object {
        private const val TAG = "SuperCardToast"
        private const val MANAGER_TAG = "SuperCardToast Manager"

        private const val ERROR_ACTIVITYNULL = " - You cannot pass a null Activity as a parameter."
        private const val ERROR_CONTAINERNULL = " - You must have a LinearLayout with the id of card_container in your layout!"
        private const val ERROR_VIEWCONTAINERNULL = " - Either the View or Container was null when trying to dismiss."
        private const val ERROR_NOTBUTTONTYPE = " is only compatible with BUTTON type SuperCardToasts."
        private const val ERROR_NOTPROGRESSHORIZONTALTYPE = " is only compatible with PROGRESS_HORIZONTAL type SuperCardToasts."

        private const val WARNING_PREHONEYCOMB = "Swipe to dismiss was enabled but the SDK version is pre-Honeycomb"

        // Bundle tag with a hex as a string so it can't interfere with other tags in the bundle
        private const val BUNDLE_TAG = "0x532e432e542e"

        /**
         * Returns a standard SuperCardToast.
         */
        @JvmStatic
        fun create(activity: Activity, textCharSequence: CharSequence, durationInteger: Int): SuperCardToast {
            val superCardToast = SuperCardToast(activity)
            superCardToast.setText(textCharSequence)
            superCardToast.setDuration(durationInteger)
            return superCardToast
        }

        /**
         * Returns a standard SuperCardToast with specified animations.
         */
        @JvmStatic
        fun create(
            activity: Activity,
            textCharSequence: CharSequence,
            durationInteger: Int,
            animations: Animations
        ): SuperCardToast {
            val superCardToast = SuperCardToast(activity)
            superCardToast.setText(textCharSequence)
            superCardToast.setDuration(durationInteger)
            superCardToast.setAnimations(animations)
            return superCardToast
        }

        /**
         * Returns a SuperCardToast with a specified style.
         */
        @JvmStatic
        fun create(
            activity: Activity,
            textCharSequence: CharSequence,
            durationInteger: Int,
            style: Style
        ): SuperCardToast {
            val superCardToast = SuperCardToast(activity)
            superCardToast.setText(textCharSequence)
            superCardToast.setDuration(durationInteger)
            superCardToast.setStyle(style)
            return superCardToast
        }

        /**
         * Dismisses and removes all showing/pending SuperCardToasts.
         */
        @JvmStatic
        fun cancelAllSuperCardToasts() {
            ManagerSuperCardToast.getInstance().cancelAllSuperActivityToasts()
        }

        /**
         * Saves pending/shown SuperCardToasts to a bundle.
         */
        @JvmStatic
        fun onSaveState(bundle: Bundle) {
            val list = Array(ManagerSuperCardToast.getInstance().getList().size) { i ->
                ReferenceHolder(ManagerSuperCardToast.getInstance().getList()[i])
            }
            bundle.putParcelableArray(BUNDLE_TAG, list)
            cancelAllSuperCardToasts()
        }

        /**
         * Returns and shows pending/shown SuperCardToasts from orientation change.
         */
        @JvmStatic
        fun onRestoreState(bundle: Bundle?, activity: Activity) {
            if (bundle == null) return

            val savedArray = bundle.getParcelableArray(BUNDLE_TAG) ?: return
            savedArray.forEachIndexed { index, parcelable ->
                SuperCardToast(activity, parcelable as ReferenceHolder, null, index + 1)
            }
        }

        /**
         * Returns and shows pending/shown SuperCardToasts from orientation change
         * and reattaches any OnClickWrappers/OnDismissWrappers.
         */
        @JvmStatic
        fun onRestoreState(bundle: Bundle?, activity: Activity, wrappers: Wrappers) {
            if (bundle == null) return

            val savedArray = bundle.getParcelableArray(BUNDLE_TAG) ?: return
            savedArray.forEachIndexed { index, parcelable ->
                SuperCardToast(activity, parcelable as ReferenceHolder, wrappers, index + 1)
            }
        }
    }

    private var activity: Activity? = null
    private var animations: Animations = Animations.FADE
    private var isIndeterminate: Boolean = false
    private var isTouchDismissible: Boolean = false
    private var isSwipeDismissible: Boolean = false
    private var isProgressIndeterminate: Boolean = false
    internal var showImmediate: Boolean = false
    private var button: Button? = null
    private var handler: Handler? = null
    private var iconPosition: IconPosition? = null
    private var duration: Int = SuperToast.Duration.SHORT
    private var icon: Int = 0
    private var background: Int = R.drawable.background_standard_gray
    private var typeface: Int = Typeface.NORMAL
    private var buttonTypefaceStyle: Int = Typeface.BOLD
    private var buttonIcon: Int = SuperToast.Icon.Dark.UNDO
    private var dividerColor: Int = Color.DKGRAY
    private lateinit var layoutInflater: LayoutInflater
    private lateinit var rootLayout: LinearLayout
    internal var onDismissWrapper: OnDismissWrapper? = null
    private var onClickWrapper: OnClickWrapper? = null
    private var token: Parcelable? = null
    private var progressBar: ProgressBar? = null
    private var onClickWrapperTag: String? = null
    private var onDismissWrapperTag: String? = null
    private lateinit var messageTextView: TextView
    private var type: Type = Type.STANDARD
    private var viewGroup: ViewGroup? = null
    private lateinit var toastView: View
    private var dividerView: View? = null

    // Runnable to invalidate the layout
    private val invalidateRunnable = Runnable {
        viewGroup?.postInvalidate()
    }

    // Runnable to dismiss
    private val hideRunnable = Runnable {
        dismiss()
    }

    // Runnable to dismiss immediately
    private val hideImmediateRunnable = Runnable {
        dismissImmediately()
    }

    // Runnable to dismiss with animation
    private val hideWithAnimationRunnable = Runnable {
        dismissWithLayoutAnimation()
    }

    // Touch dismiss listener
    private val touchDismissListener = View.OnTouchListener { _, motionEvent ->
        var timesTouched = 0
        if (timesTouched == 0) {
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                dismiss()
            }
        }
        timesTouched++
        false
    }

    // Button click listener
    private val buttonListener = View.OnClickListener {
        onClickWrapper?.onClick(it, token)
        dismiss()
        button?.isClickable = false
    }

    /**
     * Instantiates a new SuperCardToast.
     */
    @Suppress("ConstantConditions")
    constructor(activity: Activity) {
        if (activity == null) {
            throw IllegalArgumentException(TAG + ERROR_ACTIVITYNULL)
        }

        this.activity = activity
        this.type = Type.STANDARD

        layoutInflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        viewGroup = activity.findViewById(R.id.card_container)

        if (viewGroup == null) {
            throw IllegalArgumentException(TAG + ERROR_CONTAINERNULL)
        }

        toastView = layoutInflater.inflate(R.layout.supercardtoast, viewGroup, false)
        messageTextView = toastView.findViewById(R.id.message_textview)
        rootLayout = toastView.findViewById(R.id.root_layout)
    }

    /**
     * Instantiates a new SuperCardToast with a specified default style.
     */
    @Suppress("ConstantConditions")
    constructor(activity: Activity, style: Style) : this(activity) {
        setStyle(style)
    }

    /**
     * Instantiates a new SuperCardToast with a type.
     */
    @Suppress("ConstantConditions")
    constructor(activity: Activity, type: Type) {
        if (activity == null) {
            throw IllegalArgumentException(TAG + ERROR_ACTIVITYNULL)
        }

        this.activity = activity
        this.type = type

        layoutInflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        viewGroup = activity.findViewById(R.id.card_container)

        if (viewGroup == null) {
            throw IllegalArgumentException(TAG + ERROR_CONTAINERNULL)
        }

        when (type) {
            Type.BUTTON -> {
                toastView = layoutInflater.inflate(R.layout.supercardtoast_button, viewGroup, false)
                button = toastView.findViewById(R.id.button)
                dividerView = toastView.findViewById(R.id.divider)
                button?.setOnClickListener(buttonListener)
            }
            Type.PROGRESS -> {
                toastView = layoutInflater.inflate(R.layout.supercardtoast_progresscircle, viewGroup, false)
                progressBar = toastView.findViewById(R.id.progress_bar)
            }
            Type.PROGRESS_HORIZONTAL -> {
                toastView = layoutInflater.inflate(R.layout.supercardtoast_progresshorizontal, viewGroup, false)
                progressBar = toastView.findViewById(R.id.progress_bar)
            }
            else -> {
                toastView = layoutInflater.inflate(R.layout.supercardtoast, viewGroup, false)
            }
        }

        messageTextView = toastView.findViewById(R.id.message_textview)
        rootLayout = toastView.findViewById(R.id.root_layout)
    }

    /**
     * Instantiates a new SuperCardToast with a type and a specified style.
     */
    @Suppress("ConstantConditions")
    constructor(activity: Activity, type: Type, style: Style) : this(activity, type) {
        setStyle(style)
    }

    /**
     * Method used to recreate SuperCardToast after orientation change.
     */
    private constructor(
        activity: Activity,
        referenceHolder: ReferenceHolder,
        wrappers: Wrappers?,
        position: Int
    ) {
        this.activity = activity
        layoutInflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        viewGroup = activity.findViewById(R.id.card_container)

        val superCardToast: SuperCardToast? = when (referenceHolder.type) {
            Type.BUTTON -> {
                val toast = SuperCardToast(activity, Type.BUTTON)
                referenceHolder.buttonText?.let { toast.setButtonText(it) }
                toast.setButtonTextSizeFloat(referenceHolder.buttonTextSize)
                toast.setButtonTextColor(referenceHolder.buttonTextColor)
                toast.setButtonIcon(referenceHolder.buttonIcon)
                toast.setDividerColor(referenceHolder.buttonDivider)
                toast.setButtonTypefaceStyle(referenceHolder.buttonTypefaceStyle)

                wrappers?.getOnClickWrappers()?.forEach { onClickWrapper ->
                    if (onClickWrapper.getTag().equals(referenceHolder.clickListenerTag, ignoreCase = true)) {
                        toast.setOnClickWrapper(onClickWrapper, referenceHolder.token)
                    }
                }
                toast
            }
            Type.PROGRESS -> {
                // PROGRESS style SuperCardToasts should be managed by the developer
                this.type = Type.PROGRESS
                this.toastView = layoutInflater.inflate(R.layout.supercardtoast_progresscircle, viewGroup, false)
                this.messageTextView = this.toastView.findViewById(R.id.message_textview)
                this.rootLayout = this.toastView.findViewById(R.id.root_layout)
                return
            }
            Type.PROGRESS_HORIZONTAL -> {
                // PROGRESS_HORIZONTAL style SuperCardToasts should be managed by the developer
                this.type = Type.PROGRESS_HORIZONTAL
                this.toastView = layoutInflater.inflate(R.layout.supercardtoast_progresshorizontal, viewGroup, false)
                this.messageTextView = this.toastView.findViewById(R.id.message_textview)
                this.rootLayout = this.toastView.findViewById(R.id.root_layout)
                return
            }
            else -> SuperCardToast(activity)
        }

        superCardToast?.let { toast ->
            wrappers?.getOnDismissWrappers()?.forEach { onDismissWrapper ->
                if (onDismissWrapper.getTag().equals(referenceHolder.dismissListenerTag, ignoreCase = true)) {
                    toast.setOnDismissWrapper(onDismissWrapper)
                }
            }

            toast.setAnimations(referenceHolder.animations)
            referenceHolder.text?.let { toast.setText(it) }
            toast.setTypefaceStyle(referenceHolder.typefaceStyle)
            toast.setDuration(referenceHolder.duration)
            toast.setTextColor(referenceHolder.textColor)
            toast.setTextSizeFloat(referenceHolder.textSize)
            toast.setIndeterminate(referenceHolder.isIndeterminate)
            referenceHolder.iconPosition?.let { toast.setIcon(referenceHolder.icon, it) }
            toast.setBackground(referenceHolder.background)

            if (referenceHolder.isTouchDismissible) {
                toast.setTouchToDismiss(true)
            } else if (referenceHolder.isSwipeDismissible) {
                toast.setSwipeToDismiss(true)
            }

            toast.setShowImmediate(true)
            toast.show()
        }
    }

    /**
     * Shows the SuperCardToast. If another SuperCardToast is showing than
     * this one will be added underneath.
     */
    fun show() {
        ManagerSuperCardToast.getInstance().add(this)

        if (!isIndeterminate) {
            handler = Handler()
            handler?.postDelayed(hideRunnable, duration.toLong())
        }

        viewGroup?.addView(toastView)

        if (!showImmediate) {
            val animation = getShowAnimation()

            animation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationEnd(arg0: Animation?) {
                    Handler().post(invalidateRunnable)
                }

                override fun onAnimationRepeat(arg0: Animation?) {}

                override fun onAnimationStart(arg0: Animation?) {}
            })

            toastView.startAnimation(animation)
        }
    }

    /**
     * Returns the Type of SuperCardToast.
     */
    fun getType(): Type = type

    /**
     * Returns the message text of the SuperCardToast.
     */
    fun getText(): CharSequence = messageTextView.text

    /**
     * Sets the message text of the SuperCardToast.
     */
    fun setText(text: CharSequence) {
        messageTextView.text = text
    }

    /**
     * Returns the message Typeface style of the SuperCardToast.
     */
    fun getTypefaceStyle(): Int = typeface

    /**
     * Sets the message Typeface style of the SuperCardToast.
     */
    fun setTypefaceStyle(typeface: Int) {
        this.typeface = typeface
        messageTextView.setTypeface(messageTextView.typeface, typeface)
    }

    /**
     * Returns the message text color of the SuperCardToast.
     */
    fun getTextColor(): Int = messageTextView.currentTextColor

    /**
     * Sets the message text color of the SuperCardToast.
     */
    fun setTextColor(textColor: Int) {
        messageTextView.setTextColor(textColor)
    }

    /**
     * Used by orientation change recreation.
     */
    private fun setTextSizeFloat(textSize: Float) {
        messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
    }

    /**
     * Returns the text size of the SuperCardToast message in pixels.
     */
    fun getTextSize(): Float = messageTextView.textSize

    /**
     * Sets the text size of the SuperCardToast message.
     */
    fun setTextSize(textSize: Int) {
        messageTextView.textSize = textSize.toFloat()
    }

    /**
     * Returns the duration of the SuperCardToast.
     */
    fun getDuration(): Int = duration

    /**
     * Sets the duration that the SuperCardToast will show.
     */
    fun setDuration(duration: Int) {
        this.duration = duration
    }

    /**
     * Returns true if the SuperCardToast is indeterminate.
     */
    fun isIndeterminate(): Boolean = isIndeterminate

    /**
     * If true will show the SuperCardToast for an indeterminate time period and ignore any set duration.
     */
    fun setIndeterminate(isIndeterminate: Boolean) {
        this.isIndeterminate = isIndeterminate
    }

    /**
     * Sets an icon resource to the SuperCardToast with a specified position.
     */
    fun setIcon(icon: Int, iconPosition: IconPosition) {
        this.icon = icon
        this.iconPosition = iconPosition

        val iconDrawable = activity?.resources?.getDrawable(icon)
        when (iconPosition) {
            IconPosition.BOTTOM -> messageTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, iconDrawable)
            IconPosition.LEFT -> messageTextView.setCompoundDrawablesWithIntrinsicBounds(iconDrawable, null, null, null)
            IconPosition.RIGHT -> messageTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, iconDrawable, null)
            IconPosition.TOP -> messageTextView.setCompoundDrawablesWithIntrinsicBounds(null, iconDrawable, null, null)
        }
    }

    /**
     * Returns the icon position of the SuperCardToast.
     */
    fun getIconPosition(): IconPosition? = iconPosition

    /**
     * Returns the icon resource of the SuperCardToast.
     */
    fun getIconResource(): Int = icon

    /**
     * Sets the background resource of the SuperCardToast.
     */
    fun setBackground(background: Int) {
        this.background = checkForKitKatBackgrounds(background)
        rootLayout.setBackgroundResource(this.background)
    }

    /**
     * Make sure KitKat style backgrounds are not used with SuperCardToast.
     */
    private fun checkForKitKatBackgrounds(background: Int): Int {
        return when (background) {
            R.drawable.background_kitkat_black -> R.drawable.background_standard_black
            R.drawable.background_kitkat_blue -> R.drawable.background_standard_blue
            R.drawable.background_kitkat_gray -> R.drawable.background_standard_gray
            R.drawable.background_kitkat_green -> R.drawable.background_standard_green
            R.drawable.background_kitkat_orange -> R.drawable.background_standard_orange
            R.drawable.background_kitkat_purple -> R.drawable.background_standard_purple
            R.drawable.background_kitkat_red -> R.drawable.background_standard_red
            R.drawable.background_kitkat_white -> R.drawable.background_standard_white
            else -> background
        }
    }

    /**
     * Returns the background resource of the SuperCardToast.
     */
    fun getBackgroundResource(): Int = background

    /**
     * Returns the show/hide animations of the SuperCardToast.
     */
    fun getAnimations(): Animations = animations

    /**
     * Sets the show/hide animations of the SuperCardToast.
     */
    fun setAnimations(animations: Animations) {
        this.animations = animations
    }

    /**
     * Returns true if the SuperCardToast is set to show without animation.
     */
    fun getShowImmediate(): Boolean = showImmediate

    /**
     * If true will show the SuperCardToast without animation.
     */
    fun setShowImmediate(showImmediate: Boolean) {
        this.showImmediate = showImmediate
    }

    /**
     * If true will dismiss the SuperCardToast if the user touches it.
     */
    fun setTouchToDismiss(touchDismiss: Boolean) {
        this.isTouchDismissible = touchDismiss

        if (touchDismiss) {
            toastView.setOnTouchListener(touchDismissListener)
        } else {
            toastView.setOnTouchListener(null)
        }
    }

    /**
     * Returns true if the SuperCardToast is touch dismissible.
     */
    fun isTouchDismissible(): Boolean = isTouchDismissible

    /**
     * If true will dismiss the SuperCardToast if the user swipes it.
     */
    fun setSwipeToDismiss(swipeDismiss: Boolean) {
        this.isSwipeDismissible = swipeDismiss

        if (swipeDismiss) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB_MR1) {
                val swipeDismissListener = SwipeDismissListener(toastView, object : SwipeDismissListener.OnDismissCallback {
                    override fun onDismiss(view: View) {
                        dismissImmediately()
                    }
                })
                toastView.setOnTouchListener(swipeDismissListener)
            } else {
                Log.w(TAG, WARNING_PREHONEYCOMB)
            }
        } else {
            toastView.setOnTouchListener(null)
        }
    }

    /**
     * Returns true if the SuperCardToast is swipe dismissible.
     */
    fun isSwipeDismissible(): Boolean = isSwipeDismissible

    /**
     * Used in ManagerSuperCardToast.
     */
    protected fun getOnDismissWrapper(): OnDismissWrapper? = onDismissWrapper

    /**
     * Sets an OnDismissWrapper defined in this library to the SuperCardToast.
     */
    fun setOnDismissWrapper(onDismissWrapper: OnDismissWrapper) {
        this.onDismissWrapper = onDismissWrapper
        this.onDismissWrapperTag = onDismissWrapper.getTag()
    }

    /**
     * Used in orientation change recreation.
     */
    private fun getDismissListenerTag(): String? = onDismissWrapperTag

    /**
     * Dismisses the SuperCardToast.
     */
    fun dismiss() {
        ManagerSuperCardToast.getInstance().remove(this)
        dismissWithAnimation()
    }

    /**
     * Dismisses the SuperCardToast without an animation.
     */
    fun dismissImmediately() {
        ManagerSuperCardToast.getInstance().remove(this)

        handler?.let {
            it.removeCallbacks(hideRunnable)
            it.removeCallbacks(hideWithAnimationRunnable)
            handler = null
        }

        if (toastView != null && viewGroup != null) {
            viewGroup?.removeView(toastView)
            onDismissWrapper?.onDismiss(getView())
        } else {
            Log.e(TAG, ERROR_VIEWCONTAINERNULL)
        }
    }

    /**
     * Hide the SuperCardToast and animate the Layout. Post Honeycomb only.
     */
    @SuppressLint("NewApi")
    private fun dismissWithLayoutAnimation() {
        if (toastView != null) {
            toastView.visibility = View.INVISIBLE

            val layoutParams = toastView.layoutParams
            val originalHeight = toastView.height

            val animator = ValueAnimator.ofInt(originalHeight, 1)
                .setDuration(activity?.resources?.getInteger(android.R.integer.config_shortAnimTime)?.toLong() ?: 200)

            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    Handler().post(hideImmediateRunnable)
                }
            })

            animator.addUpdateListener { valueAnimator ->
                if (toastView != null) {
                    try {
                        layoutParams.height = valueAnimator.animatedValue as Int
                        toastView.layoutParams = layoutParams
                    } catch (e: NullPointerException) {
                        // Do nothing
                    }
                }
            }

            animator.start()
        } else {
            dismissImmediately()
        }
    }

    @SuppressLint("NewApi")
    @Suppress("deprecation")
    private fun dismissWithAnimation() {
        val animation = getDismissAnimation()

        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationEnd(animation: Animation?) {
                Handler().post(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    hideWithAnimationRunnable
                } else {
                    hideImmediateRunnable
                })
            }

            override fun onAnimationRepeat(animation: Animation?) {}

            override fun onAnimationStart(animation: Animation?) {}
        })

        toastView?.startAnimation(animation)
    }

    /**
     * Sets an OnClickWrapper to the button in a BUTTON type SuperCardToast.
     */
    fun setOnClickWrapper(onClickWrapper: OnClickWrapper) {
        if (type != Type.BUTTON) {
            Log.w(TAG, "setOnClickListenerWrapper()$ERROR_NOTBUTTONTYPE")
        }

        this.onClickWrapper = onClickWrapper
        this.onClickWrapperTag = onClickWrapper.getTag()
    }

    /**
     * Sets an OnClickWrapper with a parcelable object to the button in a BUTTON type SuperCardToast.
     */
    fun setOnClickWrapper(onClickWrapper: OnClickWrapper, token: Parcelable?) {
        if (type != Type.BUTTON) {
            Log.e(TAG, "setOnClickListenerWrapper()$ERROR_NOTBUTTONTYPE")
        }

        onClickWrapper.setToken(token)

        this.token = token
        this.onClickWrapper = onClickWrapper
        this.onClickWrapperTag = onClickWrapper.getTag()
    }

    /**
     * Used in orientation change recreation.
     */
    private fun getToken(): Parcelable? = token

    /**
     * Used in orientation change recreation.
     */
    private fun getOnClickWrapperTag(): String? = onClickWrapperTag

    /**
     * Sets the icon resource and text of the button in a BUTTON type SuperCardToast.
     */
    fun setButtonIcon(buttonIcon: Int, buttonText: CharSequence) {
        if (type != Type.BUTTON) {
            Log.w(TAG, "setButtonIcon()$ERROR_NOTBUTTONTYPE")
        }

        this.buttonIcon = buttonIcon

        button?.let {
            it.setCompoundDrawablesWithIntrinsicBounds(activity?.resources?.getDrawable(buttonIcon), null, null, null)
            it.text = buttonText
        }
    }

    /**
     * Returns the icon resource of the button.
     */
    fun getButtonIcon(): Int = buttonIcon

    /**
     * Sets the icon resource of the button in a BUTTON type SuperCardToast.
     */
    fun setButtonIcon(buttonIcon: Int) {
        if (type != Type.BUTTON) {
            Log.w(TAG, "setButtonIcon()$ERROR_NOTBUTTONTYPE")
        }

        this.buttonIcon = buttonIcon

        button?.setCompoundDrawablesWithIntrinsicBounds(
            activity?.resources?.getDrawable(buttonIcon),
            null,
            null,
            null
        )
    }

    /**
     * Returns the divider color of a BUTTON type SuperCardToast.
     */
    fun getDividerColor(): Int = dividerColor

    /**
     * Sets the divider color of a BUTTON type SuperCardToast.
     */
    fun setDividerColor(dividerColor: Int) {
        if (type != Type.BUTTON) {
            Log.w(TAG, "setDivider()$ERROR_NOTBUTTONTYPE")
        }

        this.dividerColor = dividerColor
        dividerView?.setBackgroundColor(dividerColor)
    }

    /**
     * Returns the button text of a BUTTON type SuperCardToast.
     */
    fun getButtonText(): CharSequence {
        return button?.text ?: run {
            Log.e(TAG, "getButtonText()$ERROR_NOTBUTTONTYPE")
            ""
        }
    }

    /**
     * Sets the button text of a BUTTON type SuperCardToast.
     */
    fun setButtonText(buttonText: CharSequence) {
        if (type != Type.BUTTON) {
            Log.w(TAG, "setButtonText()$ERROR_NOTBUTTONTYPE")
        }

        button?.text = buttonText
    }

    /**
     * Returns the typeface style of the button in a BUTTON type SuperCardToast.
     */
    fun getButtonTypefaceStyle(): Int = buttonTypefaceStyle

    /**
     * Sets the typeface style of the button in a BUTTON type SuperCardToast.
     */
    fun setButtonTypefaceStyle(typefaceStyle: Int) {
        if (type != Type.BUTTON) {
            Log.w(TAG, "setButtonTypefaceStyle()$ERROR_NOTBUTTONTYPE")
        }

        button?.let {
            this.buttonTypefaceStyle = typefaceStyle
            it.setTypeface(it.typeface, typefaceStyle)
        }
    }

    /**
     * Returns the button text color of a BUTTON type SuperCardToast.
     */
    fun getButtonTextColor(): Int {
        return button?.currentTextColor ?: run {
            Log.e(TAG, "getButtonTextColor()$ERROR_NOTBUTTONTYPE")
            0
        }
    }

    /**
     * Sets the button text color of a BUTTON type SuperCardToast.
     */
    fun setButtonTextColor(buttonTextColor: Int) {
        if (type != Type.BUTTON) {
            Log.w(TAG, "setButtonTextColor()$ERROR_NOTBUTTONTYPE")
        }

        button?.setTextColor(buttonTextColor)
    }

    /**
     * Used by orientation change recreation.
     */
    private fun setButtonTextSizeFloat(buttonTextSize: Float) {
        button?.setTextSize(TypedValue.COMPLEX_UNIT_PX, buttonTextSize)
    }

    /**
     * Returns the button text size of a BUTTON type SuperCardToast.
     */
    fun getButtonTextSize(): Float {
        return button?.textSize ?: run {
            Log.e(TAG, "getButtonTextSize()$ERROR_NOTBUTTONTYPE")
            0.0f
        }
    }

    /**
     * Sets the button text size of a BUTTON type SuperCardToast.
     */
    fun setButtonTextSize(buttonTextSize: Int) {
        if (type != Type.BUTTON) {
            Log.w(TAG, "setButtonTextSize()$ERROR_NOTBUTTONTYPE")
        }

        button?.textSize = buttonTextSize.toFloat()
    }

    /**
     * Returns the progress of the progressbar in a PROGRESS_HORIZONTAL type SuperCardToast.
     */
    fun getProgress(): Int {
        return progressBar?.progress ?: run {
            Log.e(TAG, "getProgress()$ERROR_NOTPROGRESSHORIZONTALTYPE")
            0
        }
    }

    /**
     * Sets the progress of the progressbar in a PROGRESS_HORIZONTAL type SuperCardToast.
     */
    fun setProgress(progress: Int) {
        if (type != Type.PROGRESS_HORIZONTAL) {
            Log.w(TAG, "setProgress()$ERROR_NOTPROGRESSHORIZONTALTYPE")
        }

        progressBar?.progress = progress
    }

    /**
     * Returns the maximum value of the progressbar in a PROGRESS_HORIZONTAL type SuperCardToast.
     */
    fun getMaxProgress(): Int {
        return progressBar?.max ?: run {
            Log.e(TAG, "getMaxProgress()$ERROR_NOTPROGRESSHORIZONTALTYPE")
            progressBar?.max ?: 0
        }
    }

    /**
     * Sets the maximum value of the progressbar in a PROGRESS_HORIZONTAL type SuperCardToast.
     */
    fun setMaxProgress(maxProgress: Int) {
        if (type != Type.PROGRESS_HORIZONTAL) {
            Log.w(TAG, "setMaxProgress()$ERROR_NOTPROGRESSHORIZONTALTYPE")
        }

        progressBar?.max = maxProgress
    }

    /**
     * Returns an indeterminate value to the progressbar of a PROGRESS type SuperCardToast.
     */
    fun getProgressIndeterminate(): Boolean = isProgressIndeterminate

    /**
     * Sets an indeterminate value to the progressbar of a PROGRESS type SuperCardToast.
     */
    fun setProgressIndeterminate(isIndeterminate: Boolean) {
        if (type != Type.PROGRESS_HORIZONTAL) {
            Log.w(TAG, "setProgressIndeterminate()$ERROR_NOTPROGRESSHORIZONTALTYPE")
        }

        this.isProgressIndeterminate = isIndeterminate
        progressBar?.isIndeterminate = isIndeterminate
    }

    /**
     * Returns the SuperCardToast message textview.
     */
    fun getTextView(): TextView = messageTextView

    /**
     * Returns the SuperCardToast view.
     */
    fun getView(): View = toastView

    /**
     * Runnable to dismiss the SuperCardToast with animation.
     */

    /**
     * Returns true if the SuperCardToast is showing.
     */
    fun isShowing(): Boolean = toastView != null && toastView.isShown

    /**
     * Returns the viewgroup that the SuperCardToast is attached to.
     */
    fun getViewGroup(): ViewGroup? = viewGroup

    /**
     * Returns the calling activity of the SuperCardToast.
     */
    fun getActivity(): Activity? = activity

    /**
     * Private method used to set a default style to the SuperCardToast.
     */
    private fun setStyle(style: Style) {
        this.setAnimations(style.animations)
        this.setTypefaceStyle(style.typefaceStyle)
        this.setTextColor(style.textColor)
        this.setBackground(style.background)

        if (this.type == Type.BUTTON) {
            this.setDividerColor(style.dividerColor)
            this.setButtonTextColor(style.buttonTextColor)
        }
    }

    private fun getShowAnimation(): Animation {
        return when (animations) {
            Animations.FLYIN -> {
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
            Animations.SCALE -> {
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
            Animations.POPUP -> {
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

    private fun getDismissAnimation(): Animation {
        return when (animations) {
            Animations.FLYIN -> {
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
            Animations.SCALE -> {
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
            Animations.POPUP -> {
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
     * Parcelable class that saves all data on orientation change.
     */
    private class ReferenceHolder : Parcelable {
        var type: Type
        var animations: Animations
        var isIndeterminate: Boolean = false
        var isTouchDismissible: Boolean = false
        var isSwipeDismissible: Boolean = false
        var textSize: Float = 0f
        var buttonTextSize: Float = 0f
        var iconPosition: IconPosition? = null
        var duration: Int = 0
        var textColor: Int = 0
        var icon: Int = 0
        var background: Int = 0
        var typefaceStyle: Int = 0
        var buttonTextColor: Int = 0
        var buttonIcon: Int = 0
        var buttonDivider: Int = 0
        var buttonTypefaceStyle: Int = 0
        var token: Parcelable? = null
        var text: String? = null
        var buttonText: String? = null
        var clickListenerTag: String? = null
        var dismissListenerTag: String? = null

        constructor(superCardToast: SuperCardToast) {
            type = superCardToast.getType()

            if (type == Type.BUTTON) {
                buttonText = superCardToast.getButtonText().toString()
                buttonTextSize = superCardToast.getButtonTextSize()
                buttonTextColor = superCardToast.getButtonTextColor()
                buttonIcon = superCardToast.getButtonIcon()
                buttonDivider = superCardToast.getDividerColor()
                clickListenerTag = superCardToast.getOnClickWrapperTag()
                buttonTypefaceStyle = superCardToast.getButtonTypefaceStyle()
                token = superCardToast.getToken()
            }

            if (superCardToast.getIconResource() != 0 && superCardToast.getIconPosition() != null) {
                icon = superCardToast.getIconResource()
                iconPosition = superCardToast.getIconPosition()
            }

            dismissListenerTag = superCardToast.getDismissListenerTag()
            animations = superCardToast.getAnimations()
            text = superCardToast.getText().toString()
            typefaceStyle = superCardToast.getTypefaceStyle()
            duration = superCardToast.getDuration()
            textColor = superCardToast.getTextColor()
            textSize = superCardToast.getTextSize()
            isIndeterminate = superCardToast.isIndeterminate()
            background = superCardToast.getBackgroundResource()
            isTouchDismissible = superCardToast.isTouchDismissible()
            isSwipeDismissible = superCardToast.isSwipeDismissible()
        }

        constructor(parcel: Parcel) {
            type = Type.values()[parcel.readInt()]

            if (type == Type.BUTTON) {
                buttonText = parcel.readString()
                buttonTextSize = parcel.readFloat()
                buttonTextColor = parcel.readInt()
                buttonIcon = parcel.readInt()
                buttonDivider = parcel.readInt()
                buttonTypefaceStyle = parcel.readInt()
                clickListenerTag = parcel.readString()
                token = parcel.readParcelable(this::class.java.classLoader)
            }

            val hasIcon = parcel.readByte().toInt() != 0

            if (hasIcon) {
                icon = parcel.readInt()
                iconPosition = IconPosition.values()[parcel.readInt()]
            }

            dismissListenerTag = parcel.readString()
            animations = Animations.values()[parcel.readInt()]
            text = parcel.readString()
            typefaceStyle = parcel.readInt()
            duration = parcel.readInt()
            textColor = parcel.readInt()
            textSize = parcel.readFloat()
            isIndeterminate = parcel.readByte().toInt() != 0
            background = parcel.readInt()
            isTouchDismissible = parcel.readByte().toInt() != 0
            isSwipeDismissible = parcel.readByte().toInt() != 0
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(type.ordinal)

            if (type == Type.BUTTON) {
                parcel.writeString(buttonText)
                parcel.writeFloat(buttonTextSize)
                parcel.writeInt(buttonTextColor)
                parcel.writeInt(buttonIcon)
                parcel.writeInt(buttonDivider)
                parcel.writeInt(buttonTypefaceStyle)
                parcel.writeString(clickListenerTag)
                parcel.writeParcelable(token, 0)
            }

            if (icon != 0 && iconPosition != null) {
                parcel.writeByte(1.toByte())
                parcel.writeInt(icon)
                parcel.writeInt(iconPosition!!.ordinal)
            } else {
                parcel.writeByte(0.toByte())
            }

            parcel.writeString(dismissListenerTag)
            parcel.writeInt(animations.ordinal)
            parcel.writeString(text)
            parcel.writeInt(typefaceStyle)
            parcel.writeInt(duration)
            parcel.writeInt(textColor)
            parcel.writeFloat(textSize)
            parcel.writeByte((if (isIndeterminate) 1 else 0).toByte())
            parcel.writeInt(background)
            parcel.writeByte((if (isTouchDismissible) 1 else 0).toByte())
            parcel.writeByte((if (isSwipeDismissible) 1 else 0).toByte())
        }

        override fun describeContents(): Int = 0

        companion object CREATOR : Parcelable.Creator<ReferenceHolder> {
            override fun createFromParcel(parcel: Parcel): ReferenceHolder {
                return ReferenceHolder(parcel)
            }

            override fun newArray(size: Int): Array<ReferenceHolder?> {
                return arrayOfNulls(size)
            }
        }
    }
}
