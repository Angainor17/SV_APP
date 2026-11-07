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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.commonui.ui.components.AppLoadingIndicator
import su.sv.news.presentation.root.model.UiNewsItem
import su.sv.news.presentation.root.model.UiNewsMedia
import su.sv.news.presentation.root.viewmodel.actions.RootNewsActions
import su.sv.news.presentation.root.viewmodel.actions.RootNewsActionsHandler
import su.sv.news.testing.NewsTestTags

/** Минимальное время показа индикатора обновления, чтобы свайп был заметен */
private const val MIN_REFRESH_DURATION_MS = 1500L

/**
 * Список новостей с поддержкой Pull-to-Refresh и пагинации
 *
 * @param lazyPagingItems данные для отображения
 * @param actions обработчик действий
 * @param contentPadding отступы от Scaffold
 * @param onImageClick обработчик клика на изображение (новость, индекс изображения)
 */
@Composable
fun NewsList(
    lazyPagingItems: LazyPagingItems<UiNewsItem>,
    actions: RootNewsActionsHandler,
    contentPadding: PaddingValues,
    onImageClick: (UiNewsItem, Int) -> Unit = { _, _ -> },
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val dimensions = LocalAppDimensions.current

    // Индикатор обновления: виден во время загрузки и минимум MIN_REFRESH_DURATION_MS
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshHideJob by remember { mutableStateOf<Job?>(null) }

    // PullToRefreshBox должен учитывать contentPadding, чтобы индикатор не уходил под тулбар
    PullToRefreshBox(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            lazyPagingItems.refresh()
            refreshHideJob?.cancel()
            refreshHideJob = scope.launch {
                delay(MIN_REFRESH_DURATION_MS)
                isRefreshing = false
            }
        },
        state = pullToRefreshState,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag(NewsTestTags.LIST),
            verticalArrangement = Arrangement.spacedBy(dimensions.listItemSpacing),
        ) {
            items(
                count = lazyPagingItems.itemCount,
                key = lazyPagingItems.itemKey { it.id },
                contentType = { "news_item" }
            ) { index ->
                lazyPagingItems[index]?.let { item ->
                    NewsItem(
                        modifier = Modifier.testTag(NewsTestTags.ITEM),
                        item = item,
                        onItemClick = { media ->
                            handleMediaClick(item, media, actions, onImageClick)
                        }
                    )
                } ?: MessagePlaceholder()
            }
        }
    }
}

/**
 * Обработка клика на медиа-контент
 */
private fun handleMediaClick(
    newsItem: UiNewsItem,
    media: UiNewsMedia,
    actions: RootNewsActionsHandler,
    onImageClick: (UiNewsItem, Int) -> Unit,
) {
    when (media) {
        is UiNewsMedia.ItemVideo -> {
            actions.onAction(RootNewsActions.OnNewsMediaClick(media))
        }

        is UiNewsMedia.ItemImage -> {
            // Находим индекс изображения в списке изображений новости
            val imageIndex = newsItem.images.indexOfFirst { it.image == media.image }
            onImageClick(newsItem, imageIndex.coerceAtLeast(0))
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
