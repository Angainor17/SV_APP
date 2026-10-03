package su.sv.books.catalog.presentation.root.model

import androidx.compose.runtime.Immutable
import su.sv.books.catalog.domain.model.BookFilter
import su.sv.models.ui.book.UiBook

/**
 * Все состояния экрана со списка книг
 */
sealed class UiRootBooksState {

    /**
     * @Immutable - оптимизация Compose recomposition
     */
    @Immutable
    data class Content(
        val books: List<UiBook>,
        val filteredBooks: List<UiBook>,
        val filters: List<UiBookFilter>,
        val selectedFilters: Set<BookFilter>,
        val searchQuery: String = "",
        val isRefreshing: Boolean = false,
        val hasDownloadedBooks: Boolean = false,
        val filterScrollResetKey: Int = 0, // Ключ для сброса скролла чипов
    ) : UiRootBooksState() {

        companion object {
            fun create(
                books: List<UiBook>,
                filters: List<UiBookFilter>,
                selectedFilters: Set<BookFilter>,
                hasDownloadedBooks: Boolean,
                searchQuery: String = "",
                filterScrollResetKey: Int = 0,
            ): Content {
                val categoryFilteredBooks = if (selectedFilters.isEmpty() || selectedFilters.contains(
                        BookFilter.All
                    )
                ) {
                    books
                } else {
                    books.filter { book ->
                        selectedFilters.all { filter ->
                            when (filter) {
                                is BookFilter.All -> true
                                is BookFilter.Category -> book.category == filter.name
                                is BookFilter.Author -> book.author.contains(filter.name)
                                is BookFilter.Series -> book.title.contains(filter.name)
                            }
                        }
                    }
                }
                // Дополнительная фильтрация по поисковому запросу (по названию или автору)
                val filteredBooks = if (searchQuery.isBlank()) {
                    categoryFilteredBooks
                } else {
                    categoryFilteredBooks.filter { book ->
                        book.title.contains(searchQuery, ignoreCase = true) ||
                                book.author.contains(searchQuery, ignoreCase = true)
                    }
                }
                return Content(
                    books = books,
                    filteredBooks = filteredBooks,
                    filters = filters,
                    selectedFilters = selectedFilters,
                    searchQuery = searchQuery,
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
