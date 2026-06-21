@file:OptIn(ExperimentalMaterial3Api::class)

package su.sv.books.catalog.presentation.root.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.sv.books.R
import su.sv.books.catalog.presentation.root.model.UiRootBooksState
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBookActions
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBooksActions
import su.sv.books.catalog.presentation.root.viewmodel.effects.BooksListOneTimeEffect

@Composable
fun BookList(
    state: UiRootBooksState.Content,
    actions: RootBooksActions,
    snackbarHostState: SnackbarHostState,
    scrollEffect: BooksListOneTimeEffect? = null,
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val lazyGridState = rememberLazyGridState()

    // Скролл к началу при смене фильтра
    LaunchedEffect(scrollEffect) {
        if (scrollEffect is BooksListOneTimeEffect.ScrollToTop) {
            lazyGridState.animateScrollToItem(0)
        }
    }

    // Определяем, виден ли TopAppBar (для показа/скрытия фильтров)
    val isFiltersVisible by remember {
        derivedStateOf {
            lazyGridState.firstVisibleItemIndex == 0 &&
                lazyGridState.firstVisibleItemScrollOffset < 100
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BookListTopBar(
                scrollBehavior = scrollBehavior,
                actions = actions,
                hasDownloadedBooks = state.hasDownloadedBooks,
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentPadding.calculateTopPadding())
        ) {
            // Фильтры chips
            if (state.filters.isNotEmpty()) {
                BookFiltersChips(
                    filters = state.filters,
                    onFilterClick = { filter ->
                        actions.onAction(RootBookActions.OnFilterSelect(filter))
                    },
                    isVisible = isFiltersVisible,
                    resetScrollKey = state.filterScrollResetKey,
                )
            }

            PullToRefreshBox(
                modifier = Modifier.weight(1f),
                isRefreshing = state.isRefreshing,
                onRefresh = {
                    actions.onAction(RootBookActions.OnSwipeRefresh)
                },
                state = pullToRefreshState,
            ) {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        end = 8.dp,
                        bottom = 8.dp
                    ),
                ) {
                    state.filteredBooks.forEach { book ->
                        item(key = book.id) {
                            BookItem(book, actions)
                        }
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
    hasDownloadedBooks: Boolean,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.books_title),
            )
        },
        actions = {
            if (hasDownloadedBooks) {
                IconButton(
                    onClick = { actions.onAction(RootBookActions.OnToolbarBooksClick) }) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = stringResource(
                            R.string.books_menu_action_content_description
                        ),
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior,
        windowInsets = WindowInsets(
            top = 0,
            bottom = TopAppBarDefaults.windowInsets.getBottom(density = LocalDensity.current),
        ),
    )
}
