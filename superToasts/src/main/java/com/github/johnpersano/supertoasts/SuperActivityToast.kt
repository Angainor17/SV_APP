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
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
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
import com.github.johnpersano.supertoasts.util.Wrappers

/**
 * SuperActivityToasts are designed to be used inside of Activities.
 * When the Activity is destroyed the SuperActivityToast is destroyed along with it.
 *
 * ## Usage Example
 * ```kotlin
 * // Simple toast
 * SuperActivityToast.create(activity, "Hello World", SuperToast.Duration.SHORT).show()
 *
 * // With button
 * val toast = SuperActivityToast(activity, Type.BUTTON)
 * toast.setText("Item deleted")
 * toast.setButtonText("UNDO")
 * toast.setOnClickWrapper(OnClickWrapper("undo") { view, token ->
 *     // Handle undo
 * })
 * toast.show()
 *
 * // With progress
 * val progressToast = SuperActivityToast(activity, Type.PROGRESS_HORIZONTAL)
 * progressToast.setProgress(50)
 * progressToast.show()
 *
 * // Handle orientation changes
 * override fun onSaveInstanceState(outState: Bundle) {
 *     SuperActivityToast.onSaveState(outState)
 *     super.onSaveInstanceState(outState)
 * }
 *
 * override fun onCreate(savedInstanceState: Bundle?) {
 *     super.onCreate(savedInstanceState)
 *     SuperActivityToast.onRestoreState(savedInstanceState, this)
 * }
 * ```
 *
 * ## Types
 * - [Type.STANDARD] - Simple message toast
 * - [Type.BUTTON] - Toast with action button
 * - [Type.PROGRESS] - Toast with indeterminate progress indicator
 * - [Type.PROGRESS_HORIZONTAL] - Toast with horizontal progress bar
 */
@Suppress("unused", "BooleanMethodIsAlwaysInverted", "ConstantConditions")
class SuperActivityToast {

    companion object {
        private const val TAG = "SuperActivityToast"
        private const val MANAGER_TAG = "SuperActivityToast Manager"

        private const val ERROR_ACTIVITYNULL = " - You cannot pass a null Activity as a parameter."
        private const val ERROR_NOTBUTTONTYPE = " - is only compatible with BUTTON type SuperActivityToasts."
        private const val ERROR_NOTPROGRESSHORIZONTALTYPE = " - is only compatible with PROGRESS_HORIZONTAL type SuperActivityToasts."
        private const val ERROR_NOTEITHERPROGRESSTYPE = " - is only compatible with PROGRESS_HORIZONTAL or PROGRESS type SuperActivityToasts."

        // Bundle tag with a hex as a string so it can't interfere with other tags in the bundle
        private const val BUNDLE_TAG = "0x532e412e542e"

        /**
         * Returns a standard SuperActivityToast.
         */
        @JvmStatic
        fun create(activity: Activity, textCharSequence: CharSequence, durationInteger: Int): SuperActivityToast {
            val superActivityToast = SuperActivityToast(activity)
            superActivityToast.setText(textCharSequence)
            superActivityToast.setDuration(durationInteger)
            return superActivityToast
        }

        /**
         * Returns a standard SuperActivityToast with specified animations.
         */
        @JvmStatic
        fun create(
            activity: Activity,
            textCharSequence: CharSequence,
            durationInteger: Int,
            animations: Animations
        ): SuperActivityToast {
            val superActivityToast = SuperActivityToast(activity)
            superActivityToast.setText(textCharSequence)
            superActivityToast.setDuration(durationInteger)
            superActivityToast.setAnimations(animations)
            return superActivityToast
        }

        /**
         * Returns a SuperActivityToast with a specified style.
         */
        @JvmStatic
        fun create(
            activity: Activity,
            textCharSequence: CharSequence,
            durationInteger: Int,
            style: Style
        ): SuperActivityToast {
            val superActivityToast = SuperActivityToast(activity)
            superActivityToast.setText(textCharSequence)
            superActivityToast.setDuration(durationInteger)
            superActivityToast.setStyle(style)
            return superActivityToast
        }

        /**
         * Dismisses and removes all pending/showing SuperActivityToasts.
         */
        @JvmStatic
        fun cancelAllSuperActivityToasts() {
            ManagerSuperActivityToast.getInstance().cancelAllSuperActivityToasts()
        }

        /**
         * Dismisses and removes all pending/showing SuperActivityToasts for a specific activity.
         */
        @JvmStatic
        fun clearSuperActivityToastsForActivity(activity: Activity) {
            ManagerSuperActivityToast.getInstance().cancelAllSuperActivityToastsForActivity(activity)
        }

        /**
         * Saves pending/showing SuperActivityToasts to a bundle.
         */
        @JvmStatic
        fun onSaveState(bundle: Bundle) {
            val list = Array(ManagerSuperActivityToast.getInstance().getList().size) { i ->
                ReferenceHolder(ManagerSuperActivityToast.getInstance().getList()[i])
            }
            bundle.putParcelableArray(BUNDLE_TAG, list)
            cancelAllSuperActivityToasts()
        }

        /**
         * Recreates pending/showing SuperActivityToasts from orientation change.
         */
        @JvmStatic
        fun onRestoreState(bundle: Bundle?, activity: Activity) {
            if (bundle == null) return

            val savedArray = bundle.getParcelableArray(BUNDLE_TAG) ?: return
            savedArray.forEachIndexed { index, parcelable ->
                SuperActivityToast(activity, parcelable as ReferenceHolder, null, index + 1)
            }
        }

        /**
         * Recreates pending/showing SuperActivityToasts from orientation change
         * and reattaches any OnClickWrappers/OnDismissWrappers.
         */
        @JvmStatic
        fun onRestoreState(bundle: Bundle?, activity: Activity, wrappers: Wrappers) {
            if (bundle == null) return

            val savedArray = bundle.getParcelableArray(BUNDLE_TAG) ?: return
            savedArray.forEachIndexed { index, parcelable ->
                SuperActivityToast(activity, parcelable as ReferenceHolder, wrappers, index + 1)
            }
        }
    }

