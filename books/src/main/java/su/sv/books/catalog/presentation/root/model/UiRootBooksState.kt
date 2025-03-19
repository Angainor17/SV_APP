package su.sv.books.catalog.presentation.root.model

sealed class UiRootBooksState {

    data class Content(
        val books: List<UiBook>,
    ) : UiRootBooksState()

    object EmptyState : UiRootBooksState()

    object Loading : UiRootBooksState()

    class Failure(throwable: Throwable) : UiRootBooksState()
}
