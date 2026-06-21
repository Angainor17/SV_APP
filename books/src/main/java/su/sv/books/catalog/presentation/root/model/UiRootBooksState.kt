package su.sv.books.catalog.presentation.root.model

import su.sv.models.ui.book.UiBook

/**
 * Все состояния экрана со списка книг
 */
sealed class UiRootBooksState {

    data class Content(
        val books: List<UiBook>,
        val filteredBooks: List<UiBook>,
        val filters: List<UiBookFilter>,
        val selectedFilters: Set<su.sv.books.catalog.domain.model.BookFilter>,
        val isRefreshing: Boolean = false,
        val hasDownloadedBooks: Boolean = false,
        val filterScrollResetKey: Int = 0, // Ключ для сброса скролла чипов
    ) : UiRootBooksState() {

        companion object {
            fun create(
                books: List<UiBook>,
                filters: List<UiBookFilter>,
                selectedFilters: Set<su.sv.books.catalog.domain.model.BookFilter>,
                hasDownloadedBooks: Boolean,
                filterScrollResetKey: Int = 0,
            ): Content {
                val filteredBooks = if (selectedFilters.isEmpty() || selectedFilters.contains(
                        su.sv.books.catalog.domain.model.BookFilter.All
                    )
                ) {
                    books
                } else {
                    books.filter { book ->
                        selectedFilters.all { filter ->
                            when (filter) {
                                is su.sv.books.catalog.domain.model.BookFilter.All -> true
                                is su.sv.books.catalog.domain.model.BookFilter.Category -> book.category == filter.name
                                is su.sv.books.catalog.domain.model.BookFilter.Author -> book.author.contains(filter.name)
                                is su.sv.books.catalog.domain.model.BookFilter.Series -> book.title.contains(filter.name)
                            }
                        }
                    }
                }
                return Content(
                    books = books,
                    filteredBooks = filteredBooks,
                    filters = filters,
                    selectedFilters = selectedFilters,
                    hasDownloadedBooks = hasDownloadedBooks,
                    filterScrollResetKey = filterScrollResetKey,
                )
            }
        }
    }

    object EmptyState : UiRootBooksState()

    object Loading : UiRootBooksState()

    class Failure(throwable: Throwable) : UiRootBooksState()
}
