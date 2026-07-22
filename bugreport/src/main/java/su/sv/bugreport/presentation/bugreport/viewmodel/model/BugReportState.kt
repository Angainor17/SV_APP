package su.sv.bugreport.presentation.bugreport.viewmodel.model

import android.net.Uri

/**
 * Состояние экрана отправки баг-репорта
 */
sealed class BugReportState {

    /**
     * Состояние формы ввода
     */
    data class Form(
        val description: String = "",
        val descriptionError: String? = null,
        val screenshots: List<Uri> = emptyList(),
        val maxScreenshots: Int = MAX_SCREENSHOTS,
        val isSendButtonEnabled: Boolean = false,
        val isSending: Boolean = false,
        val sendEmailForFeedback: Boolean = false,  // Чекбокс отправки на почту
    ) : BugReportState() {

        val canAddMoreScreenshots: Boolean
            get() = screenshots.size < maxScreenshots

        val screenshotCount: Int
            get() = screenshots.size
    }

    /**
     * Состояние отправки
     */
    data object Sending : BugReportState()

    /**
     * Состояние успешной отправки (без email)
     */
    data object Success : BugReportState()

    /**
     * Состояние успешной отправки (с email)
     */
    data class SuccessWithEmail(
        val emailIntent: android.content.Intent
    ) : BugReportState()

    /**
     * Состояние ошибки
     */
    data class Error(
        val message: String
    ) : BugReportState()

    companion object {
        const val MAX_SCREENSHOTS = 5
    }
}