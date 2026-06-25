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

import android.view.View
import com.github.johnpersano.supertoasts.SuperToast

/**
 * Class that holds a reference to OnDismissListeners set to SuperActivityToasts/SuperCardToasts.
 * This is used for restoring listeners on orientation changes.
 *
 * ## Usage Example
 * ```kotlin
 * // Create an OnDismissWrapper with a unique tag
 * val dismissWrapper = OnDismissWrapper("toast_dismissed") { view ->
 *     // Handle the dismiss event
 *     Log.d("Toast", "Toast was dismissed")
 * }
 *
 * // Apply to a toast
 * val toast = SuperActivityToast(activity)
 * toast.setOnDismissWrapper(dismissWrapper)
 * toast.show()
 *
 * // For orientation handling, add to Wrappers
 * val wrappers = Wrappers()
 * wrappers.add(dismissWrapper)
 *
 * // Restore state with wrappers
 * SuperActivityToast.onRestoreState(savedInstanceState, activity, wrappers)
 * ```
 *
 * ## Important
 * - The tag must be unique among all OnDismissWrappers used in the same activity
 * - The same tag must be used when restoring after orientation change
 */
class OnDismissWrapper(
    private val tag: String,
    private val onDismissListener: SuperToast.OnDismissListener
) : SuperToast.OnDismissListener {

    /**
     * Returns the tag associated with this OnDismissWrapper.
     * This is used to reattach [SuperToast.OnDismissListener]s.
     *
     * @return The unique tag string
     */
    fun getTag(): String = tag

    override fun onDismiss(view: View) {
        onDismissListener.onDismiss(view)
    }
}
