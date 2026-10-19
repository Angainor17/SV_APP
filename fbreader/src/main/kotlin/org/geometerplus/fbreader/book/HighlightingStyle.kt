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

package org.geometerplus.fbreader.book

import org.geometerplus.zlibrary.core.util.ZLColor

class HighlightingStyle(
    @JvmField val Id: Int,
    @JvmField val LastUpdateTimestamp: Long,
    name: String?,
    bgColor: ZLColor?,
    fgColor: ZLColor?
) {
    private var myName: String? = name
    private var myBackgroundColor: ZLColor? = bgColor
    private var myForegroundColor: ZLColor? = fgColor

    fun getNameOrNull(): String? = if (myName == "") null else myName

    fun setName(name: String?) {
        myName = name
    }

    fun getBackgroundColor(): ZLColor? = myBackgroundColor

    fun setBackgroundColor(bgColor: ZLColor?) {
        myBackgroundColor = bgColor
    }

    fun getForegroundColor(): ZLColor? = myForegroundColor

    fun setForegroundColor(fgColor: ZLColor?) {
        myForegroundColor = fgColor
    }
}
