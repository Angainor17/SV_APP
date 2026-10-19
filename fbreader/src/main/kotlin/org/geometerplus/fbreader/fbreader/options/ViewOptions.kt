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

import org.geometerplus.fbreader.fbreader.FBView
import org.geometerplus.zlibrary.core.library.ZLibrary
import org.geometerplus.zlibrary.core.options.ZLBooleanOption
import org.geometerplus.zlibrary.core.options.ZLIntegerRangeOption
import org.geometerplus.zlibrary.core.options.ZLStringOption
import org.geometerplus.zlibrary.text.view.style.ZLTextStyleCollection

class ViewOptions {
    @JvmField
    val TwoColumnView: ZLBooleanOption

    @JvmField
    val LeftMargin: ZLIntegerRangeOption

    @JvmField
    val RightMargin: ZLIntegerRangeOption

    @JvmField
    val TopMargin: ZLIntegerRangeOption

    @JvmField
    val BottomMargin: ZLIntegerRangeOption

    @JvmField
    val SpaceBetweenColumns: ZLIntegerRangeOption

    @JvmField
    val FooterHeight: ZLIntegerRangeOption

    @JvmField
    val ColorProfileName: ZLStringOption

    @JvmField
    val ScrollbarType: ZLIntegerRangeOption

    private var myColorProfile: ColorProfile? = null
    private var myTextStyleCollection: ZLTextStyleCollection? = null
    private var myFooterOptions: FooterOptions? = null

    init {
        val zlibrary = ZLibrary.Instance()

        val dpi = zlibrary.getDisplayDPI()
        val x = zlibrary.getWidthInPixels()
        val y = zlibrary.getHeightInPixels()
        val horMargin = minOf(dpi / 5, minOf(x, y) / 30)

        TwoColumnView =
            ZLBooleanOption("Options", "TwoColumnView", x * x + y * y >= 42 * dpi * dpi)
        LeftMargin =
            ZLIntegerRangeOption("Options", "LeftMargin", 0, 100, horMargin)
        RightMargin =
            ZLIntegerRangeOption("Options", "RightMargin", 0, 100, horMargin)
        TopMargin =
            ZLIntegerRangeOption("Options", "TopMargin", 0, 100, 0)
        BottomMargin =
            ZLIntegerRangeOption("Options", "BottomMargin", 0, 100, 4)
        SpaceBetweenColumns =
            ZLIntegerRangeOption("Options", "SpaceBetweenColumns", 0, 300, 3 * horMargin)
        ScrollbarType =
            ZLIntegerRangeOption("Options", "ScrollbarType", 0, 4, FBView.SCROLLBAR_SHOW_AS_FOOTER)
        FooterHeight =
            ZLIntegerRangeOption("Options", "FooterHeight", 8, dpi / 8, dpi / 20)
        ColorProfileName =
            ZLStringOption("Options", "ColorProfile", ColorProfile.DAY)
        ColorProfileName.setSpecialName("colorProfile")
    }

    val colorProfile: ColorProfile
        get() {
            val name = ColorProfileName.getValue()
            var profile = myColorProfile
            if (profile == null || name != profile.Name) {
                profile = ColorProfile.get(name)
                myColorProfile = profile
            }
            return profile
        }

    val textStyleCollection: ZLTextStyleCollection
        get() {
            var collection = myTextStyleCollection
            if (collection == null) {
                collection = ZLTextStyleCollection("Base")
                myTextStyleCollection = collection
            }
            return collection
        }

    val footerOptions: FooterOptions
        get() {
            var options = myFooterOptions
            if (options == null) {
                options = FooterOptions()
                myFooterOptions = options
            }
            return options
        }
}
