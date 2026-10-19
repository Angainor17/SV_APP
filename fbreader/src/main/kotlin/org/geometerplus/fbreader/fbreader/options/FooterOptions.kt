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

class FooterOptions {
    @JvmField
    val ShowTOCMarks: ZLBooleanOption = ZLBooleanOption("Options", "FooterShowTOCMarks", true)

    @JvmField
    val MaxTOCMarks: ZLIntegerRangeOption =
        ZLIntegerRangeOption("Options", "FooterMaxTOCMarks", 10, 1000, 100)

    @JvmField
    val ShowClock: ZLBooleanOption = ZLBooleanOption("Options", "ShowClockInFooter", true)

    @JvmField
    val ShowBattery: ZLBooleanOption = ZLBooleanOption("Options", "ShowBatteryInFooter", true)

    @JvmField
    val ShowProgress: ZLEnumOption<ProgressDisplayType> =
        ZLEnumOption("Options", "DisplayProgressInFooter", ProgressDisplayType.asPages)

    @JvmField
    val Font: ZLStringOption = ZLStringOption("Options", "FooterFont", "Droid Sans")

    init {
        val oldShowProgress = ZLBooleanOption("Options", "ShowProgressInFooter", true)
        if (!oldShowProgress.getValue()) {
            oldShowProgress.setValue(true)
            ShowProgress.setValue(ProgressDisplayType.dontDisplay)
        }
    }

    fun showProgressAsPercentage(): Boolean {
        return when (ShowProgress.getValue()) {
            ProgressDisplayType.asPercentage,
            ProgressDisplayType.asPagesAndPercentage -> true
            else -> false
        }
    }

    fun showProgressAsPages(): Boolean {
        return when (ShowProgress.getValue()) {
            ProgressDisplayType.asPages,
            ProgressDisplayType.asPagesAndPercentage -> true
            else -> false
        }
    }

    enum class ProgressDisplayType {
        dontDisplay, asPages, asPercentage, asPagesAndPercentage,
    }
}
