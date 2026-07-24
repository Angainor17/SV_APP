package su.sv.commonui.util

import android.os.Build
import android.text.TextUtils

/**
 * Утилиты для определения характеристик устройства
 */
object DeviceUtils {

    /**
     * Проверить, является ли устройство Xiaomi/MIUI
     *
     * MIUI имеет проблемы с обновлением темы при recreate()
     */
    fun isMiui(): Boolean {
        return !TextUtils.isEmpty(getMiuiVersion())
    }

    /**
     * Получить версию MIUI
     *
     * @return версия MIUI или null если не MIUI
     */
    fun getMiuiVersion(): String? {
        return try {
            val prop = Class.forName("android.os.SystemProperties")
            val method = prop.getDeclaredMethod("get", String::class.java)
            method.isAccessible = true
            method.invoke(null, "ro.miui.ui.version.name") as? String
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Проверить, является ли устройство Samsung
     */
    fun isSamsung(): Boolean {
        return Build.MANUFACTURER.equals("samsung", ignoreCase = true)
    }

    /**
     * Проверить, является ли устройство Huawei
     */
    fun isHuawei(): Boolean {
        return Build.MANUFACTURER.equals("huawei", ignoreCase = true) ||
               Build.MANUFACTURER.equals("honor", ignoreCase = true)
    }

    /**
     * Проверить, требует ли устройство специальных мер при смене темы
     *
     * Некоторые производители (Xiaomi/MIUI, Samsung, Huawei) имеют проблемы
     * с обновлением цветов при смене темы
     */
    fun needsThemeRecreateWorkaround(): Boolean {
        return isMiui() || isSamsung() || isHuawei()
    }

    /**
     * Получить название производителя для логирования
     */
    fun getManufacturer(): String {
        return Build.MANUFACTURER ?: "Unknown"
    }

    /**
     * Получить модель устройства для логирования
     */
    fun getDeviceModel(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }
}