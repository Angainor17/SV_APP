@file:OptIn(ExperimentalMaterial3Api::class)

package su.sv.books.catalog.presentation.root.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.sv.books.catalog.presentation.root.model.UiRootBooksState
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBookActions
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBooksActions

@Composable
fun BookList(
    state: UiRootBooksState.Content,
    actions: RootBooksActions,
) {
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = {
            actions.onAction(RootBookActions.OnSwipeRefresh)
        },
        state = pullToRefreshState,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(horizontal = 8.dp)
        ) {
            state.books.forEach { book ->
                item(key = book.id) {
                    BookItem(book, actions)
                }
            }
        }
    }
}
