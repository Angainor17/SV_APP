package su.sv.books.catalog.presentation.root.ui

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import su.sv.books.catalog.presentation.root.model.UiRootBooksState
import su.sv.books.catalog.presentation.root.viewmodel.RootBooksActions

@Composable
fun BookList(
    state: UiRootBooksState.Content,
    actions: RootBooksActions,
) {
    LazyColumn {
        state.books.forEach { book ->
            item(key = book.id) {
                BookItem(book, actions)
            }
        }
    }
}
