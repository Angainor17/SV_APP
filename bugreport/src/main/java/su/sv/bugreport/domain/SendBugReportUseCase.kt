package su.sv.bugreport.domain

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.ok.tracer.Tracer
import ru.ok.tracer.crash.report.TracerCrashReport
import su.sv.bugreport.data.BugReportWorkManager
import su.sv.bugreport.data.ImgbbUploader
import su.sv.bugreport.domain.model.BugReport
import timber.log.Timber
import javax.inject.Inject

/**
 * UseCase для отправки баг-репорта через Tracer API + Imgbb
 */
class SendBugReportUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imgbbUploader: ImgbbUploader,
    private val bugReportWorkManager: BugReportWorkManager,
) {

    /**
     * Отправляет баг-репорт через Tracer с загрузкой скриншотов на Imgbb
     *
     * @param report данные репорта
     * @return Result успеха или ошибки
     */
    suspend fun execute(report: BugReport): Result<Unit> {
        return runCatching {
            // Немедленная отправка метаданных через Tracer
            TracerCrashReport.log("=== Bug Report ===")
            TracerCrashReport.log("Timestamp: ${report.timestamp}")
            TracerCrashReport.log("Description: ${report.description}")
            TracerCrashReport.log("App Version: ${report.appVersion}")
            TracerCrashReport.log("Device: ${report.deviceManufacturer} ${report.deviceModel}")
            TracerCrashReport.log("Android: ${report.androidVersion}")

            // Добавляем custom properties для фильтрации в Tracer dashboard
            Tracer.setCustomProperty("bug_report_type", "user_feedback")
            Tracer.setCustomProperty("bug_report_timestamp", report.timestamp.toString())
            Tracer.setCustomProperty("device_model", report.deviceModel)
            Tracer.setCustomProperty("device_manufacturer", report.deviceManufacturer)

            // Загружаем скриншоты на Imgbb и логируем ссылки
            if (report.screenshots.isNotEmpty()) {
                TracerCrashReport.log("Screenshots: ${report.screenshots.size}")
                report.screenshots.forEachIndexed { index, uri ->
                    val imageUrl = imgbbUploader.uploadImage(uri, report.timestamp, index)
                    if (imageUrl != null) {
                        TracerCrashReport.log("Screenshot $index: $imageUrl")
                        Tracer.setCustomProperty("screenshot_$index", imageUrl)
                    } else {
                        TracerCrashReport.log("Screenshot $index: upload pending (will retry in background)")
                    }
                }
            }

            // Отправляем кастомный репорт через TracerCrashReport
            val issueKey = "user_bug_report_${report.timestamp}"
            TracerCrashReport.report(
                BugReportException(report.description),
                issueKey = issueKey
            )

            // Ставим задачу в WorkManager для гарантированной доставки
            // (если пользователь закроет приложение, отправка продолжится)
            bugReportWorkManager.enqueueBugReport(report)

            Timber.tag("voronin").d("Bug report sent successfully: $issueKey")
        }
    }
}

/**
 * Исключение для отправки баг-репорта через Tracer
 */
class BugReportException(message: String) : Exception("User Bug Report: $message")