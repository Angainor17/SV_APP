package su.sv.bugreport.data

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ru.ok.tracer.Tracer
import ru.ok.tracer.crash.report.TracerCrashReport
import su.sv.bugreport.domain.model.BugReport
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Worker для фоновой отправки баг-репортов
 * Гарантирует доставку даже если пользователь закроет приложение
 */
@HiltWorker
class BugReportWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val imgbbUploader: ImgbbUploader,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val description = inputData.getString(KEY_DESCRIPTION) ?: ""
            val appVersion = inputData.getString(KEY_APP_VERSION) ?: "unknown"
            val deviceModel = inputData.getString(KEY_DEVICE_MODEL) ?: "unknown"
            val deviceManufacturer = inputData.getString(KEY_DEVICE_MANUFACTURER) ?: "unknown"
            val androidVersion = inputData.getString(KEY_ANDROID_VERSION) ?: "unknown"
            val timestamp = inputData.getLong(KEY_TIMESTAMP, System.currentTimeMillis())
            val screenshotUris = inputData.getStringArray(KEY_SCREENSHOTS)?.map { Uri.parse(it) } ?: emptyList()

            Timber.tag("voronin").d("BugReportWorker: starting work for $timestamp")

            // Логируем метаданные
            TracerCrashReport.log("=== Bug Report (Worker) ===")
            TracerCrashReport.log("Timestamp: $timestamp")
            TracerCrashReport.log("Description: $description")
            TracerCrashReport.log("App Version: $appVersion")
            TracerCrashReport.log("Device: $deviceManufacturer $deviceModel")
            TracerCrashReport.log("Android: $androidVersion")

            // Добавляем custom properties
            Tracer.setCustomProperty("bug_report_type", "user_feedback")
            Tracer.setCustomProperty("bug_report_timestamp", timestamp.toString())
            Tracer.setCustomProperty("device_model", deviceModel)

            // Загружаем скриншоты на Imgbb и логируем ссылки
            if (screenshotUris.isNotEmpty()) {
                TracerCrashReport.log("Screenshots: ${screenshotUris.size}")
                screenshotUris.forEachIndexed { index, uri ->
                    val imageUrl = imgbbUploader.uploadImage(uri, timestamp, index)
                    if (imageUrl != null) {
                        TracerCrashReport.log("Screenshot $index: $imageUrl")
                        Tracer.setCustomProperty("screenshot_$index", imageUrl)
                    } else {
                        TracerCrashReport.log("Screenshot $index: upload failed")
                    }
                }
            }

            // Отправляем отчёт через Tracer
            val issueKey = "user_bug_report_$timestamp"
            TracerCrashReport.report(
                BugReportException(description),
                issueKey = issueKey
            )

            Timber.tag("voronin").d("BugReportWorker: success for $timestamp")
            Result.success()

        } catch (e: Exception) {
            Timber.tag("voronin").e(e, "BugReportWorker: failed")
            // Повторная попытка с экспоненциальной задержкой
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val KEY_DESCRIPTION = "description"
        const val KEY_APP_VERSION = "app_version"
        const val KEY_DEVICE_MODEL = "device_model"
        const val KEY_DEVICE_MANUFACTURER = "device_manufacturer"
        const val KEY_ANDROID_VERSION = "android_version"
        const val KEY_TIMESTAMP = "timestamp"
        const val KEY_SCREENSHOTS = "screenshots"
    }
}

/**
 * Менеджер для постановки задач отправки баг-репортов в WorkManager
 */
class BugReportWorkManager @Inject constructor(
    private val workManager: WorkManager,
) {
    /**
     * Ставит задачу на отправку баг-репорта в очередь
     */
    fun enqueueBugReport(report: BugReport) {
        val inputData = Data.Builder()
            .putString(BugReportWorker.KEY_DESCRIPTION, report.description)
            .putString(BugReportWorker.KEY_APP_VERSION, report.appVersion)
            .putString(BugReportWorker.KEY_DEVICE_MODEL, report.deviceModel)
            .putString(BugReportWorker.KEY_DEVICE_MANUFACTURER, report.deviceManufacturer)
            .putString(BugReportWorker.KEY_ANDROID_VERSION, report.androidVersion)
            .putLong(BugReportWorker.KEY_TIMESTAMP, report.timestamp)
            .putStringArray(
                BugReportWorker.KEY_SCREENSHOTS,
                report.screenshots.map { it.toString() }.toTypedArray()
            )
            .build()

        val workRequest = OneTimeWorkRequestBuilder<BugReportWorker>()
            .setInputData(inputData)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniqueWork(
            "bug_report_${report.timestamp}",
            ExistingWorkPolicy.KEEP,
            workRequest
        )

        Timber.tag("voronin").d("BugReportWorkManager: enqueued work for ${report.timestamp}")
    }
}

/**
 * Исключение для отправки баг-репорта через Tracer
 */
class BugReportException(message: String) : Exception("User Bug Report: $message")