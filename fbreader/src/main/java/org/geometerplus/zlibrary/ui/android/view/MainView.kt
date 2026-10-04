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

package org.geometerplus.zlibrary.ui.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.View

abstract class MainView : View {
    @JvmField
    protected var myColorLevel: Int? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    open fun getScreenBrightness(): Int {
        val colorLevel = myColorLevel
        return if (colorLevel != null) {
            (colorLevel - 0x60) * 25 / (0xFF - 0x60)
        } else {
            50
        }
    }

    open fun setScreenBrightness(percent: Int) {
        val clamped = when {
            percent < 1 -> 1
            percent > 100 -> 100
            else -> percent
        }

        val oldColorLevel = myColorLevel
        if (clamped >= 25) {
            myColorLevel = null
        } else {
            myColorLevel = 0x60 + (0xFF - 0x60) * maxOf(clamped, 0) / 25
        }

        if (oldColorLevel !== myColorLevel) {
            updateColorLevel()
            postInvalidate()
        }
    }

    protected abstract fun updateColorLevel()
}
