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

import java.util.LinkedList

/**
 * Manages the life of a SuperCardToast on orientation changes.
 *
 * ## Purpose
 * - Maintains references to showing SuperCardToasts
 * - Handles cleanup when activity is destroyed
 * - Supports state restoration after orientation change
 */
class ManagerSuperCardToast private constructor() {

    companion object {
        @Suppress("unused")
        private const val TAG = "Manager SuperCardToast"

        @Volatile
        private var instance: ManagerSuperCardToast? = null

        /**
         * Singleton method to ensure all SuperCardToasts are passed through the same manager.
         */
        @Synchronized
        internal fun getInstance(): ManagerSuperCardToast {
            return instance ?: ManagerSuperCardToast().also { instance = it }
        }
    }

    private val list: LinkedList<SuperCardToast> = LinkedList()

    /**
     * Add a SuperCardToast to the list.
     */
    internal fun add(superCardToast: SuperCardToast) {
        list.add(superCardToast)
    }

    /**
     * Removes a SuperCardToast from the list.
     */
    internal fun remove(superCardToast: SuperCardToast) {
        list.remove(superCardToast)
    }

    /**
     * Removes all SuperCardToasts and clears the list.
     */
    internal fun cancelAllSuperActivityToasts() {
        for (superCardToast in list) {
            if (superCardToast.isShowing()) {
                superCardToast.getViewGroup()?.removeView(superCardToast.getView())
                superCardToast.getViewGroup()?.invalidate()
            }
        }
        list.clear()
    }

    /**
     * Used in SuperCardToast saveState().
     */
    internal fun getList(): LinkedList<SuperCardToast> = list
}
