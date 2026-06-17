package org.geometerplus.fbreader.fbreader.options

import org.geometerplus.zlibrary.core.options.ZLBooleanOption
import org.geometerplus.zlibrary.core.options.ZLEnumOption
import org.geometerplus.zlibrary.core.options.ZLIntegerRangeOption
import org.geometerplus.zlibrary.core.options.ZLStringOption

class FooterOptions {

    @JvmField val showTOCMarks = ZLBooleanOption("Options", "FooterShowTOCMarks", true)
    @JvmField val maxTOCMarks = ZLIntegerRangeOption("Options", "FooterMaxTOCMarks", 10, 1000, 100)
    @JvmField val showClock = ZLBooleanOption("Options", "ShowClockInFooter", true)
    @JvmField val showBattery = ZLBooleanOption("Options", "ShowBatteryInFooter", true)
    @JvmField val showProgress = ZLEnumOption("Options", "DisplayProgressInFooter", ProgressDisplayType.asPages)
    @JvmField val font = ZLStringOption("Options", "FooterFont", "Droid Sans")

    init {
        val oldShowProgress = ZLBooleanOption("Options", "ShowProgressInFooter", true)
        if (!oldShowProgress.value) {
            oldShowProgress.value = true
            showProgress.value = ProgressDisplayType.dontDisplay
        }
    }

    fun showProgressAsPercentage(): Boolean = when (showProgress.value) {
        ProgressDisplayType.asPercentage,
        ProgressDisplayType.asPagesAndPercentage -> true
        else -> false
    }

    fun showProgressAsPages(): Boolean = when (showProgress.value) {
        ProgressDisplayType.asPages,
        ProgressDisplayType.asPagesAndPercentage -> true
        else -> false
    }

    enum class ProgressDisplayType {
        dontDisplay,
        asPages,
        asPercentage,
        asPagesAndPercentage
    }
}
