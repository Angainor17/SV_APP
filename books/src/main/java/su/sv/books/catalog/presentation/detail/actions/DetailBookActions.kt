package su.sv.books.catalog.presentation.detail.actions

import su.sv.models.ui.book.UiBook

sealed class DetailBookActions {

    /** Первоначальная загрузка контента на экран */
    data class LoadState(
        val book: UiBook,
    ) : DetailBookActions()

    /** Нажатие на кнопку "Читать"/"Скачать" */
    data class OnActionClick(
        val book: UiBook,
    ) : DetailBookActions()
}
