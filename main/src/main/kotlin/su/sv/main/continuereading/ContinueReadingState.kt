package su.sv.main.continuereading

import com.github.axet.bookreader.domain.LastReadBookInfo

/**
 * Состояние snackbar "Продолжить чтение".
 */
sealed class ContinueReadingState {
    /**
     * Snackbar скрыт.
     */
    data object Hidden : ContinueReadingState()

    /**
     * Snackbar виден с информацией о книге.
     *
     * @param bookInfo информация о последней прочитанной книге
     */
    data class Visible(
        val bookInfo: LastReadBookInfo,
    ) : ContinueReadingState()
}