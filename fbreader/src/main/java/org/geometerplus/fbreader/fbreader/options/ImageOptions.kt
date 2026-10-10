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
import org.geometerplus.zlibrary.core.options.ZLColorOption
import org.geometerplus.zlibrary.core.options.ZLEnumOption
import org.geometerplus.zlibrary.core.util.ZLColor
import org.geometerplus.zlibrary.text.view.ZLTextViewBase

class ImageOptions {
    @JvmField
    val ImageViewBackground: ZLColorOption

    @JvmField
    val FitToScreen: ZLEnumOption<ZLTextViewBase.ImageFitting>

    @JvmField
    val TapAction: ZLEnumOption<TapActionEnum>

    @JvmField
    val MatchBackground: ZLBooleanOption

    init {
        ImageViewBackground = ZLColorOption("Colors", "ImageViewBackground", ZLColor(255, 255, 255))
        FitToScreen = ZLEnumOption("Options", "FitImagesToScreen", ZLTextViewBase.ImageFitting.covers)
        TapAction = ZLEnumOption("Options", "ImageTappingAction", TapActionEnum.openImageView)
        MatchBackground = ZLBooleanOption("Colors", "ImageMatchBackground", true)
    }

    enum class TapActionEnum {
        doNothing, selectImage, openImageView
    }
}
