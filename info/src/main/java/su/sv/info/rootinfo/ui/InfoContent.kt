@file:OptIn(ExperimentalMaterial3Api::class)

package su.sv.info.rootinfo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.sv.info.rootinfo.model.UiInfoState
import su.sv.info.rootinfo.viewmodel.RootInfoActions
import su.sv.info.rootinfo.viewmodel.RootInfoActionsHandler

@Composable
fun InfoContent(
    state: UiInfoState.Content,
    actionsHandler: RootInfoActionsHandler
) {
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = {
            actionsHandler.onAction(RootInfoActions.OnSwipeRefresh)
        },
        state = pullToRefreshState,
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier,
            contentPadding = PaddingValues(
                bottom = 12.dp,
            )
        ) {
            items(state.items.size) {
                InfoItem(state.items[it])
            }
        }
    }
}
