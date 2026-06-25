@file:OptIn(ExperimentalMaterial3Api::class)

package su.sv.news.presentation.root.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.commonui.ui.components.AppLoadingIndicator
import su.sv.news.presentation.root.model.UiNewsItem
import su.sv.news.presentation.root.model.UiRootNewsState
import su.sv.news.presentation.root.viewmodel.actions.RootNewsActions
import su.sv.news.presentation.root.viewmodel.actions.RootNewsActionsHandler

/**
 * Список новостей с поддержкой Pull-to-Refresh и пагинации
 *
 * @param lazyPagingItems данные для отображения
 * @param state состояние экрана
 * @param actions обработчик действий
 * @param contentPadding отступы от Scaffold
 */
@Composable
fun NewsList(
    lazyPagingItems: LazyPagingItems<UiNewsItem>,
    state: UiRootNewsState,
    actions: RootNewsActionsHandler,
    contentPadding: PaddingValues,
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val dimensions = LocalAppDimensions.current

    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        isRefreshing = state.isRefreshing,
        onRefresh = {
            lazyPagingItems.refresh()
            actions.onAction(RootNewsActions.OnSwipeRefresh)
        },
        state = pullToRefreshState,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(dimensions.listItemSpacing),
            contentPadding = contentPadding,
        ) {
            items(
                count = lazyPagingItems.itemCount,
                key = lazyPagingItems.itemKey { it.id },
                contentType = { "news_item" }
            ) { index ->
                lazyPagingItems[index]?.let { item ->
                    NewsItem(
                        item = item,
                        onItemClick = {
                            actions.onAction(RootNewsActions.OnNewsMediaClick(it))
                        }
                    )
                } ?: MessagePlaceholder()
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
            .padding(horizontal = LocalAppDimensions.current.screenPaddingHorizontal),
        contentAlignment = Alignment.Center
    ) {
        AppLoadingIndicator()
    }
}
