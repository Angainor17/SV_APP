@file:OptIn(ExperimentalMaterial3Api::class)

package su.sv.news.presentation.root.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import su.sv.news.presentation.root.RootNewsViewModel
import su.sv.news.presentation.root.model.UiRootNewsState
import su.sv.news.presentation.root.viewmodel.actions.RootNewsActions
import su.sv.news.presentation.root.viewmodel.actions.RootNewsActionsHandler

@Composable
fun BookList(
    viewModel: RootNewsViewModel,
    state: UiRootNewsState.Content,
    actions: RootNewsActionsHandler,
) {
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = {
            actions.onAction(RootNewsActions.OnSwipeRefresh)
        },
        state = pullToRefreshState,
    ) {
        val lazyPagingItems = viewModel.pagingDataFlow.collectAsLazyPagingItems()

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(horizontal = 8.dp)
        ) {
            items(
                lazyPagingItems.itemCount,
                key = lazyPagingItems.itemKey { it.id }
            ) { index ->
                val item = lazyPagingItems[index]
                if (item != null) {
                    NewsItem(item, actions)
                } else {
                    MessagePlaceholder()
                }
            }
        }
    }
}

@Composable
private fun MessagePlaceholder() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        CircularProgressIndicator()
    }
}
