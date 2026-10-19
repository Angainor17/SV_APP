package su.sv.books.catalog.presentation.detail.effects

import su.sv.books.catalog.presentation.detail.model.UiNoteWithContext
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

    /** Открыть книгу на позиции заметки */
    data class OpenBookAtNote(
        val book: UiBook,
        val note: UiNoteWithContext,
    ) : BookDetailOneTimeEffect()

    /** Открыть экран заметок с фильтром по книге */
    data class OpenBookmarksForBook(
        val bookFileUri: String?,  // URI файла книги для вычисления MD5
        val bookTitle: String,     // Название для заголовка
    ) : BookDetailOneTimeEffect()
}