    private var activity: Activity? = null
    internal var animations: Animations = Animations.FADE
    private var isIndeterminate: Boolean = false
    private var isTouchDismissible: Boolean = false
    private var isProgressIndeterminate: Boolean = false
    internal var showImmediate: Boolean = false
    private var button: Button? = null
    private var iconPosition: IconPosition? = null
    private var duration: Int = SuperToast.Duration.SHORT
    private var icon: Int = 0
    private var background: Int = Style.getBackground(Style.GRAY)
    private var typefaceStyle: Int = Typeface.NORMAL
    private var buttonTypefaceStyle: Int = Typeface.BOLD
    private var buttonIcon: Int = SuperToast.Icon.Dark.UNDO
    private var dividerColor: Int = Color.LTGRAY
    private val layoutInflater: LayoutInflater
    private val rootLayout: LinearLayout
    private var onDismissWrapper: OnDismissWrapper? = null
    private var onClickWrapper: OnClickWrapper? = null
    private var token: Parcelable? = null
    private var progressBar: ProgressBar? = null
    private var onClickWrapperTag: String? = null
    private var onDismissWrapperTag: String? = null
    private val messageTextView: TextView
    private var type: Type = Type.STANDARD
    private var dividerView: View? = null
    private var viewGroup: ViewGroup? = null
    private val toastView: View

    // Touch dismiss listener
    private val touchDismissListener = View.OnTouchListener { view, motionEvent ->
        var timesTouched = 0
        // Hack to prevent repeat touch events causing erratic behavior
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
        // Make sure the button cannot be clicked multiple times
        button?.isClickable = false
    }

    /**
     * Instantiates a new SuperActivityToast.
     */
    constructor(activity: Activity) {
        if (activity == null) {
            throw IllegalArgumentException(TAG + ERROR_ACTIVITYNULL)
        }

        this.activity = activity
        layoutInflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        viewGroup = activity.findViewById(android.R.id.content)

        toastView = layoutInflater.inflate(R.layout.supertoast, viewGroup, false)
        messageTextView = toastView.findViewById(R.id.message_textview)
        rootLayout = toastView.findViewById(R.id.root_layout)
    }

