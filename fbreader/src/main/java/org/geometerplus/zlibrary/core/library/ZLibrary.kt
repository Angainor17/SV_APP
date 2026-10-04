/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.zlibrary.core.library

import org.geometerplus.zlibrary.core.filesystem.ZLResourceFile
import org.geometerplus.zlibrary.core.options.ZLIntegerOption
import org.geometerplus.zlibrary.core.options.ZLStringOption

abstract class ZLibrary protected constructor() {
    @JvmField
    val ScreenHintStageOption = ZLIntegerOption("LookNFeel", "ScreenHintStage", 0)

    init {
        ourImplementation = this
    }

    fun getOrientationOption(): ZLStringOption =
        ZLStringOption("LookNFeel", "Orientation", "system")

    abstract fun createResourceFile(path: String): ZLResourceFile

    abstract fun createResourceFile(parent: ZLResourceFile, name: String): ZLResourceFile

    abstract fun getVersionName(): String

    abstract fun getFullVersionName(): String

    abstract fun getCurrentTimeString(): String

    abstract fun getDisplayDPI(): Int

    abstract fun getWidthInPixels(): Int

    abstract fun getHeightInPixels(): Int

    abstract fun defaultLanguageCodes(): List<String>

    abstract fun supportsAllOrientations(): Boolean

    fun allOrientations(): Array<String> =
        if (supportsAllOrientations()) {
            arrayOf(
                SCREEN_ORIENTATION_SYSTEM,
                SCREEN_ORIENTATION_SENSOR,
                SCREEN_ORIENTATION_PORTRAIT,
                SCREEN_ORIENTATION_LANDSCAPE,
                SCREEN_ORIENTATION_REVERSE_PORTRAIT,
                SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
            )
        } else {
            arrayOf(
                SCREEN_ORIENTATION_SYSTEM,
                SCREEN_ORIENTATION_SENSOR,
                SCREEN_ORIENTATION_PORTRAIT,
                SCREEN_ORIENTATION_LANDSCAPE,
            )
        }

    companion object {
        const val SCREEN_ORIENTATION_SYSTEM = "system"
        const val SCREEN_ORIENTATION_SENSOR = "sensor"
        const val SCREEN_ORIENTATION_PORTRAIT = "portrait"
        const val SCREEN_ORIENTATION_LANDSCAPE = "landscape"
        const val SCREEN_ORIENTATION_REVERSE_PORTRAIT = "reversePortrait"
        const val SCREEN_ORIENTATION_REVERSE_LANDSCAPE = "reverseLandscape"

        private var ourImplementation: ZLibrary? = null

        @JvmStatic
        fun Instance(): ZLibrary = ourImplementation!!
    }
}
