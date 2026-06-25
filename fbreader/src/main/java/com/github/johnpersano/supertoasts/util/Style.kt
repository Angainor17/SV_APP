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

import android.graphics.Color
import android.graphics.Typeface
import com.github.johnpersano.supertoasts.SuperToast
import com.github.johnpersano.supertoasts.util.Style.Companion.BLACK
import com.github.johnpersano.supertoasts.util.Style.Companion.BLUE
import com.github.johnpersano.supertoasts.util.Style.Companion.GRAY
import com.github.johnpersano.supertoasts.util.Style.Companion.GREEN
import com.github.johnpersano.supertoasts.util.Style.Companion.ORANGE
import com.github.johnpersano.supertoasts.util.Style.Companion.PURPLE
import com.github.johnpersano.supertoasts.util.Style.Companion.RED
import com.github.johnpersano.supertoasts.util.Style.Companion.WHITE
import org.geometerplus.R

/**
 * Creates a reference to basic style options so that all types of SuperToasts
 * will be themed the same way in a particular class.
 *
 * ## Usage Example
 * ```kotlin
 * // Get a preset style
 * val style = Style.getStyle(Style.BLUE)
 *
 * // Customize style
 * val customStyle = Style().apply {
 *     textColor = Color.WHITE
 *     background = R.drawable.background_kitkat_blue
 *     animations = SuperToast.Animations.FLYIN
 * }
 *
 * // Apply to toast
 * val toast = SuperToast(context, style)
 * ```
 *
 * ## Available Preset Styles
 * - [BLACK] - Dark background with white text
 * - [BLUE] - Blue background with white text
 * - [GRAY] - Gray background with white text
 * - [GREEN] - Green background with white text
 * - [ORANGE] - Orange background with white text
 * - [PURPLE] - Purple background with white text
 * - [RED] - Red background with white text
 * - [WHITE] - White background with dark text
 */
@Suppress("unused")
class Style {

    companion object {
        /** Black background style constant */
        const val BLACK = 0
        /** Blue background style constant */
        const val BLUE = 1
        /** Gray background style constant */
        const val GRAY = 2
        /** Green background style constant */
        const val GREEN = 3
        /** Orange background style constant */
        const val ORANGE = 4
        /** Purple background style constant */
        const val PURPLE = 5
        /** Red background style constant */
        const val RED = 6
        /** White background style constant */
        const val WHITE = 7

        /**
         * Returns a preset style.
         *
         * @param styleType One of the style constants (BLACK, BLUE, GRAY, etc.)
         * @return [Style] configured with the preset colors
         */
        @JvmStatic
        fun getStyle(styleType: Int): Style {
            return Style().apply {
                when (styleType) {
                    BLACK -> {
                        textColor = Color.WHITE
                        background = getBackground(BLACK)
                        dividerColor = Color.WHITE
                    }
                    WHITE -> {
                        textColor = Color.DKGRAY
                        background = getBackground(WHITE)
                        dividerColor = Color.DKGRAY
                        buttonTextColor = Color.GRAY
                    }
                    GRAY -> {
                        textColor = Color.WHITE
                        background = getBackground(GRAY)
                        dividerColor = Color.WHITE
                        buttonTextColor = Color.GRAY
                    }
                    PURPLE -> {
                        textColor = Color.WHITE
                        background = getBackground(PURPLE)
                        dividerColor = Color.WHITE
                    }
                    RED -> {
                        textColor = Color.WHITE
                        background = getBackground(RED)
                        dividerColor = Color.WHITE
                    }
                    ORANGE -> {
                        textColor = Color.WHITE
                        background = getBackground(ORANGE)
                        dividerColor = Color.WHITE
                    }
                    BLUE -> {
                        textColor = Color.WHITE
                        background = getBackground(BLUE)
                        dividerColor = Color.WHITE
                    }
                    GREEN -> {
                        textColor = Color.WHITE
                        background = getBackground(GREEN)
                        dividerColor = Color.WHITE
                    }
                    else -> {
                        textColor = Color.WHITE
                        background = getBackground(GRAY)
                        dividerColor = Color.WHITE
                    }
                }
            }
        }

        /**
         * Returns a preset style with specified animations.
         *
         * @param styleType One of the style constants (BLACK, BLUE, GRAY, etc.)
         * @param animations The animation type to use
         * @return [Style] configured with the preset colors and animations
         */
        @JvmStatic
        fun getStyle(styleType: Int, animations: SuperToast.Animations): Style {
            return Style().apply {
                this.animations = animations
                when (styleType) {
                    BLACK -> {
                        textColor = Color.WHITE
                        background = getBackground(BLACK)
                        dividerColor = Color.WHITE
                    }
                    WHITE -> {
                        textColor = Color.DKGRAY
                        background = getBackground(WHITE)
                        dividerColor = Color.DKGRAY
                        buttonTextColor = Color.GRAY
                    }
                    GRAY -> {
                        textColor = Color.WHITE
                        background = getBackground(GRAY)
                        dividerColor = Color.WHITE
                        buttonTextColor = Color.GRAY
                    }
                    PURPLE -> {
                        textColor = Color.WHITE
                        background = getBackground(PURPLE)
                        dividerColor = Color.WHITE
                    }
                    RED -> {
                        textColor = Color.WHITE
                        background = getBackground(RED)
                        dividerColor = Color.WHITE
                    }
                    ORANGE -> {
                        textColor = Color.WHITE
                        background = getBackground(ORANGE)
                        dividerColor = Color.WHITE
                    }
                    BLUE -> {
                        textColor = Color.WHITE
                        background = getBackground(BLUE)
                        dividerColor = Color.WHITE
                    }
                    GREEN -> {
                        textColor = Color.WHITE
                        background = getBackground(GREEN)
                        dividerColor = Color.WHITE
                    }
                    else -> {
                        textColor = Color.WHITE
                        background = getBackground(GRAY)
                        dividerColor = Color.WHITE
                    }
                }
            }
        }

        /**
         * Returns the background drawable resource for a style type.
         *
         * @param style One of the style constants
         * @return Drawable resource ID
         */
        @JvmStatic
        fun getBackground(style: Int): Int {
            return when (style) {
                BLACK -> R.drawable.background_kitkat_black
                WHITE -> R.drawable.background_kitkat_white
                GRAY -> R.drawable.background_kitkat_gray
                PURPLE -> R.drawable.background_kitkat_purple
                RED -> R.drawable.background_kitkat_red
                ORANGE -> R.drawable.background_kitkat_orange
                BLUE -> R.drawable.background_kitkat_blue
                GREEN -> R.drawable.background_kitkat_green
                else -> R.drawable.background_kitkat_gray
            }
        }
    }

    /** Animation type for show/hide animations */
    var animations: SuperToast.Animations = SuperToast.Animations.FADE

    /** Background drawable resource */
    var background: Int = getBackground(GRAY)

    /** Typeface style for message text */
    var typefaceStyle: Int = Typeface.NORMAL

    /** Text color for message */
    var textColor: Int = Color.WHITE

    /** Divider color for BUTTON type toasts */
    var dividerColor: Int = Color.WHITE

    /** Button text color for BUTTON type toasts */
    var buttonTextColor: Int = Color.LTGRAY
}
