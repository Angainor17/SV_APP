package org.geometerplus.fbreader.fbreader.options

import org.geometerplus.fbreader.fbreader.DurationEnum
import org.geometerplus.zlibrary.core.options.ZLBooleanOption
import org.geometerplus.zlibrary.core.options.ZLEnumOption
import org.geometerplus.zlibrary.core.options.ZLIntegerRangeOption
import org.geometerplus.zlibrary.core.options.ZLStringOption

class MiscOptions {

    @JvmField val textSearchPattern = ZLStringOption("TextSearch", "Pattern", "")
    @JvmField val enableDoubleTap = ZLBooleanOption("Options", "EnableDoubleTap", false)
    @JvmField val navigateAllWords = ZLBooleanOption("Options", "NavigateAllWords", false)
    @JvmField val wordTappingAction = ZLEnumOption("Options", "WordTappingAction", WordTappingActionEnum.startSelecting)
    @JvmField val toastFontSizePercent = ZLIntegerRangeOption("Options", "ToastFontSizePercent", 25, 100, 90)
    @JvmField val showFootnoteToast = ZLEnumOption("Options", "ShowFootnoteToast", FootnoteToastEnum.footnotesAndSuperscripts)
    @JvmField val footnoteToastDuration = ZLEnumOption("Options", "FootnoteToastDuration", DurationEnum.duration5)
    @JvmField var allowScreenBrightnessAdjustment = ZLBooleanOption("LookNFeel", "AllowScreenBrightnessAdjustment", true)

    enum class WordTappingActionEnum {
        doNothing, selectSingleWord, startSelecting, openDictionary
    }

    enum class FootnoteToastEnum {
        never, footnotesOnly, footnotesAndSuperscripts, allInternalLinks
    }
}
