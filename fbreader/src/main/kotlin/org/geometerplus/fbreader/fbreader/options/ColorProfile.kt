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

import org.geometerplus.zlibrary.core.options.ZLColorOption
import org.geometerplus.zlibrary.core.options.ZLEnumOption
import org.geometerplus.zlibrary.core.options.ZLIntegerOption
import org.geometerplus.zlibrary.core.options.ZLStringOption
import org.geometerplus.zlibrary.core.util.ZLColor
import org.geometerplus.zlibrary.core.view.ZLPaintContext
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap

class ColorProfile private constructor(name: String) {
    @JvmField
    val Name: String = name

    @JvmField
    val WallpaperOption: ZLStringOption

    @JvmField
    val FillModeOption: ZLEnumOption<ZLPaintContext.FillMode>

    @JvmField
    val BackgroundOption: ZLColorOption

    @JvmField
    val SelectionBackgroundOption: ZLColorOption

    @JvmField
    val SelectionForegroundOption: ZLColorOption

    @JvmField
    val HighlightingForegroundOption: ZLColorOption

    @JvmField
    val HighlightingBackgroundOption: ZLColorOption

    @JvmField
    val RegularTextOption: ZLColorOption

    @JvmField
    val HyperlinkTextOption: ZLColorOption

    @JvmField
    val VisitedHyperlinkTextOption: ZLColorOption

    @JvmField
    val FooterFillOption: ZLColorOption

    @JvmField
    val FooterNGBackgroundOption: ZLColorOption

    @JvmField
    val FooterNGForegroundOption: ZLColorOption

    @JvmField
    val FooterNGForegroundUnreadOption: ZLColorOption

    init {
        if (NIGHT == name) {
            WallpaperOption = ZLStringOption("Colors", "$name:Wallpaper", "")
            FillModeOption = ZLEnumOption("Colors", "$name:FillMode", ZLPaintContext.FillMode.tile)
            BackgroundOption = createOption(name, "Background", 0, 0, 0)
            SelectionBackgroundOption = createOption(name, "SelectionBackground", 82, 131, 194)
            SelectionForegroundOption = createNullOption(name, "SelectionForeground")
            HighlightingBackgroundOption = createOption(name, "Highlighting", 96, 96, 128)
            HighlightingForegroundOption = createNullOption(name, "HighlightingForeground")
            RegularTextOption = createOption(name, "Text", 192, 192, 192)
            HyperlinkTextOption = createOption(name, "Hyperlink", 60, 142, 224)
            VisitedHyperlinkTextOption = createOption(name, "VisitedHyperlink", 200, 139, 255)
            FooterFillOption = createOption(name, "FooterFillOption", 85, 85, 85)
            FooterNGBackgroundOption = createOption(name, "FooterNGBackgroundOption", 68, 68, 68)
            FooterNGForegroundOption = createOption(name, "FooterNGForegroundOption", 187, 187, 187)
            FooterNGForegroundUnreadOption =
                createOption(name, "FooterNGForegroundUnreadOption", 119, 119, 119)
        } else {
            WallpaperOption = ZLStringOption("Colors", "$name:Wallpaper", "wallpapers/sepia.jpg")
            FillModeOption = ZLEnumOption("Colors", "$name:FillMode", ZLPaintContext.FillMode.tile)
            BackgroundOption = createOption(name, "Background", 255, 255, 255)
            SelectionBackgroundOption = createOption(name, "SelectionBackground", 82, 131, 194)
            SelectionForegroundOption = createNullOption(name, "SelectionForeground")
            HighlightingBackgroundOption = createOption(name, "Highlighting", 255, 192, 128)
            HighlightingForegroundOption = createNullOption(name, "HighlightingForeground")
            RegularTextOption = createOption(name, "Text", 0, 0, 0)
            HyperlinkTextOption = createOption(name, "Hyperlink", 60, 139, 255)
            VisitedHyperlinkTextOption = createOption(name, "VisitedHyperlink", 200, 139, 255)
            FooterFillOption = createOption(name, "FooterFillOption", 170, 170, 170)
            FooterNGBackgroundOption = createOption(name, "FooterNGBackgroundOption", 68, 68, 68)
            FooterNGForegroundOption = createOption(name, "FooterNGForegroundOption", 187, 187, 187)
            FooterNGForegroundUnreadOption =
                createOption(name, "FooterNGForegroundUnreadOption", 119, 119, 119)
        }
    }

    companion object {
        const val DAY = "defaultLight"
        const val NIGHT = "defaultDark"

        private val ourNames = ArrayList<String>()
        private val ourProfiles = HashMap<String, ColorProfile>()

        @JvmStatic
        fun names(): List<String> {
            if (ourNames.isEmpty()) {
                val size = ZLIntegerOption("Colors", "NumberOfSchemes", 0).getValue()
                if (size == 0) {
                    ourNames.add(DAY)
                    ourNames.add(NIGHT)
                } else {
                    for (i in 0 until size) {
                        ourNames.add(ZLStringOption("Colors", "Scheme$i", "").getValue())
                    }
                }
            }
            return Collections.unmodifiableList(ourNames)
        }

        @JvmStatic
        fun get(name: String): ColorProfile {
            var profile = ourProfiles[name]
            if (profile == null) {
                profile = ColorProfile(name)
                ourProfiles[name] = profile
            }
            return profile
        }

        private fun createOption(profileName: String, optionName: String, r: Int, g: Int, b: Int): ZLColorOption =
            ZLColorOption("Colors", "$profileName:$optionName", ZLColor(r, g, b))

        private fun createNullOption(profileName: String, optionName: String): ZLColorOption =
            ZLColorOption("Colors", "$profileName:$optionName", null)
    }
}
