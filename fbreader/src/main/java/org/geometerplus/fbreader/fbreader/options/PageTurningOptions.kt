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

package org.geometerplus.fbreader.fbreader.options

import org.geometerplus.zlibrary.core.options.ZLBooleanOption
import org.geometerplus.zlibrary.core.options.ZLEnumOption
import org.geometerplus.zlibrary.core.options.ZLIntegerRangeOption
import org.geometerplus.zlibrary.core.options.ZLStringOption
import org.geometerplus.zlibrary.core.view.ZLViewEnums

class PageTurningOptions {

    @JvmField
    val fingerScrolling = ZLEnumOption(
        "Scrolling", "Finger", FingerScrollingType.byTapAndFlick
    )

    @JvmField
    val animation = ZLEnumOption(
        "Scrolling", "Animation", ZLViewEnums.Animation.slide
    )

    @JvmField
    val animationSpeed = ZLIntegerRangeOption("Scrolling", "AnimationSpeed", 1, 10, 7)

    @JvmField
    val horizontal = ZLBooleanOption("Scrolling", "Horizontal", true)

    @JvmField
    val tapZoneMap = ZLStringOption("Scrolling", "TapZoneMap", "")

    enum class FingerScrollingType {
        byTap, byFlick, byTapAndFlick
    }
}
