package su.sv.books.catalog.presentation.downloaded.effects

import su.sv.books.catalog.presentation.downloaded.model.UiDownloadedBook

/**
 * Одноразовые события на экране скачанных книг
 */
sealed class DownloadedBookEffect {

    /** Открыть детали книги */
    data class OpenBookDetail(val book: UiDownloadedBook) : DownloadedBookEffect()

    /** Открыть читалку напрямую */
    data class OpenReader(val book: UiDownloadedBook) : DownloadedBookEffect()

    /** Вернуться назад */
    object NavigateBack : DownloadedBookEffect()

    /** Показать ошибку */
    data class ShowError(val message: String) : DownloadedBookEffect()
}
