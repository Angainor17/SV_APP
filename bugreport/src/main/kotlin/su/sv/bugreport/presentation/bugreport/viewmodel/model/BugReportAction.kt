package su.sv.bugreport.presentation.bugreport.viewmodel.model

import android.net.Uri

/**
 * Действия пользователя на экране отправки баг-репорта
 */
sealed class BugReportAction {

    /**
     * Изменение текста описания
     */
    data class OnDescriptionChange(val text: String) : BugReportAction()

    /**
     * Переключение чекбокса отправки на почту
     */
    data class OnSendEmailForFeedbackChange(val checked: Boolean) : BugReportAction()

    /**
     * Выбор скриншотов
     */
    data class OnScreenshotsSelected(val uris: List<Uri>) : BugReportAction()

    /**
     * Удаление скриншота
     */
    data class OnRemoveScreenshot(val uri: Uri) : BugReportAction()

    /**
     * Нажатие на кнопку отправить
     */
    data object OnSendReport : BugReportAction()

    /**
     * Закрытие экрана успеха
     */
    data object OnSuccessDismiss : BugReportAction()

    /**
     * Повторная попытка отправки после ошибки
     */
    data object OnRetry : BugReportAction()
}