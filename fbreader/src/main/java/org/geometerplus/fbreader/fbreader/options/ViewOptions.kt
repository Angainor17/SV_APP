package org.geometerplus.fbreader.fbreader.options

import org.geometerplus.fbreader.fbreader.FBView
import org.geometerplus.zlibrary.core.library.ZLibrary
import org.geometerplus.zlibrary.core.options.ZLBooleanOption
import org.geometerplus.zlibrary.core.options.ZLIntegerRangeOption
import org.geometerplus.zlibrary.core.options.ZLStringOption
import org.geometerplus.zlibrary.text.view.style.ZLTextStyleCollection

class ViewOptions {

    @JvmField val twoColumnView: ZLBooleanOption
    @JvmField val leftMargin: ZLIntegerRangeOption
    @JvmField val rightMargin: ZLIntegerRangeOption
    @JvmField val topMargin: ZLIntegerRangeOption
    @JvmField val bottomMargin: ZLIntegerRangeOption
    @JvmField val spaceBetweenColumns: ZLIntegerRangeOption
    @JvmField val footerHeight: ZLIntegerRangeOption
    @JvmField val colorProfileName: ZLStringOption
    @JvmField var scrollbarType: ZLIntegerRangeOption

    private var myColorProfile: ColorProfile? = null
    private var textStyleCollection: ZLTextStyleCollection? = null
    private var myFooterOptions: FooterOptions? = null

    init {
        val zlibrary = ZLibrary.Instance()

        val dpi = zlibrary.displayDPI
        val x = zlibrary.widthInPixels
        val y = zlibrary.heightInPixels
        val horMargin = minOf(dpi / 5, minOf(x, y) / 30)

        twoColumnView = ZLBooleanOption("Options", "TwoColumnView", x * x + y * y >= 42 * dpi * dpi)
        leftMargin = ZLIntegerRangeOption("Options", "LeftMargin", 0, 100, horMargin)
        rightMargin = ZLIntegerRangeOption("Options", "RightMargin", 0, 100, horMargin)
        topMargin = ZLIntegerRangeOption("Options", "TopMargin", 0, 100, 0)
        bottomMargin = ZLIntegerRangeOption("Options", "BottomMargin", 0, 100, 4)
        spaceBetweenColumns = ZLIntegerRangeOption("Options", "SpaceBetweenColumns", 0, 300, 3 * horMargin)
        scrollbarType = ZLIntegerRangeOption("Options", "ScrollbarType", 0, 4, FBView.SCROLLBAR_SHOW_AS_FOOTER)
        footerHeight = ZLIntegerRangeOption("Options", "FooterHeight", 8, dpi / 8, dpi / 20)
        colorProfileName = ZLStringOption("Options", "ColorProfile", ColorProfile.DAY)
        colorProfileName.setSpecialName("colorProfile")
    }

    fun getColorProfile(): ColorProfile {
        val name = colorProfileName.value
        if (myColorProfile == null || name != myColorProfile?.name) {
            myColorProfile = ColorProfile.get(name)
        }
        return myColorProfile!!
    }

    fun getTextStyleCollection(): ZLTextStyleCollection {
        if (textStyleCollection == null) {
            textStyleCollection = ZLTextStyleCollection("Base")
        }
        return textStyleCollection!!
    }

    fun getFooterOptions(): FooterOptions {
        if (myFooterOptions == null) {
            myFooterOptions = FooterOptions()
        }
        return myFooterOptions!!
    }
}
