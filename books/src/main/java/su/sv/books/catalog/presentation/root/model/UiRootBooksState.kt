package su.sv.books.catalog.presentation.root.model

/**
 * Все состояния экрана со списка книг
 */
sealed class UiRootBooksState {

    data class Content(
        val books: List<UiBook>,
        val isRefreshing: Boolean = false,
    ) : UiRootBooksState()

    object EmptyState : UiRootBooksState()

    object Loading : UiRootBooksState()

    class Failure(throwable: Throwable) : UiRootBooksState()
}
