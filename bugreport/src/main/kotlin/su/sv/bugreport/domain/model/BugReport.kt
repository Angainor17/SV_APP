package su.sv.bugreport.domain.model

import android.net.Uri

/**
 * Модель баг-репорта от пользователя
 *
 * @param description описание проблемы или пожелания
 * @param screenshots список URI скриншотов (до 5 штук)
 * @param appVersion версия приложения
 * @param deviceModel модель устройства
 * @param deviceManufacturer производитель устройства
 * @param androidVersion версия Android
 * @param timestamp время создания репорта
 */
data class BugReport(
    val description: String,
    val screenshots: List<Uri>,
    val appVersion: String,
    val deviceModel: String,
    val deviceManufacturer: String,
    val androidVersion: String,
    val timestamp: Long = System.currentTimeMillis(),
)