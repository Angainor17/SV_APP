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

package com.github.johnpersano.supertoasts.util

import android.os.Parcelable
import android.view.View
import com.github.johnpersano.supertoasts.SuperToast

/**
 * Class that holds a reference to an OnClickListener set to button type SuperActivityToasts/SuperCardToasts.
 * This is used for restoring listeners on orientation changes.
 *
 * ## Usage Example
 * ```kotlin
 * // Create an OnClickWrapper with a unique tag
 * val clickWrapper = OnClickWrapper("undo_action") { view, token ->
 *     // Handle the click - undo some action
 *     (token as? MyData)?.let { data ->
 *         undoAction(data)
 *     }
 * }
 *
 * // Apply to a BUTTON type toast
 * val toast = SuperActivityToast(activity, SuperToast.Type.BUTTON)
 * toast.setOnClickWrapper(clickWrapper, myParcelableData)
 * toast.show()
 *
 * // For orientation handling, add to Wrappers
 * val wrappers = Wrappers()
 * wrappers.add(clickWrapper)
 * ```
 *
 * ## Important
 * - The tag must be unique among all OnClickWrappers used in the same activity
 * - The same tag must be used when restoring after orientation change
 */
@Suppress("unused")
class OnClickWrapper(
    private val tag: String,
    private val onClickListener: SuperToast.OnClickListener
) : SuperToast.OnClickListener {

    private var token: Parcelable? = null

    /**
     * Returns the tag associated with this OnClickWrapper.
     * This is used to reattach [SuperToast.OnClickListener].
     *
     * @return The unique tag string
     */
    fun getTag(): String = tag

    /**
     * This is used during SuperActivityToast/SuperCardToast recreation and should
     * never be called by the developer.
     *
     * @param token The parcelable token to pass to the click listener
     */
    fun setToken(token: Parcelable?) {
        this.token = token
    }

    override fun onClick(view: View, token: Parcelable?) {
        onClickListener.onClick(view, this.token)
    }
}
