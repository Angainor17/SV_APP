@file:OptIn(ExperimentalMaterial3Api::class)

package su.sv.books.catalog.presentation.root.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.sv.books.R
import su.sv.books.catalog.presentation.root.model.UiRootBooksState
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBookActions
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBooksActions
import su.sv.books.testing.BooksTestTags
import su.sv.commonui.theme.LocalAdaptiveDimensions
import su.sv.commonui.theme.LocalAppDimensions

@Composable
fun BookList(
    state: UiRootBooksState.Content,
    actions: RootBooksActions,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val lazyGridState = rememberLazyGridState()
    val dimensions = LocalAppDimensions.current
    val adaptiveDims = LocalAdaptiveDimensions.current

    // Скролл к началу при смене фильтра
    // Используем filterScrollResetKey как ключ - он инкрементируется при каждом изменении фильтра
    LaunchedEffect(state.filterScrollResetKey) {
        lazyGridState.animateScrollToItem(0)
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
        // Поле поиска по названию или автору
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { query ->
                actions.onAction(RootBookActions.OnSearchQueryChange(query))
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = adaptiveDims.screenPadding / 2,
                    vertical = dimensions.itemSpacingMedium,
                )
                .testTag(BooksTestTags.BooksCatalog.SEARCH_FIELD),
            placeholder = { Text(stringResource(R.string.books_search_placeholder)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
        )

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
                modifier = Modifier.testTag(BooksTestTags.BooksCatalog.LIST),
                columns = GridCells.Fixed(adaptiveDims.gridColumns),
                horizontalArrangement = Arrangement.spacedBy(adaptiveDims.gridSpacing),
                verticalArrangement = Arrangement.spacedBy(dimensions.itemSpacingMedium),
                contentPadding = PaddingValues(
                    start = adaptiveDims.screenPadding / 2,
                    end = adaptiveDims.screenPadding / 2,
                    bottom = 16.dp,
                ),
            ) {
                items(
                    items = state.filteredBooks,
                    key = { it.id }
                ) { book ->
                    BookItem(book, actions)
                }
            }
        }
    }
}
