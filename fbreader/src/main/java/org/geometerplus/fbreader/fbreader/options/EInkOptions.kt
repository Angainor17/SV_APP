package org.geometerplus.fbreader.fbreader.options

import org.geometerplus.zlibrary.core.options.ZLBooleanOption
import org.geometerplus.zlibrary.core.options.ZLIntegerRangeOption

class EInkOptions {
    @JvmField
    val EnableFastRefresh = ZLBooleanOption("EInk", "EnableFastRefresh", true)

    @JvmField
    val UpdateInterval = ZLIntegerRangeOption("EInk", "UpdateInterval", 0, 20, 10)
}
