package su.sv.books.catalog.presentation.root.model

sealed class UiRootNewsState {

    data class Success(
        val books: List<UiBook>,
    ) : UiRootNewsState()

    object EmptyState : UiRootNewsState()

    object Loading : UiRootNewsState()

    class Failure(throwable: Throwable) : UiRootNewsState()
}
