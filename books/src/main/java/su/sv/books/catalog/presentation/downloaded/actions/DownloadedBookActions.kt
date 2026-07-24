package su.sv.books.catalog.presentation.downloaded.actions

import su.sv.books.catalog.presentation.downloaded.model.UiDownloadedBook

/**
 * Действия на экране скачанных книг
 */
sealed class DownloadedBookActions {

    /** Нажатие кнопки "Назад" */
    object OnBackClick : DownloadedBookActions()

    /** Клик на карточку книги - открывает экран информации */
    data class OnBookClick(val book: UiDownloadedBook) : DownloadedBookActions()

    /** Нажатие кнопки "Читать" - открывает читалку */
    data class OnReadClick(val book: UiDownloadedBook) : DownloadedBookActions()

    /** Запрос на удаление книги (свайп) */
    data class OnDeleteRequest(val book: UiDownloadedBook) : DownloadedBookActions()

    /** Подтверждение удаления */
    object OnDeleteConfirm : DownloadedBookActions()

    /** Отмена удаления */
    object OnDeleteCancel : DownloadedBookActions()

    /** Подсказка свайпа была показана */
    object OnSwipeHintShown : DownloadedBookActions()
}
