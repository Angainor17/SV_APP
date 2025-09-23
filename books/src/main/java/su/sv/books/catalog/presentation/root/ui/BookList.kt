@file:OptIn(ExperimentalMaterial3Api::class)

package su.sv.books.catalog.presentation.root.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.sv.books.R
import su.sv.books.catalog.presentation.root.model.UiRootBooksState
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBookActions
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBooksActions

@Composable
fun BookList(
    state: UiRootBooksState.Content,
    actions: RootBooksActions,
    snackbarHostState: SnackbarHostState
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BookListTopBar(scrollBehavior, actions)
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { contentPadding ->
        PullToRefreshBox(
            modifier = Modifier.padding(top = contentPadding.calculateTopPadding()),
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
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    bottom = 8.dp
                ),
            ) {
                state.books.forEach { book ->
                    item(key = book.id) {
                        BookItem(book, actions)
                    }
                }
            }
        }
    }
}

@Composable
private fun BookListTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    actions: RootBooksActions,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.books_title),
            )
        },
        actions = {
            IconButton(
                onClick = { actions.onAction(RootBookActions.OnToolbarBooksClick) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = stringResource(
                        R.string.books_menu_action_content_description
                    ),
                )
            }
        },
        scrollBehavior = scrollBehavior,
        windowInsets = WindowInsets(
            top = 0,
            bottom = TopAppBarDefaults.windowInsets.getBottom(density = LocalDensity.current),
        ),
    )
}
