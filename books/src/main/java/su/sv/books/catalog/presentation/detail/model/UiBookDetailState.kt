package su.sv.books.catalog.presentation.detail.model

import su.sv.models.ui.book.UiBook

/**
 * Состояние экрана конкретной книги
 */
sealed class UiBookDetailState {

    /**
     * Нет контента, но т.к. запрос в сеть не делаю, то он сразу будет
     */
    object NoContent : UiBookDetailState()

    data class Content(
        val book: UiBook,
        val isActionLoading: Boolean,
        val actionText: String,
    ) : UiBookDetailState()
}
