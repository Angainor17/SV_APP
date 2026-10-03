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

import org.geometerplus.fbreader.fbreader.DurationEnum
import org.geometerplus.zlibrary.core.options.ZLBooleanOption
import org.geometerplus.zlibrary.core.options.ZLEnumOption
import org.geometerplus.zlibrary.core.options.ZLIntegerRangeOption
import org.geometerplus.zlibrary.core.options.ZLStringOption

class MiscOptions {
    @JvmField
    var AllowScreenBrightnessAdjustment: ZLBooleanOption =
        ZLBooleanOption("LookNFeel", "AllowScreenBrightnessAdjustment", true)

    @JvmField
    val TextSearchPattern: ZLStringOption = ZLStringOption("TextSearch", "Pattern", "")

    @JvmField
    val EnableDoubleTap: ZLBooleanOption = ZLBooleanOption("Options", "EnableDoubleTap", false)

    @JvmField
    val NavigateAllWords: ZLBooleanOption = ZLBooleanOption("Options", "NavigateAllWords", false)

    @JvmField
    val WordTappingAction: ZLEnumOption<WordTappingActionEnum> =
        ZLEnumOption("Options", "WordTappingAction", WordTappingActionEnum.startSelecting)

    @JvmField
    val ToastFontSizePercent: ZLIntegerRangeOption =
        ZLIntegerRangeOption("Options", "ToastFontSizePercent", 25, 100, 90)

    @JvmField
    val ShowFootnoteToast: ZLEnumOption<FootnoteToastEnum> =
        ZLEnumOption("Options", "ShowFootnoteToast", FootnoteToastEnum.footnotesAndSuperscripts)

    @JvmField
    val FootnoteToastDuration: ZLEnumOption<DurationEnum> =
        ZLEnumOption("Options", "FootnoteToastDuration", DurationEnum.duration5)

    enum class WordTappingActionEnum {
        doNothing, selectSingleWord, startSelecting, openDictionary,
    }

    enum class FootnoteToastEnum {
        never, footnotesOnly, footnotesAndSuperscripts, allInternalLinks,
    }
}