    /**
     * Instantiates a new SuperActivityToast with a specified style.
     */
    constructor(activity: Activity, style: Style) : this(activity) {
        setStyle(style)
    }

    /**
     * Instantiates a new SuperActivityToast with a type.
     */
    constructor(activity: Activity, type: Type) {
        if (activity == null) {
            throw IllegalArgumentException(TAG + ERROR_ACTIVITYNULL)
        }

        this.activity = activity
        this.type = type
        layoutInflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        viewGroup = activity.findViewById(android.R.id.content)

        when (type) {
            Type.STANDARD -> {
                toastView = layoutInflater.inflate(R.layout.supertoast, viewGroup, false)
            }
            Type.BUTTON -> {
                toastView = layoutInflater.inflate(R.layout.superactivitytoast_button, viewGroup, false)
                button = toastView.findViewById(R.id.button)
                dividerView = toastView.findViewById(R.id.divider)
                button?.setOnClickListener(buttonListener)
            }
            Type.PROGRESS -> {
                toastView = layoutInflater.inflate(R.layout.superactivitytoast_progresscircle, viewGroup, false)
                progressBar = toastView.findViewById(R.id.progress_bar)
            }
            Type.PROGRESS_HORIZONTAL -> {
                toastView = layoutInflater.inflate(R.layout.superactivitytoast_progresshorizontal, viewGroup, false)
                progressBar = toastView.findViewById(R.id.progress_bar)
            }
        }

        messageTextView = toastView.findViewById(R.id.message_textview)
        rootLayout = toastView.findViewById(R.id.root_layout)
    }

    /**
     * Instantiates a new SuperActivityToast with a type and a specified style.
     */
    constructor(activity: Activity, type: Type, style: Style) : this(activity, type) {
        setStyle(style)
    }

    /**
     * Method used to recreate SuperActivityToast after orientation change.
     */
    private constructor(
        activity: Activity,
        referenceHolder: ReferenceHolder,
        wrappers: Wrappers?,
        position: Int
    ) {
        this.activity = activity
        layoutInflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        viewGroup = activity.findViewById(android.R.id.content)

        val superActivityToast: SuperActivityToast = when (referenceHolder.type) {
            Type.BUTTON -> {
                val toast = SuperActivityToast(activity, Type.BUTTON)
                referenceHolder.buttonText?.let { toast.setButtonText(it) }
                toast.setButtonTextSizeFloat(referenceHolder.buttonTextSize)
                toast.setButtonTextColor(referenceHolder.buttonTextColor)
                toast.setButtonIcon(referenceHolder.buttonIcon)
                toast.setDividerColor(referenceHolder.divider)
                toast.setButtonTypefaceStyle(referenceHolder.buttonTypefaceStyle)

                val screenSize = activity.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK

                // Changes the size of the BUTTON type SuperActivityToast to mirror Gmail app
                if (screenSize >= Configuration.SCREENLAYOUT_SIZE_LARGE) {
                    val layoutParams = FrameLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.BOTTOM or Gravity.RIGHT
                        bottomMargin = activity.resources.getDimension(R.dimen.buttontoast_hover).toInt()
                        rightMargin = activity.resources.getDimension(R.dimen.buttontoast_x_padding).toInt()
                        leftMargin = activity.resources.getDimension(R.dimen.buttontoast_x_padding).toInt()
                    }
                    toast.rootLayout.layoutParams = layoutParams
                }

                // Reattach any OnClickWrappers by matching tags sent through parcel
                wrappers?.getOnClickWrappers()?.forEach { onClickWrapper ->
                    if (onClickWrapper.getTag().equals(referenceHolder.clickListenerTag, ignoreCase = true)) {
                        toast.setOnClickWrapper(onClickWrapper, referenceHolder.token)
                    }
                }
                toast
            }
            Type.PROGRESS -> {
                // PROGRESS SuperActivityToasts should be managed by the developer
                this.type = Type.PROGRESS
                toastView = layoutInflater.inflate(R.layout.superactivitytoast_progresscircle, viewGroup, false)
                messageTextView = toastView.findViewById(R.id.message_textview)
                rootLayout = toastView.findViewById(R.id.root_layout)
                return
            }
            Type.PROGRESS_HORIZONTAL -> {
                // PROGRESS_HORIZONTAL SuperActivityToasts should be managed by the developer
                this.type = Type.PROGRESS_HORIZONTAL
                toastView = layoutInflater.inflate(R.layout.superactivitytoast_progresshorizontal, viewGroup, false)
                messageTextView = toastView.findViewById(R.id.message_textview)
                rootLayout = toastView.findViewById(R.id.root_layout)
                return
            }
            else -> SuperActivityToast(activity)
        }

