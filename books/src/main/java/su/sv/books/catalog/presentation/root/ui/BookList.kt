@file:OptIn(ExperimentalMaterial3Api::class)

package su.sv.books.catalog.presentation.root.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.sv.books.catalog.presentation.root.model.UiRootBooksState
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBookActions
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBooksActions
import su.sv.books.catalog.presentation.root.viewmodel.effects.BooksListOneTimeEffect
import su.sv.commonui.theme.LocalAppDimensions

@Composable
fun BookList(
    state: UiRootBooksState.Content,
    actions: RootBooksActions,
    scrollEffect: BooksListOneTimeEffect? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val lazyGridState = rememberLazyGridState()
    val dimensions = LocalAppDimensions.current

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
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
                horizontalArrangement = Arrangement.spacedBy(dimensions.itemSpacingMedium),
                verticalArrangement = Arrangement.spacedBy(dimensions.itemSpacingMedium),
                contentPadding = PaddingValues(
                    start = dimensions.screenPaddingHorizontal / 2,
                    end = dimensions.screenPaddingHorizontal / 2,
                    bottom = dimensions.itemSpacingLarge
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
