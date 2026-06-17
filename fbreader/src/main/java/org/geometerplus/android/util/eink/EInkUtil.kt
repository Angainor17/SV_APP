package org.geometerplus.android.util.eink

import android.app.Activity
import org.geometerplus.android.util.DeviceType

object EInkUtil {
    @JvmStatic
    fun prepareSingleFullRefresh(a: Activity) {
        val deviceType = DeviceType.Instance()
        if (deviceType == DeviceType.NOOK || deviceType == DeviceType.NOOK12) {
            Nook2Util.setGL16Mode(a)
        }
    }
}
