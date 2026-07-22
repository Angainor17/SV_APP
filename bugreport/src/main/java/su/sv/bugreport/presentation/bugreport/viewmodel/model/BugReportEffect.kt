package su.sv.bugreport.presentation.bugreport.viewmodel.model

import android.content.Intent

/**
 * Side-эффекты экрана отправки баг-репорта
 */
sealed class BugReportEffect {

    /**
     * Открыть системный email клиент
     */
    data class OpenEmailClient(
        val intent: Intent
    ) : BugReportEffect()

    /**
     * Закрыть экран после успешной отправки
     */
    data object CloseScreen : BugReportEffect()

    /**
     * Показать ошибку
     */
    data class ShowError(
        val message: String
    ) : BugReportEffect()
}