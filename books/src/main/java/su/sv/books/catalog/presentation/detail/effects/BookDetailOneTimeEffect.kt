package su.sv.books.catalog.presentation.detail.effects

import su.sv.models.ui.book.UiBook

/**
 * Единожды отображаемые события на экране списка книг
 */
sealed class BookDetailOneTimeEffect {

    /** Отображение снека об ошибке */
    data class ShowErrorSnackBar(
        val text: String,
    ) : BookDetailOneTimeEffect()

    /** Отображение информации о книге */
    data class OpenBook(
        val book: UiBook,
    ) : BookDetailOneTimeEffect()
}
