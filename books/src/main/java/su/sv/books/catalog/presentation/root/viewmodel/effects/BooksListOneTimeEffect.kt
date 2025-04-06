package su.sv.books.catalog.presentation.root.viewmodel.effects

import su.sv.models.ui.book.UiBook

/**
 * Единожды отображаемые события на экране списка книг
 */
sealed class BooksListOneTimeEffect {

    /** Отображение снека об ошибке */
    data class ShowErrorSnackBar(
        val text: String,
    ) : BooksListOneTimeEffect()

    /** Отображение информации о книге */
    data class OpenBook(
        val book: UiBook,
    ) : BooksListOneTimeEffect()
}