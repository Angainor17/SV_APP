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
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.info.rootinfo.model.UiInfoState
import su.sv.info.rootinfo.viewmodel.RootInfoActions
import su.sv.info.rootinfo.viewmodel.RootInfoActionsHandler

/**
 * Контент информационного экрана
 *
 * @param state состояние с данными
 * @param actionsHandler обработчик действий
 * @param modifier модификатор
 */
@Composable
fun InfoContent(
    state: UiInfoState.Content,
    actionsHandler: RootInfoActionsHandler,
    modifier: Modifier = Modifier
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val dimensions = LocalAppDimensions.current

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = {
            actionsHandler.onAction(RootInfoActions.OnSwipeRefresh)
        },
        state = pullToRefreshState,
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(dimensions.listItemSpacing),
            modifier = modifier,
            contentPadding = PaddingValues(
                bottom = dimensions.itemSpacingLarge
            )
        ) {
            items(state.items.size) {
                InfoItem(state.items[it])
            }
        }
    }
}
