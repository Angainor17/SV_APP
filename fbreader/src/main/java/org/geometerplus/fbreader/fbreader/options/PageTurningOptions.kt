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