        // Reattach any OnDismissWrappers by matching tags sent through parcel
        wrappers?.getOnDismissWrappers()?.forEach { onDismissWrapper ->
            if (onDismissWrapper.getTag().equals(referenceHolder.dismissListenerTag, ignoreCase = true)) {
                superActivityToast.setOnDismissWrapper(onDismissWrapper)
            }
        }

        superActivityToast.setAnimations(referenceHolder.animations)
        referenceHolder.text?.let { superActivityToast.setText(it) }
        superActivityToast.setTypefaceStyle(referenceHolder.typefaceStyle)
        superActivityToast.setDuration(referenceHolder.duration)
        superActivityToast.setTextColor(referenceHolder.textColor)
        superActivityToast.setTextSizeFloat(referenceHolder.textSize)
        superActivityToast.setIndeterminate(referenceHolder.isIndeterminate)
        referenceHolder.iconPosition?.let { superActivityToast.setIcon(referenceHolder.icon, it) }
        superActivityToast.setBackground(referenceHolder.background)
        superActivityToast.setTouchToDismiss(referenceHolder.isTouchDismissible)

        // Do not use show animation on recreation of SuperActivityToast that was previously showing
        if (position == 1) {
            superActivityToast.setShowImmediate(true)
        }

        superActivityToast.show()

