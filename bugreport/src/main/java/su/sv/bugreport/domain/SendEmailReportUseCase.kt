package su.sv.bugreport.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import su.sv.bugreport.R
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
        screenshots: List<Uri>,
    ): Intent {
        val chooserTitle = context.getString(R.string.bug_report_email_chooser_title)

        return if (screenshots.size > 1) {
            // Для множественных вложений используем ACTION_SEND_MULTIPLE
            // Это лучше поддерживается email-клиентами (Gmail и др.)
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(screenshots))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            // Для одного или отсутствия вложений используем ACTION_SENDTO с mailto:
            // Это фильтрует только email-клиенты
            val uri = "mailto:$email".toUri()
            Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                // Для одного вложения
                if (screenshots.size == 1) {
                    putExtra(Intent.EXTRA_STREAM, screenshots.first())
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
        }.let { intent ->
            // Показываем chooser с понятным заголовком
            Intent.createChooser(intent, chooserTitle)
        }
    }

    companion object {
        const val DEFAULT_EMAIL = "angainor17@gmail.com"
    }
}