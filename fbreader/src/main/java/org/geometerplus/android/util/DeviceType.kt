package org.geometerplus.android.util

import android.os.Build

enum class DeviceType {
    GENERIC,
    YOTA_PHONE,
    KINDLE_FIRE_1ST_GENERATION,
    KINDLE_FIRE_2ND_GENERATION,
    KINDLE_FIRE_HD,
    NOOK,
    NOOK12,
    EKEN_M001,
    PAN_DIGITAL,
    SAMSUNG_GT_S5830;

    fun hasNoHardwareMenuButton(): Boolean = this == EKEN_M001 || this == PAN_DIGITAL

    fun hasButtonLightsBug(): Boolean = this == SAMSUNG_GT_S5830

    fun isEInk(): Boolean = this == NOOK || this == NOOK12

    fun hasStandardSearchDialog(): Boolean = this != NOOK && this != NOOK12

    companion object {
        private var ourInstance: DeviceType? = null

        @JvmStatic
        fun Instance(): DeviceType {
            if (ourInstance == null) {
                ourInstance = when {
                    "YotaPhone" == Build.BRAND -> YOTA_PHONE
                    "GT-S5830" == Build.MODEL -> SAMSUNG_GT_S5830
                    "Amazon" == Build.MANUFACTURER -> when (Build.MODEL) {
                        "Kindle Fire" -> KINDLE_FIRE_1ST_GENERATION
                        "KFOT" -> KINDLE_FIRE_2ND_GENERATION
                        else -> KINDLE_FIRE_HD
                    }
                    Build.DISPLAY?.contains("simenxie") == true -> EKEN_M001
                    "PD_Novel" == Build.MODEL -> PAN_DIGITAL
                    "BarnesAndNoble" == Build.MANUFACTURER &&
                            "zoom2" == Build.DEVICE &&
                            Build.MODEL != null &&
                            ("NOOK" == Build.MODEL ||
                                    "unknown" == Build.MODEL ||
                                    Build.MODEL.startsWith("BNRV")) -> {
                        if (Build.VERSION.INCREMENTAL?.let { it.startsWith("1.2") || it.startsWith("1.3") } == true) {
                            NOOK12
                        } else {
                            NOOK
                        }
                    }
                    else -> GENERIC
                }
            }
            return ourInstance!!
        }
    }
}
