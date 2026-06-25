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

/**
 * Used to store any OnClickWrappers and OnDismissWrappers set to SuperActivityToasts/SuperCardToasts.
 * This should be sent through the onRestoreState() methods of the SuperActivityToasts/SuperCardToasts
 * classes in order for those methods to reattach any listeners.
 *
 * ## Usage Example
 * ```kotlin
 * // Create wrappers container
 * val wrappers = Wrappers()
 *
 * // Add click wrapper for button toast
 * wrappers.add(OnClickWrapper("undo_click") { view, token ->
 *     // Handle undo action
 * })
 *
 * // Add dismiss wrapper
 * wrappers.add(OnDismissWrapper("toast_dismiss") { view ->
 *     // Handle dismiss
 * })
 *
 * // Restore state with wrappers
 * SuperActivityToast.onRestoreState(savedInstanceState, activity, wrappers)
 * ```
 *
 * ## When to Use
 * Use Wrappers when you need to restore click/dismiss listeners after
 * orientation changes. The wrapper tags must match the ones used when
 * originally creating the toast.
 */
class Wrappers {

    private val onClickWrapperList: MutableList<OnClickWrapper> = ArrayList()
    private val onDismissWrapperList: MutableList<OnDismissWrapper> = ArrayList()

    /**
     * Adds an OnClickWrapper to a list that will be reattached on orientation change.
     *
     * @param onClickWrapper The wrapper to add
     */
    fun add(onClickWrapper: OnClickWrapper) {
        onClickWrapperList.add(onClickWrapper)
    }

    /**
     * Adds an OnDismissWrapper to a list that will be reattached on orientation change.
     *
     * @param onDismissWrapper The wrapper to add
     */
    fun add(onDismissWrapper: OnDismissWrapper) {
        onDismissWrapperList.add(onDismissWrapper)
    }

    /**
     * Used during recreation of SuperActivityToasts/SuperCardToasts.
     *
     * @return List of registered OnClickWrappers
     */
    fun getOnClickWrappers(): List<OnClickWrapper> = onClickWrapperList

    /**
     * Used during recreation of SuperActivityToasts/SuperCardToasts.
     *
     * @return List of registered OnDismissWrappers
     */
    fun getOnDismissWrappers(): List<OnDismissWrapper> = onDismissWrapperList
}