        // Copy properties to this instance
        this.type = superActivityToast.type
        this.toastView = superActivityToast.toastView
        this.messageTextView = superActivityToast.messageTextView
        this.rootLayout = superActivityToast.rootLayout
        this.button = superActivityToast.button
        this.dividerView = superActivityToast.dividerView
        this.progressBar = superActivityToast.progressBar
    }

    /**
     * Shows the SuperActivityToast. If another SuperActivityToast is showing than
     * this one will be added to a queue and shown when the previous SuperActivityToast
     * is dismissed.
     */
    fun show() {
        ManagerSuperActivityToast.getInstance().add(this)
    }

    /**
     * Returns the type of the SuperActivityToast.
     */
    fun getType(): Type = type

    /**
     * Returns the message text of the SuperActivityToast.
     */
    fun getText(): CharSequence = messageTextView.text

    /**
     * Sets the message text of the SuperActivityToast.
     */
    fun setText(text: CharSequence) {
        messageTextView.text = text
    }

    /**
     * Sets the typeface for the message text.
     */
    fun setTypeface(typeface: Typeface?) {
        if (typeface != null) {
            messageTextView.typeface = typeface
        }
    }

    /**
     * Returns the message typeface style of the SuperActivityToast.
     */
    fun getTypefaceStyle(): Int = typefaceStyle

    /**
     * Sets the message typeface style of the SuperActivityToast.
     */
    fun setTypefaceStyle(typeface: Int) {
        this.typefaceStyle = typeface
        messageTextView.setTypeface(messageTextView.typeface, typeface)
    }

    /**
     * Returns the message text color of the SuperActivityToast.
     */
    fun getTextColor(): Int = messageTextView.currentTextColor

    /**
     * Sets the message text color of the SuperActivityToast.
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
     * Returns the text size of the SuperActivityToast message in pixels.
     */
    fun getTextSize(): Float = messageTextView.textSize

    /**
     * Sets the text size of the SuperActivityToast message.
     */
    fun setTextSize(textSize: Int) {
        messageTextView.textSize = textSize.toFloat()
    }

    /**
     * Returns the duration of the SuperActivityToast.
     */
    fun getDuration(): Int = duration

    /**
     * Sets the duration that the SuperActivityToast will show.
     */
    fun setDuration(duration: Int) {
        this.duration = duration
    }

    /**
     * Returns true if the SuperActivityToast is indeterminate.
     */
    fun isIndeterminate(): Boolean = isIndeterminate

    /**
     * If true will show the SuperActivityToast for an indeterminate time period and ignore any set duration.
     */
    fun setIndeterminate(isIndeterminate: Boolean) {
        this.isIndeterminate = isIndeterminate
    }

    /**
     * Sets an icon resource to the SuperActivityToast with a specified position.
     */
    fun setIcon(iconResource: Int, iconPosition: IconPosition) {
        this.icon = iconResource
        this.iconPosition = iconPosition

        val iconDrawable = activity?.resources?.getDrawable(iconResource)
        when (iconPosition) {
            IconPosition.BOTTOM -> messageTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, iconDrawable)
            IconPosition.LEFT -> messageTextView.setCompoundDrawablesWithIntrinsicBounds(iconDrawable, null, null, null)
            IconPosition.RIGHT -> messageTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, iconDrawable, null)
            IconPosition.TOP -> messageTextView.setCompoundDrawablesWithIntrinsicBounds(null, iconDrawable, null, null)
            null -> {}
        }
    }

    /**
     * Returns the icon position of the SuperActivityToast.
     */
    fun getIconPosition(): IconPosition? = iconPosition

    /**
     * Returns the icon resource of the SuperActivityToast.
     */
    fun getIconResource(): Int = icon

    /**
     * Returns the background resource of the SuperActivityToast.
     */
    fun getBackground(): Int = background

    /**
     * Sets the background resource of the SuperActivityToast.
     */
    fun setBackground(background: Int) {
        this.background = background
        rootLayout.setBackgroundResource(background)
    }

    /**
     * Returns the show/hide animations of the SuperActivityToast.
     */
    fun getAnimations(): Animations = animations

    /**
     * Sets the show/hide animations of the SuperActivityToast.
     */
    fun setAnimations(animations: Animations) {
        this.animations = animations
    }

    /**
     * Returns true if the SuperActivityToast is set to show without animation.
     */
    fun getShowImmediate(): Boolean = showImmediate

    /**
     * If true will show the SuperActivityToast without animation.
     */
    fun setShowImmediate(showImmediate: Boolean) {
        this.showImmediate = showImmediate
    }

    /**
     * If true will dismiss the SuperActivityToast if the user touches it.
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
     * Returns true if the SuperActivityToast is touch dismissible.
     */
    fun isTouchDismissible(): Boolean = isTouchDismissible

    /**
     * Used in ManagerSuperActivityToast.
     */
    /**
     * Used in ManagerSuperActivityToast.
     */
    internal fun getOnDismissWrapper(): OnDismissWrapper? = onDismissWrapper

    /**
     * Sets an OnDismissWrapper defined in this library to the SuperActivityToast.
     */
    fun setOnDismissWrapper(onDismissWrapper: OnDismissWrapper) {
        this.onDismissWrapper = onDismissWrapper
        this.onDismissWrapperTag = onDismissWrapper.getTag()
    }

    /**
     * Used in orientation change recreation.
     */
    private fun getOnDismissWrapperTag(): String? = onDismissWrapperTag

    /**
     * Dismisses the SuperActivityToast.
     */
    fun dismiss() {
        ManagerSuperActivityToast.getInstance().removeSuperToast(this)
    }

    /**
     * Sets an OnClickWrapper to the button in a BUTTON type SuperActivityToast.
     */
    fun setOnClickWrapper(onClickWrapper: OnClickWrapper) {
        if (type != Type.BUTTON) {
            Log.e(TAG, "setOnClickListenerWrapper()$ERROR_NOTBUTTONTYPE")
        }

        this.onClickWrapper = onClickWrapper
        this.onClickWrapperTag = onClickWrapper.getTag()
    }

    /**
     * Sets an OnClickWrapper with a parcelable object to the button in a BUTTON type SuperActivityToast.
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
     * Sets the icon resource and text of the button in a BUTTON type SuperActivityToast.
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
     * Sets the icon resource of the button in a BUTTON type SuperActivityToast.
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
     * Returns the divider color of a BUTTON type SuperActivityToast.
     */
    fun getDividerColor(): Int = dividerColor

    /**
     * Sets the divider color of a BUTTON type SuperActivityToast.
     */
    fun setDividerColor(dividerColor: Int) {
        if (type != Type.BUTTON) {
            Log.w(TAG, "setDivider()$ERROR_NOTBUTTONTYPE")
        }

        this.dividerColor = dividerColor

        dividerView?.setBackgroundColor(dividerColor)
    }

    /**
     * Returns the button text of a BUTTON type SuperActivityToast.
     */
    fun getButtonText(): CharSequence {
        return button?.text ?: run {
            Log.e(TAG, "getButtonText()$ERROR_NOTBUTTONTYPE")
            ""
        }
    }

    /**
     * Sets the button text of a BUTTON type SuperActivityToast.
     */
    fun setButtonText(buttonText: CharSequence) {
        if (type != Type.BUTTON) {
            Log.w(TAG, "setButtonText()$ERROR_NOTBUTTONTYPE")
        }

        button?.text = buttonText
    }

    /**
     * Returns the typeface style of the button in a BUTTON type SuperActivityToast.
     */
    fun getButtonTypefaceStyle(): Int = buttonTypefaceStyle

    /**
     * Sets the typeface style of the button in a BUTTON type SuperActivityToast.
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
     * Returns the button text color of a BUTTON type SuperActivityToast.
     */
    fun getButtonTextColor(): Int {
        return button?.currentTextColor ?: run {
            Log.e(TAG, "getButtonTextColor()$ERROR_NOTBUTTONTYPE")
            0
        }
    }

    /**
     * Sets the button text color of a BUTTON type SuperActivityToast.
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
     * Returns the button text size of a BUTTON type SuperActivityToast.
     */
    fun getButtonTextSize(): Float {
        return button?.textSize ?: run {
            Log.e(TAG, "getButtonTextSize()$ERROR_NOTBUTTONTYPE")
            0.0f
        }
    }

    /**
     * Sets the button text size of a BUTTON type SuperActivityToast.
     */
    fun setButtonTextSize(buttonTextSize: Int) {
        if (type != Type.BUTTON) {
            Log.w(TAG, "setButtonTextSize()$ERROR_NOTBUTTONTYPE")
        }

        button?.textSize = buttonTextSize.toFloat()
    }

    /**
     * Returns the progress of the progressbar in a PROGRESS_HORIZONTAL type SuperActivityToast.
     */
    fun getProgress(): Int {
        return progressBar?.progress ?: run {
            Log.e(TAG, "getProgress()$ERROR_NOTPROGRESSHORIZONTALTYPE")
            0
        }
    }

    /**
     * Sets the progress of the progressbar in a PROGRESS_HORIZONTAL type SuperActivityToast.
     */
    fun setProgress(progress: Int) {
        if (type != Type.PROGRESS_HORIZONTAL) {
            Log.w(TAG, "setProgress()$ERROR_NOTPROGRESSHORIZONTALTYPE")
        }

        progressBar?.progress = progress
    }

    /**
     * Returns the maximum value of the progressbar in a PROGRESS_HORIZONTAL type SuperActivityToast.
     */
    fun getMaxProgress(): Int {
        return progressBar?.max ?: run {
            Log.e(TAG, "getMaxProgress()$ERROR_NOTPROGRESSHORIZONTALTYPE")
            0
        }
    }

    /**
     * Sets the maximum value of the progressbar in a PROGRESS_HORIZONTAL type SuperActivityToast.
     */
    fun setMaxProgress(maxProgress: Int) {
        if (type != Type.PROGRESS_HORIZONTAL) {
            Log.w(TAG, "setMaxProgress()$ERROR_NOTPROGRESSHORIZONTALTYPE")
        }

        progressBar?.max = maxProgress
    }

    /**
     * Returns an indeterminate value to the progressbar of a PROGRESS type SuperActivityToast.
     */
    fun getProgressIndeterminate(): Boolean = isProgressIndeterminate

    /**
     * Sets an indeterminate value to the progressbar of a PROGRESS type SuperActivityToast.
     */
    fun setProgressIndeterminate(isIndeterminate: Boolean) {
        if (type != Type.PROGRESS_HORIZONTAL && type != Type.PROGRESS) {
            Log.w(TAG, "setProgressIndeterminate()$ERROR_NOTEITHERPROGRESSTYPE")
        }

        this.isProgressIndeterminate = isIndeterminate

        progressBar?.isIndeterminate = isIndeterminate
    }

    /**
     * Returns the SuperActivityToast message textview.
     */
    fun getTextView(): TextView = messageTextView

    /**
     * Returns the SuperActivityToast view.
     */
    fun getView(): View = toastView

    /**
     * Returns true if the SuperActivityToast is showing.
     */
    fun isShowing(): Boolean = toastView != null && toastView.isShown

    /**
     * Returns the calling activity of the SuperActivityToast.
     */
    fun getActivity(): Activity? = activity

    /**
     * Returns the viewgroup that the SuperActivityToast is attached to.
     */
    fun getViewGroup(): ViewGroup? = viewGroup

    /**
     * Returns the LinearLayout that the SuperActivityToast is attached to.
     */
    private fun getRootLayout(): LinearLayout = rootLayout

    /**
     * Private method used to set a default style to the SuperActivityToast.
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

    /**
     * Parcelable class that saves all data on orientation change.
     */
    private class ReferenceHolder : Parcelable {
        var type: Type
        var animations: Animations
        var isIndeterminate: Boolean = false
        var isTouchDismissible: Boolean = false
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
        var divider: Int = 0
        var buttonTypefaceStyle: Int = 0
        var token: Parcelable? = null
        var text: String? = null
        var buttonText: String? = null
        var clickListenerTag: String? = null
        var dismissListenerTag: String? = null

        constructor(superActivityToast: SuperActivityToast) {
            type = superActivityToast.getType()

            if (type == Type.BUTTON) {
                buttonText = superActivityToast.getButtonText().toString()
                buttonTextSize = superActivityToast.getButtonTextSize()
                buttonTextColor = superActivityToast.getButtonTextColor()
                buttonIcon = superActivityToast.getButtonIcon()
                divider = superActivityToast.getDividerColor()
                clickListenerTag = superActivityToast.getOnClickWrapperTag()
                buttonTypefaceStyle = superActivityToast.getButtonTypefaceStyle()
                token = superActivityToast.getToken()
            }

            if (superActivityToast.getIconResource() != 0 && superActivityToast.getIconPosition() != null) {
                icon = superActivityToast.getIconResource()
                iconPosition = superActivityToast.getIconPosition()
            }

            dismissListenerTag = superActivityToast.getOnDismissWrapperTag()
            animations = superActivityToast.getAnimations()
            text = superActivityToast.getText().toString()
            typefaceStyle = superActivityToast.getTypefaceStyle()
            duration = superActivityToast.getDuration()
            textColor = superActivityToast.getTextColor()
            textSize = superActivityToast.getTextSize()
            isIndeterminate = superActivityToast.isIndeterminate()
            background = superActivityToast.getBackground()
            isTouchDismissible = superActivityToast.isTouchDismissible()
        }

        constructor(parcel: Parcel) {
            type = Type.values()[parcel.readInt()]

            if (type == Type.BUTTON) {
                buttonText = parcel.readString()
                buttonTextSize = parcel.readFloat()
                buttonTextColor = parcel.readInt()
                buttonIcon = parcel.readInt()
                divider = parcel.readInt()
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
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(type.ordinal)

            if (type == Type.BUTTON) {
                parcel.writeString(buttonText)
                parcel.writeFloat(buttonTextSize)
                parcel.writeInt(buttonTextColor)
                parcel.writeInt(buttonIcon)
                parcel.writeInt(divider)
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
