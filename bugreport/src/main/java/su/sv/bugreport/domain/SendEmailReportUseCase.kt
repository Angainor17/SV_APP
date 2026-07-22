package su.sv.bugreport.domain

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import su.sv.bugreport.domain.model.BugReport
import javax.inject.Inject

/**
 * UseCase для создания Intent отправки баг-репорта на email
 */
class SendEmailReportUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Создаёт Intent для отправки баг-репорта на email
     *
     * @param report данные репорта
     * @param email адрес получателя (по умолчанию angainor17@gmail.com)
     * @return Intent для открытия email клиента
     */
    fun execute(
        report: BugReport,
        email: String = DEFAULT_EMAIL,
    ): Intent {
        val subject = buildSubject(report)
        val body = buildBody(report)

        return createEmailIntent(email, subject, body, report.screenshots)
    }

    private fun buildSubject(report: BugReport): String {
        return "Bug Report: SV App ${report.appVersion}"
    }

    private fun buildBody(report: BugReport): String {
        return buildString {
            appendLine("=== Bug Report ===")
            appendLine()
            appendLine("Описание проблемы:")
            appendLine(report.description)
            appendLine()
            appendLine("--- Device Info ---")
            appendLine("App Version: ${report.appVersion}")
            appendLine("Device: ${report.deviceManufacturer} ${report.deviceModel}")
            appendLine("Android: ${report.androidVersion}")
            appendLine("Timestamp: ${report.timestamp}")
            appendLine()
            if (report.screenshots.isNotEmpty()) {
                appendLine("Screenshots: ${report.screenshots.size} attached")
            }
        }
    }

    private fun createEmailIntent(
        email: String,
        subject: String,
        body: String,
        screenshots: List<android.net.Uri>,
    ): Intent {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)

            // Добавляем скриншоты как вложения
            if (screenshots.isNotEmpty()) {
                // Для множественных вложений используем putParcelableArrayListExtra
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(screenshots))
                type = "multipart/mixed"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        return Intent.createChooser(intent, "Отправить отчет")
    }

    companion object {
        const val DEFAULT_EMAIL = "angainor17@gmail.com"
    }
}