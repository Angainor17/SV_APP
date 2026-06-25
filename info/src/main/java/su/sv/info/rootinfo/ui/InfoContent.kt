package su.sv.info.rootinfo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.info.rootinfo.model.UiInfoState
import su.sv.info.rootinfo.viewmodel.RootInfoActions
import su.sv.info.rootinfo.viewmodel.RootInfoActionsHandler

/**
 * Контент информационного экрана
 *
 * @param state состояние с данными
 * @param actionsHandler обработчик действий
 * @param contentPadding отступы от Scaffold
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoContent(
    state: UiInfoState.Content,
    actionsHandler: RootInfoActionsHandler,
    contentPadding: PaddingValues,
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val dimensions = LocalAppDimensions.current

    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        isRefreshing = state.isRefreshing,
        onRefresh = {
            actionsHandler.onAction(RootInfoActions.OnSwipeRefresh)
        },
        state = pullToRefreshState,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(dimensions.listItemSpacing),
            contentPadding = contentPadding,
        ) {
            items(
                items = state.items,
                key = { it.url }
            ) { item ->
                InfoItem(item)
            }
        }
    }
}
