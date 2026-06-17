package org.geometerplus.fbreader.fbreader.options

import org.geometerplus.zlibrary.core.options.ZLColorOption
import org.geometerplus.zlibrary.core.options.ZLEnumOption
import org.geometerplus.zlibrary.core.options.ZLIntegerOption
import org.geometerplus.zlibrary.core.options.ZLStringOption
import org.geometerplus.zlibrary.core.util.ZLColor
import org.geometerplus.zlibrary.core.view.ZLPaintContext

class ColorProfile private constructor(val name: String, base: ColorProfile?) {

    @JvmField
    val wallpaperOption: ZLStringOption
    @JvmField
    val fillModeOption: ZLEnumOption<ZLPaintContext.FillMode>
    @JvmField
    val backgroundOption: ZLColorOption
    @JvmField
    val selectionBackgroundOption: ZLColorOption
    @JvmField
    val selectionForegroundOption: ZLColorOption
    @JvmField
    val highlightingForegroundOption: ZLColorOption
    @JvmField
    val highlightingBackgroundOption: ZLColorOption
    @JvmField
    val regularTextOption: ZLColorOption
    @JvmField
    val hyperlinkTextOption: ZLColorOption
    @JvmField
    val visitedHyperlinkTextOption: ZLColorOption
    @JvmField
    val footerFillOption: ZLColorOption
    @JvmField
    val footerNGBackgroundOption: ZLColorOption
    @JvmField
    val footerNGForegroundOption: ZLColorOption
    @JvmField
    val footerNGForegroundUnreadOption: ZLColorOption

    init {
        if (base != null) {
            wallpaperOption = ZLStringOption("Colors", "$name:Wallpaper", base.wallpaperOption.value)
            fillModeOption = ZLEnumOption("Colors", "$name:FillMode", base.fillModeOption.value!!)
            backgroundOption = createOption(name, "Background", base.backgroundOption.value)
            selectionBackgroundOption = createOption(name, "SelectionBackground", base.selectionBackgroundOption.value)
            selectionForegroundOption = createNullOption(name, "SelectionForeground")
            highlightingBackgroundOption = createOption(name, "Highlighting", base.highlightingBackgroundOption.value)
            highlightingForegroundOption = createNullOption(name, "HighlightingForeground")
            regularTextOption = createOption(name, "Text", base.regularTextOption.value)
            hyperlinkTextOption = createOption(name, "Hyperlink", base.hyperlinkTextOption.value)
            visitedHyperlinkTextOption = createOption(name, "VisitedHyperlink", base.visitedHyperlinkTextOption.value)
            footerFillOption = createOption(name, "FooterFillOption", base.footerFillOption.value)
            footerNGBackgroundOption = createOption(name, "FooterNGBackgroundOption", base.footerNGBackgroundOption.value)
            footerNGForegroundOption = createOption(name, "FooterNGForegroundOption", base.footerNGForegroundOption.value)
            footerNGForegroundUnreadOption = createOption(name, "FooterNGForegroundUnreadOption", base.footerNGForegroundUnreadOption.value)
        } else if (NIGHT == name) {
            wallpaperOption = ZLStringOption("Colors", "$name:Wallpaper", "")
            fillModeOption = ZLEnumOption("Colors", "$name:FillMode", ZLPaintContext.FillMode.tile)
            backgroundOption = createOption(name, "Background", 0, 0, 0)
            selectionBackgroundOption = createOption(name, "SelectionBackground", 82, 131, 194)
            selectionForegroundOption = createNullOption(name, "SelectionForeground")
            highlightingBackgroundOption = createOption(name, "Highlighting", 96, 96, 128)
            highlightingForegroundOption = createNullOption(name, "HighlightingForeground")
            regularTextOption = createOption(name, "Text", 192, 192, 192)
            hyperlinkTextOption = createOption(name, "Hyperlink", 60, 142, 224)
            visitedHyperlinkTextOption = createOption(name, "VisitedHyperlink", 200, 139, 255)
            footerFillOption = createOption(name, "FooterFillOption", 85, 85, 85)
            footerNGBackgroundOption = createOption(name, "FooterNGBackgroundOption", 68, 68, 68)
            footerNGForegroundOption = createOption(name, "FooterNGForegroundOption", 187, 187, 187)
            footerNGForegroundUnreadOption = createOption(name, "FooterNGForegroundUnreadOption", 119, 119, 119)
        } else {
            wallpaperOption = ZLStringOption("Colors", "$name:Wallpaper", "wallpapers/sepia.jpg")
            fillModeOption = ZLEnumOption("Colors", "$name:FillMode", ZLPaintContext.FillMode.tile)
            backgroundOption = createOption(name, "Background", 255, 255, 255)
            selectionBackgroundOption = createOption(name, "SelectionBackground", 82, 131, 194)
            selectionForegroundOption = createNullOption(name, "SelectionForeground")
            highlightingBackgroundOption = createOption(name, "Highlighting", 255, 192, 128)
            highlightingForegroundOption = createNullOption(name, "HighlightingForeground")
            regularTextOption = createOption(name, "Text", 0, 0, 0)
            hyperlinkTextOption = createOption(name, "Hyperlink", 60, 139, 255)
            visitedHyperlinkTextOption = createOption(name, "VisitedHyperlink", 200, 139, 255)
            footerFillOption = createOption(name, "FooterFillOption", 170, 170, 170)
            footerNGBackgroundOption = createOption(name, "FooterNGBackgroundOption", 68, 68, 68)
            footerNGForegroundOption = createOption(name, "FooterNGForegroundOption", 187, 187, 187)
            footerNGForegroundUnreadOption = createOption(name, "FooterNGForegroundUnreadOption", 119, 119, 119)
        }
    }

    companion object {
        const val DAY = "defaultLight"
        const val NIGHT = "defaultDark"

        private val namesList = mutableListOf<String>()
        private val profiles = mutableMapOf<String, ColorProfile>()

        @JvmStatic
        fun names(): List<String> {
            if (namesList.isEmpty()) {
                val size = ZLIntegerOption("Colors", "NumberOfSchemes", 0).value
                if (size == 0) {
                    namesList.add(DAY)
                    namesList.add(NIGHT)
                } else {
                    for (i in 0 until size) {
                        namesList.add(ZLStringOption("Colors", "Scheme$i", "").value)
                    }
                }
            }
            return namesList.toList()
        }

        @JvmStatic
        fun get(name: String): ColorProfile {
            return profiles.getOrPut(name) { ColorProfile(name, null) }
        }

        private fun createOption(profileName: String, optionName: String, r: Int, g: Int, b: Int): ZLColorOption =
            ZLColorOption("Colors", "$profileName:$optionName", ZLColor(r, g, b))

        private fun createOption(profileName: String, optionName: String, color: ZLColor?): ZLColorOption =
            ZLColorOption("Colors", "$profileName:$optionName", color)

        private fun createNullOption(profileName: String, optionName: String): ZLColorOption =
            ZLColorOption("Colors", "$profileName:$optionName", null)
    }
}
